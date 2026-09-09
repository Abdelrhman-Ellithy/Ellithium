package Ellithium.Utilities.interactions;

import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.ai.healing.BaselineStore;
import Ellithium.core.ai.healing.HealingOrchestrator;
import Ellithium.core.ai.models.HealingRequest;
import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.ai.models.HealOutcome;
import Ellithium.core.ai.dom.InteractiveElements;
import Ellithium.core.ai.spi.ElementHealingPort;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.UnexpectedTagNameException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

class BaseActions<T extends WebDriver> {

    private static final ElementHealingPort HEALING_PORT = HealingOrchestrator.get();

    /** Last-known match count per multi-element locator (string key), used to bound set-heal. */
    private static final java.util.concurrent.ConcurrentHashMap<String, Integer> MULTI_ELEMENT_COUNT =
            new java.util.concurrent.ConcurrentHashMap<>();

    protected final T driver;
    final InteractionRecovery recovery;
    protected BaseActions(T driver) {
        this.driver = Objects.requireNonNull(driver, "driver must not be null");
        this.recovery = new InteractionRecovery(this.driver);
    }
    /**
     * Gets a FluentWait instance with specified timeout and polling interval.
     * @param timeoutInSeconds Maximum wait time in seconds
     * @param pollingEveryInMillis Polling interval in milliseconds
     * @return FluentWait instance
     */
    FluentWait<T> getFluentWaitMillis(long timeoutMillis, int pollingEveryInMillis) {
        return WaitManager.getFluentWaitMillis(driver, timeoutMillis, pollingEveryInMillis);
    }

    FluentWait<T> getFluentWait(int timeoutInSeconds, int pollingEveryInMillis) {
        return WaitManager.getFluentWait(driver, timeoutInSeconds, pollingEveryInMillis);
    }

    protected boolean isNativeMobileContext() {
        return InteractionRecovery.isNativeMobileContext(driver);
    }

    protected void requireJavascriptContext(String operation) {
        if (isNativeMobileContext()) {
            Ellithium.core.reporting.Reporter.log(
                    operation + " requires a web or webview context; not supported in a native mobile context",
                    Ellithium.core.logging.LogLevel.ERROR);
            throw new UnsupportedOperationException(
                    operation + " is not supported in a native mobile (Appium) context");
        }
    }
    /**
     * Finds a WebElement using the given locator.
     * If the element is not found, and AI Self-Healing is configured, the healer
     * is invoked to attempt to find a corrected locator.
     * Zero overhead on successful runs — the catch block is never entered.
     *
     * @param locator Element locator
     * @return The found WebElement
     */
    public WebElement findWebElement(By locator) {
        locator = normalizeLocator(locator);
        try {
            WebElement element = driver.findElement(locator);
            BaselineStore.capture(driver, locator, element);
            return element;
        } catch (WebDriverException e) {
            if (recoverContextOrAlert(e, locator)) {
                WebElement recovered = driver.findElement(locator);
                BaselineStore.capture(driver, locator, recovered);
                return recovered;
            }
            if (SeleniumFailurePolicy.isTerminal(e)) throw e;
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            HealOutcome outcome = HEALING_PORT.heal(buildHealingRequest(locator, stack));
            if (outcome != null && outcome.element() != null) {
                return outcome.element();
            }
            throw e;
        }
    }

    private HealingRequest buildHealingRequest(By locator, StackTraceElement[] stack) {
        String actionType   = extractActionFromStack(stack);
        String callerMethod = extractCallerMethodName(stack);
        String fieldName    = extractFieldNameFromStack(stack, locator);
        String locatorValue = extractLocatorValue(locator);
        ElementFingerprint baseline = BaselineStore.getBaseline(driver, locator);
        return new HealingRequest(driver, locator, stack, actionType, callerMethod,
                fieldName, locatorValue, baseline);
    }

    /**
     * Waits for an element to be visible, returning it. 
     * If a TimeoutException or InvalidSelectorException occurs, it falls back to findWebElement()
     * which triggers AI Self-Healing if the element is missing or the locator is invalid.
     */
    WebElement waitForVisibilityAndFindElement(By locator, int timeout, int pollingEvery) {
        return waitForVisibilityAndFindElement(locator, timeout, pollingEvery, AIConfigLoader.isHealOnWaitsEnabled());
    }

    WebElement waitForVisibilityAndFindElement(By locator, int timeout, int pollingEvery, boolean heal) {
        locator = normalizeLocator(locator);
        try {
            return getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (WebDriverException e) {
            if (recoverContextOrAlert(e, locator)) return findWebElement(locator);
            if (!heal || SeleniumFailurePolicy.isTerminal(e)) throw e;
            By cached = HEALING_PORT.getCachedLocator(driver, locator);
            if (cached != null && !cached.equals(locator)) {
                try {
                    return getFluentWaitMillis(Math.min(timeout * 1000L, CACHED_HEAL_FALLBACK_TIMEOUT_MS), pollingEvery)
                            .until(ExpectedConditions.visibilityOfElementLocated(cached));
                } catch (WebDriverException ignored) {}
            }
            return findWebElement(locator);
        }
    }

    /**
     * Waits for all elements to be visible, returning them.
     * If a TimeoutException occurs, attempts to heal the locator before querying again.
     */
    List<WebElement> waitForVisibilityAndFindElements(By locator, int timeout, int pollingEvery) {
        return waitForVisibilityAndFindElements(locator, timeout, pollingEvery, AIConfigLoader.isHealOnWaitsEnabled());
    }

    List<WebElement> waitForVisibilityAndFindElements(By locator, int timeout, int pollingEvery, boolean heal) {
        locator = normalizeLocator(locator);
        try {
            getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
            List<WebElement> els = driver.findElements(locator);
            rememberSetSize(locator, els.size());
            return els;
        } catch (WebDriverException e) {
            if (recoverContextOrAlert(e, locator)) {
                try { return driver.findElements(locator); } catch (WebDriverException ignored) {}
                return new ArrayList<>();
            }
            if (!heal || SeleniumFailurePolicy.isTerminal(e)) throw e;
            List<WebElement> cachedHits = findByCachedHeal(locator);
            if (cachedHits != null) return cachedHits;
            return healElementSet(locator);
        }
    }

    /**
     * Remembers how many elements a multi-element locator matched while healthy, so a later set-heal
     * can prefer the repeating selector whose match count is closest to the original (bounding
     * false positives from an over-broad shared class).
     */
    void rememberSetSize(By locator, int size) {
        if (size > 1) MULTI_ELEMENT_COUNT.put(locator.toString(), size);
    }

    /**
     * Set-aware heal for a broken one→many locator: heals to a single anchor element, then derives
     * a NON-UNIQUE repeating selector (anchor tag + shared class) that matches the whole renamed
     * set, caches that repeating selector (so later multi-element finds reuse the set), and returns
     * all matches. Degrades to the single healed element when no repeating selector can be derived.
     */
    private List<WebElement> healElementSet(By locator) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        HealOutcome outcome = HEALING_PORT.heal(buildHealingRequest(locator, stack));
        if (outcome == null || outcome.element() == null) {
            if (outcome != null && outcome.reconstructedLocator() != null) {
                try {
                    List<WebElement> hits = driver.findElements(outcome.reconstructedLocator());
                    if (!hits.isEmpty()) return hits;
                } catch (WebDriverException ignored) {}
            }
            return new ArrayList<>();
        }
        By setSelector = deriveSetSelector(outcome.element(), locator);
        if (setSelector != null) {
            try {
                List<WebElement> set = driver.findElements(setSelector);
                if (set.size() > 1) {
                    Ellithium.core.ai.healing.AISelfHealer.overrideHealedLocator(
                            driver, locator, setSelector, outcome.score(), null);
                    Ellithium.core.reporting.Reporter.log(
                            "[HEAL] Multi-element locator healed to repeating selector " + setSelector
                            + " (" + set.size() + " elements) for " + locator,
                            Ellithium.core.logging.LogLevel.INFO_GREEN);
                    return set;
                }
            } catch (WebDriverException ignored) {}
        }
        Ellithium.core.reporting.Reporter.log(
                "[HEAL] Multi-element locator " + locator + " healed to a single element — no "
                + "repeating selector found; returning 1 of N", Ellithium.core.logging.LogLevel.WARN);
        return new ArrayList<>(java.util.List.of(outcome.element()));
    }

    /**
     * Derives a repeating CSS selector ({@code tag.class}) from a healed anchor element that matches
     * the renamed sibling set. Among the anchor's classes, prefers the one whose same-tag match
     * count is closest to the locator's last-known set size (or the largest same-tag set when no
     * size was recorded). Returns {@code null} when no class yields more than one same-tag match.
     */
    By deriveSetSelector(WebElement anchor, By originalLocator) {
        try {
            String tag = anchor.getTagName();
            if (tag == null || tag.isBlank()) return null;
            String classAttr = anchor.getDomAttribute("class");
            if (classAttr == null || classAttr.isBlank()) return null;
            String tagLc = tag.toLowerCase(java.util.Locale.ROOT);

            // Candidate classes that form a repeating set. The selector "tag.class" already
            // constrains the element type, so every match is the same tag — the match count IS
            // the same-tag count (no per-element getTagName round-trips needed).
            java.util.Map<String, By> repeating = new java.util.LinkedHashMap<>();
            java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
            for (String cls : classAttr.trim().split("\\s+")) {
                if (!isCssSafeClass(cls)) continue;
                By candidate = By.cssSelector(tagLc + "." + cls);
                int count;
                try {
                    count = driver.findElements(candidate).size();
                } catch (WebDriverException e) {
                    continue;
                }
                if (count > 1) { repeating.put(cls, candidate); counts.put(cls, count); }
            }
            if (repeating.isEmpty()) return null;
            if (repeating.size() == 1) return repeating.values().iterator().next();

            // Multiple repeating classes — pick the one whose name is most semantically related to
            // the original locator's intent, reusing Tier 2's strategy (embedding cosine, else
            // token-Jaccard). This prefers a renamed semantic class (e.g. "product-price" for ".price")
            // over a broad utility class (e.g. "col-md-3") even when the utility class matches more.
            String chosen = Ellithium.core.ai.healing.EnsembleHealer.mostSemanticallySimilar(
                    intentQuery(originalLocator), new java.util.ArrayList<>(repeating.keySet()));
            if (chosen == null) {
                Integer expected = MULTI_ELEMENT_COUNT.get(originalLocator.toString());
                int bestScore = Integer.MIN_VALUE;
                for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) {
                    int score = (expected != null) ? -Math.abs(e.getValue() - expected) : e.getValue();
                    if (score > bestScore) { bestScore = score; chosen = e.getKey(); }
                }
            }
            return repeating.get(chosen);
        } catch (WebDriverException e) {
            return null;
        }
    }

    /**
     * Builds the semantic intent string for a (broken) locator the same way the Tier 2/3 query is
     * derived — strip the {@code By.<strategy>:} prefix and CSS/locator punctuation, then deCamelCase
     * — so "By.cssSelector: .price" → "price" and "By.id: userName" → "user name".
     */
    private static String intentQuery(By locator) {
        String s = locator.toString();
        int colon = s.indexOf(':');
        String value = (colon >= 0 && colon < s.length() - 1) ? s.substring(colon + 1) : s;
        return Ellithium.core.ai.scoring.SemanticQueryBuilder.deCamelCase(
                value.replaceAll("[.#\\[\\]'\"=>~+*:,()]", " "));
    }

    private static boolean isCssSafeClass(String token) {
        if (token == null || token.isEmpty() || Character.isDigit(token.charAt(0))) return false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_')) return false;
        }
        return true;
    }

    /**
     * Queries a previously cached healed locator (if any, and different from {@code locator}).
     * Returns the non-empty hits, or {@code null} when there is no usable cached heal — so the
     * caller falls through to a full heal. The user's own locator is always tried before this.
     */
    private List<WebElement> findByCachedHeal(By locator) {
        By cached = HEALING_PORT.getCachedLocator(driver, locator);
        if (cached == null || cached.equals(locator)) return null;
        try {
            List<WebElement> hits = driver.findElements(cached);
            if (!hits.isEmpty()) return hits;
        } catch (WebDriverException ignored) {}
        return null;
    }

    /**
     * Finds all WebElements matching the given locator. The user's locator is queried first; a
     * cached heal is consulted as a fallback. {@code findElements} returns an empty list (it does
     * NOT throw) for a no-match locator, so a zero-match result also falls back to a cached heal —
     * this is what lets a multi-element method reuse the heal produced by its visibility gate.
     * @param locator Element locator
     * @return List of found WebElements
     */
    public List<WebElement> findWebElements(By locator) {
        locator = normalizeLocator(locator);
        try {
            List<WebElement> hits = driver.findElements(locator);
            if (!hits.isEmpty()) return hits;
            List<WebElement> cachedHits = findByCachedHeal(locator);
            return cachedHits != null ? cachedHits : hits;
        } catch (WebDriverException e) {
            if (recoverContextOrAlert(e, locator)) {
                try { return driver.findElements(locator); } catch (WebDriverException ignored) {}
                return new ArrayList<>();
            }
            if (SeleniumFailurePolicy.isTerminal(e)) throw e;
            List<WebElement> cachedHits = findByCachedHeal(locator);
            if (cachedHits != null) return cachedHits;
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            HealOutcome outcome = HEALING_PORT.heal(buildHealingRequest(locator, stack));
            if (outcome != null && outcome.reconstructedLocator() != null) {
                try {
                    return driver.findElements(outcome.reconstructedLocator());
                } catch (WebDriverException ignored) {}
            }
            return new ArrayList<>();
        }
    }

    /**
     * Safely iterates over elements using a consumer function.
     * Re-locates elements on each iteration to prevent stale element exceptions.
     * Handles dynamic list size changes gracefully.
     * 
     * @param locator Element locator
     * @param action Consumer function to apply to each element
     */
    void forEachElementSafely(By locator, Consumer<WebElement> action) {
        int currentIndex = 0;
        int consecutiveFailures = 0;
        final int maxConsecutiveFailures = 3;

        while (true) {
            List<WebElement> currentElements = findWebElements(locator);
            if (currentIndex >= currentElements.size()) {
                break;
            }

            WebElement current = currentElements.get(currentIndex);
            try {
                action.accept(current);
                currentIndex++;
                consecutiveFailures = 0;
            } catch (WebDriverException e) {
                recoverInListOrRethrow(e, current, locator);
                consecutiveFailures++;
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    if (InteractiveElements.isPlainClick(extractActionFromStack(Thread.currentThread().getStackTrace()))
                            && recovery.jsClick(current)) {
                        Ellithium.core.reporting.Reporter.log(
                                "[RECOVER] JS-clicked element at index " + currentIndex
                                + " after native click was blocked for: " + locator,
                                Ellithium.core.logging.LogLevel.INFO_YELLOW);
                    } else {
                        Ellithium.core.reporting.Reporter.log(
                                "[RECOVER] Skipping element at index " + currentIndex + " after "
                                + maxConsecutiveFailures + " interaction failures for: " + locator,
                                Ellithium.core.logging.LogLevel.WARN);
                    }
                    currentIndex++;
                    consecutiveFailures = 0;
                }
            } catch (IndexOutOfBoundsException e) {
                break;
            }
        }
    }

    /**
     * Safely maps elements to a result list using a function.
     * Re-locates elements on each iteration to prevent stale element exceptions.
     * Handles dynamic list size changes gracefully.
     * 
     * @param <R> The type of result
     * @param locator Element locator
     * @param mapper Function to transform each element to a result
     * @return List of results from applying the mapper function
     */
    <R> List<R> mapElementsSafely(By locator, Function<WebElement, R> mapper) {
        List<R> results = new ArrayList<>();
        int currentIndex = 0;
        int consecutiveFailures = 0;
        final int maxConsecutiveFailures = 3;

        while (true) {
            List<WebElement> currentElements = findWebElements(locator);
            if (currentIndex >= currentElements.size()) {
                break;
            }

            WebElement current = currentElements.get(currentIndex);
            try {
                results.add(mapper.apply(current));
                currentIndex++;
                consecutiveFailures = 0;
            } catch (WebDriverException e) {
                recoverInListOrRethrow(e, current, locator);
                consecutiveFailures++;
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    Ellithium.core.reporting.Reporter.log(
                            "[RECOVER] Skipping element at index " + currentIndex + " after "
                            + maxConsecutiveFailures + " interaction failures for: " + locator,
                            Ellithium.core.logging.LogLevel.WARN);
                    currentIndex++;
                    consecutiveFailures = 0;
                }
            } catch (IndexOutOfBoundsException e) {
                break;
            }
        }
        return results;
    }

    /**
     * Safely retrieves an element by index with bounds checking.
     * Re-queries the element list to prevent stale element exceptions.
     * 
     * @param locator Element locator
     * @param index Zero-based index of the element
     * @return The WebElement at the specified index
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    WebElement findElementByIndexSafely(By locator, int index) {
        List<WebElement> elements = findWebElements(locator);
        if (index < 0 || index >= elements.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds for list of size " + elements.size());
        }
        return elements.get(index);
    }

    /**
     * Gets the current count of elements matching the locator.
     * Re-queries each time to get the most up-to-date count.
     * 
     * @param locator Element locator
     * @return Current number of elements matching the locator
     */
    int getElementCount(By locator) {
        return findWebElements(locator).size();
    }

    /**
     * Safely maps Select dropdown options to a result list using a function.
     * Re-creates the Select object and re-queries options on each iteration
     * to prevent stale element exceptions. Handles dynamic list size changes gracefully.
     * 
     * @param <R> The type of result
     * @param locator Dropdown element locator
     * @param mapper Function to transform each option element to a result
     * @return List of results from applying the mapper function
     */
    <R> List<R> mapSelectOptionsSafely(By locator, Function<WebElement, R> mapper) {
        List<R> results = new ArrayList<>();
        int consecutiveFailures = 0;
        final int maxConsecutiveFailures = 3;
        org.openqa.selenium.support.ui.Select dropDown;
        List<WebElement> options;
        try {
            dropDown = new org.openqa.selenium.support.ui.Select(findWebElement(locator));
            options = dropDown.getOptions();
        } catch (WebDriverException e) {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            HealOutcome outcome = HEALING_PORT.heal(buildHealingRequest(locator, stack));
            if (outcome != null && outcome.element() != null) {
                try {
                    dropDown = new org.openqa.selenium.support.ui.Select(outcome.element());
                    options = dropDown.getOptions();
                } catch (WebDriverException healEx) {
                    Ellithium.core.reporting.Reporter.log(
                            "[HEAL] Select setup failed on healed element for " + locator + ": " + healEx.getMessage(),
                            Ellithium.core.logging.LogLevel.WARN);
                    return results;
                }
            } else {
                return results;
            }
        }
        int i = 0;
        while (i < options.size()) {
            try {
                R result = mapper.apply(options.get(i));
                results.add(result);
                i++;
                consecutiveFailures = 0;
            } catch (WebDriverException e) {
                if (!recoverContextOrAlert(e, locator) && SeleniumFailurePolicy.isTerminal(e)) throw e;
                consecutiveFailures++;
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    Ellithium.core.reporting.Reporter.log(
                            "[RECOVER] Skipping select option at index " + i + " after "
                            + maxConsecutiveFailures + " consecutive failures for: " + locator,
                            Ellithium.core.logging.LogLevel.WARN);
                    i++;
                    consecutiveFailures = 0;
                } else {
                    try {
                        dropDown = new org.openqa.selenium.support.ui.Select(findWebElement(locator));
                        options = dropDown.getOptions();
                    } catch (WebDriverException ignored) {
                        break;
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                break;
            }
        }
        return results;
    }

    /**
     * Safely iterates over Select dropdown elements using a consumer function.
     * Re-creates the Select object on each iteration to prevent stale element exceptions.
     * Handles dynamic list size changes gracefully.
     * 
     * @param locator Dropdown element locator
     * @param action Consumer function to apply to each dropdown element
     */
    void forEachSelectElementSafely(By locator, Consumer<org.openqa.selenium.support.ui.Select> action) {
        int currentIndex = 0;
        int consecutiveFailures = 0;
        final int maxConsecutiveFailures = 3;
        
        while (true) {
            List<WebElement> currentElements = null;
            try {
                currentElements = findWebElements(locator);
                if (currentIndex >= currentElements.size()) {
                    break; // No more elements or list shrunk
                }

                WebElement element = currentElements.get(currentIndex);
                org.openqa.selenium.support.ui.Select select = new org.openqa.selenium.support.ui.Select(element);
                action.accept(select);
                currentIndex++;
                consecutiveFailures = 0; // Reset on success
            } catch (WebDriverException e) {
                recoverInListOrRethrow(e,
                        currentElements != null && currentIndex < currentElements.size()
                                ? currentElements.get(currentIndex) : null,
                        locator);
                consecutiveFailures++;
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    Ellithium.core.reporting.Reporter.log(
                            "[RECOVER] Skipping select element at index " + currentIndex + " after "
                            + maxConsecutiveFailures + " interaction failures for: " + locator,
                            Ellithium.core.logging.LogLevel.WARN);
                    currentIndex++;
                    consecutiveFailures = 0;
                }
            } catch (IndexOutOfBoundsException e) {
                break;
            }
        }
    }

    // ──────────────────────── Context Extraction for Tier 2 ────────────────────────

    /**
     * Extracts the user-facing interaction method from the stack (e.g. "sendData", "clickOnElement"),
     * skipping all internal find/heal/retry/recovery/exception-handling plumbing. Delegates to the
     * shared {@link Ellithium.core.ai.dom.ActionStackResolver} so the exclusion is defined exactly
     * once and the model never sees an internal method as the action.
     */
    static String extractActionFromStack(StackTraceElement[] stack) {
        return Ellithium.core.ai.dom.ActionStackResolver.extractAction(stack);
    }

    /**
     * Extracts the caller's POM method name from the stack (e.g., "setUserEmail", "clickLoginBtn").
     */
    private static String extractCallerMethodName(StackTraceElement[] stack) {
        for (StackTraceElement frame : stack) {
            String cls = frame.getClassName();
            if (cls.startsWith("Ellithium.") || cls.startsWith("org.openqa.selenium")
                    || cls.startsWith("java.") || cls.startsWith("jdk.") || cls.startsWith("sun.")
                    || cls.startsWith("io.cucumber") || cls.startsWith("io.qameta")
                    || cls.startsWith("org.testng") || cls.startsWith("org.junit")
                    || cls.startsWith("net.bytebuddy")) {
                continue;
            }
            return frame.getMethodName();
        }
        return "unknown";
    }

    /**
     * Attempts to extract the By field name from source code at the call site.
     */
    private record CachedSource(long mtime, java.util.List<String> lines) {}
    private static final java.util.concurrent.ConcurrentHashMap<String, CachedSource> SOURCE_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static String extractFieldNameFromStack(StackTraceElement[] stack, By locator) {
        for (StackTraceElement frame : stack) {
            String cls = frame.getClassName();
            if (cls.startsWith("Ellithium.") || cls.startsWith("org.openqa.selenium")
                    || cls.startsWith("java.") || cls.startsWith("sun.")) {
                continue;
            }
            // Resolve source file
            String classFilePart = cls.replace('.', '/') + ".java";
            for (String root : new String[]{"src/test/java/", "src/main/java/"}) {
                String path = root + classFilePart;
                if (new java.io.File(path).exists()) {
                    try {
                        long mtime = new java.io.File(path).lastModified();
                        CachedSource cached = SOURCE_CACHE.get(path);
                        if (cached == null || cached.mtime() != mtime) {
                            cached = new CachedSource(mtime,
                                    java.nio.file.Files.readAllLines(java.nio.file.Paths.get(path)));
                            SOURCE_CACHE.put(path, cached);
                        }
                        java.util.List<String> lines = cached.lines();
                        int lineNum = frame.getLineNumber();
                        if (lineNum >= 1 && lineNum <= lines.size()) {
                            // Look for By field variable used at call site
                            String callLine = lines.get(lineNum - 1).trim();
                            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                                    "(?:this\\.)?([a-zA-Z_][a-zA-Z0-9_]*)").matcher(callLine);
                            while (m.find()) {
                                String candidate = m.group(1);
                                // Check if it's declared as a By field above
                                for (int i = Math.min(lineNum - 2, lines.size() - 1); i >= 0; i--) {
                                    if (lines.get(i).contains("By " + candidate)
                                            || lines.get(i).contains("By\t" + candidate)) {
                                        return candidate;
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    break;
                }
            }
            break; // only check first user frame
        }
        return null;
    }

    /**
     * Extracts the value portion from a By locator's toString().
     * "By.id: emailField" → "emailField", "By.cssSelector: #login" → "#login"
     */
    private static String extractLocatorValue(By locator) {
        String str = locator.toString();
        int colonIdx = str.indexOf(':');
        if (colonIdx >= 0 && colonIdx < str.length() - 1) {
            return str.substring(colonIdx + 1).trim();
        }
        return null;
    }

    // ──────────────────────── Central healing-aware helpers ────────────────────────

    /**
     * Executes a boolean/probe function and returns its result, or {@code defaultValue} on failure.
     * The predicate is POLLED against the present element until it yields a non-{@code false} result
     * or the {@code timeout} budget elapses — so a value that becomes true during the wait (e.g. an
     * element that turns visible after an animation) is caught, exactly like a visibility wait.
     * Honesty is preserved: if the element is present the whole time but the predicate never becomes
     * true (present-but-offscreen/disabled), {@code defaultValue} is returned WITHOUT healing. Only
     * when the locator NEVER resolves at all is healing attempted (Tier 1/2/3), so an intentionally
     * renamed/wrong locator is still resolved and reflected. Heal is the fallback, never a
     * replacement for a locator that already resolves.
     * Methods: isDisplayed/isEnabled/isSelected/isClickable, isTextContains/isAttributeContains/isTextEqual, isElementPresent.
     */
    protected <R> R performAndGetOrDefault(By locator, int timeout, int pollingEvery,
                                            Function<WebElement, R> action, R defaultValue) {
        return performAndGetOrDefault(locator, timeout, pollingEvery, action, defaultValue, false);
    }

    /**
     * As {@link #performAndGetOrDefault(By, int, int, Function, Object)}, but with an explicit
     * {@code heal} switch. When {@code heal} is {@code false} and the locator never resolves,
     * {@code defaultValue} is returned WITHOUT invoking the healing cascade — the contract for a
     * pure existence probe (e.g. {@code isElementPresent}) where a genuinely-absent element must
     * read as absent, not be healed to a near-match.
     */
    protected <R> R performAndGetOrDefault(By locator, int timeout, int pollingEvery,
                                            Function<WebElement, R> action, R defaultValue, boolean heal) {
        final By resolved = normalizeLocator(locator);
        long deadline = deadlineNanos(timeout);
        final boolean[] everPresent = {false};
        for (int pass = 0; pass < 2; pass++) {
            try {
                return getFluentWaitMillis(remainingMillis(deadline), pollingEvery).until(d -> {
                    WebElement el;
                    try {
                        el = d.findElement(resolved);
                    } catch (NoSuchElementException notPresent) {
                        return null;
                    }
                    everPresent[0] = true;
                    try {
                        R r = action.apply(el);
                        return Boolean.FALSE.equals(r) ? null : r;
                    } catch (StaleElementReferenceException stale) {
                        return null;
                    }
                });
            } catch (WebDriverException e) {
                if (SeleniumFailurePolicy.isTerminal(e)) {
                    if (pass == 0 && recoverContextOrAlert(e, resolved)) continue;
                    return defaultValue;
                }
                if (everPresent[0]) return defaultValue;
                if (!heal) return defaultValue;
                WebElement healed = healOnce(resolved);
                if (healed == null) return defaultValue;
                try {
                    return action.apply(healed);
                } catch (Exception ex) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    /**
     * Switches to a frame identified by a {@code By} locator.
     * On {@link org.openqa.selenium.TimeoutException} or
     * {@link org.openqa.selenium.NoSuchFrameException}, healing is triggered via
     * {@link #findWebElement(By)} and the switch is retried on the healed element.
     */
    protected WebDriver performFrameSwitch(By locator, int timeout, int pollingEvery) {
        try {
            return getFluentWait(timeout, pollingEvery)
                    .until(org.openqa.selenium.support.ui.ExpectedConditions
                            .frameToBeAvailableAndSwitchToIt(locator));
        } catch (WebDriverException e) {
            try {
                WebElement frame = findWebElement(locator);
                return driver.switchTo().frame(frame);
            } catch (WebDriverException ignored) {
                throw e;
            }
        }
    }

    // ──────────────────────── Stale-element retry helpers ────────────────────────

    protected static final int STALE_MAX_RETRIES = 2;
    private static final long CACHED_HEAL_FALLBACK_TIMEOUT_MS = 5_000L;

    /**
     * Remaining wait budget (ms) until {@code deadlineNanos}. The retry loop shares ONE deadline so
     * the caller's timeout is the TOTAL budget — the first attempt may use the whole of it, so a
     * genuinely slow-to-appear element is waited for up to the full timeout (not a fixed 1/3 slice),
     * while the total still never exceeds the timeout. Floored at 1ms so a wait always does one poll.
     */
    private static long remainingMillis(long deadlineNanos) {
        long rem = (deadlineNanos - System.nanoTime()) / 1_000_000L;
        return rem > 1L ? rem : 1L;
    }

    private static long deadlineNanos(int timeoutSeconds) {
        return System.nanoTime() + (long) timeoutSeconds * 1_000_000_000L;
    }

    /**
     * Corrects a common caller mistake: an XPath expression passed to the WRONG {@code By} strategy
     * — {@code By.id(...)}, {@code By.cssSelector(...)}, {@code By.name(...)}, {@code By.className(...)}
     * — instead of {@link By#xpath}. This produces a locator that can never match (an id/css/name/class
     * lookup on XPath syntax) and that the healing cascade cannot fix either: the XPath punctuation
     * (`[`, `(`, `'`, `@`, `=`) tokenizes into garbage mutation candidates and pollutes the Tier-2
     * semantic query, so both tiers fall through and a plain refactor typo surfaces as an unhealed
     * {@code NoSuchElementException}. Detected by XPath syntax ({@code //}, {@code (//}, {@code ./},
     * {@code ..}, {@code /tag}) regardless of which strategy the value was wrapped in; {@code By.xpath}
     * itself and any locator that doesn't look like XPath are returned unchanged.
     */
    private static final java.util.regex.Pattern BY_TOSTRING =
            java.util.regex.Pattern.compile("By\\.([a-zA-Z]+):\\s*(.*)", java.util.regex.Pattern.DOTALL);

    static By normalizeLocator(By locator) {
        if (locator == null) return null;
        String s = locator.toString();
        java.util.regex.Matcher m = BY_TOSTRING.matcher(s);
        if (!m.matches()) return locator;
        String method = m.group(1);
        String value = m.group(2).trim();
        if ("xpath".equals(method)) {
            return looksLikeCss(value) ? By.cssSelector(value) : locator;
        }
        if (looksLikeXpath(value)) return By.xpath(value);
        return locator;
    }

    private static boolean looksLikeXpath(String v) {
        if (v.isEmpty()) return false;
        return v.startsWith("//") || v.startsWith("(//") || v.startsWith("(/")
                || v.startsWith("./") || v.startsWith("..") || v.startsWith("/*")
                || (v.charAt(0) == '/' && v.length() > 1 && Character.isLetter(v.charAt(1)));
    }

    private static final java.util.regex.Pattern CSS_TAG_SELECTOR =
            java.util.regex.Pattern.compile("^[a-zA-Z][\\w-]*[.#].+");
    private static final java.util.regex.Pattern CSS_BARE_TAG =
            java.util.regex.Pattern.compile("^[a-zA-Z][\\w-]*$");

    /**
     * Detects a CSS-selector-shaped value wrapped in {@code By.xpath(...)} — the reverse of the
     * mistake {@link #looksLikeXpath} catches. Only shapes XPath cannot itself produce: a leading
     * {@code .class}/{@code #id}, a bracket attribute selector (bare {@code [attr='val']} or prefixed
     * with a bare tag, {@code tag[attr='val']}) whose bracket content is not an XPath {@code @attr}
     * predicate, or a bare {@code tag.class}/{@code tag#id} (XPath has no {@code .}/{@code #} directly
     * after a step name). An XPath attribute predicate always names the attribute as {@code @attr}
     * BEFORE any {@code =} — so {@code [@id='x']} is unambiguously XPath, while
     * {@code [data-testid='foo@bar.com']} is unambiguously CSS even though its VALUE contains {@code @}.
     */
    private static boolean looksLikeCss(String v) {
        if (v.isEmpty()) return false;
        char c0 = v.charAt(0);
        if ((c0 == '.' || c0 == '#') && v.length() > 1
                && (Character.isLetter(v.charAt(1)) || v.charAt(1) == '_' || v.charAt(1) == '\\')) {
            return true;
        }
        int bracket = v.indexOf('[');
        int closeBracket = bracket >= 0 ? v.indexOf(']', bracket) : -1;
        if (bracket >= 0 && closeBracket > bracket) {
            String prefix = v.substring(0, bracket);
            boolean validPrefix = prefix.isEmpty() || CSS_BARE_TAG.matcher(prefix).matches();
            if (validPrefix && !isXpathAttributePredicate(v.substring(bracket + 1, closeBracket))) {
                return true;
            }
        }
        return CSS_TAG_SELECTOR.matcher(v).matches();
    }

    /** True when the bracket content names an attribute as {@code @attr} before any {@code =}. */
    private static boolean isXpathAttributePredicate(String bracketContent) {
        int eq = bracketContent.indexOf('=');
        int at = bracketContent.indexOf('@');
        return at >= 0 && (eq < 0 || at < eq);
    }

    /**
     * Visibility-waited find with NO healing, bounded by {@code timeoutMillis}. The user's own
     * locator is always tried FIRST — a previously cached heal is used only as a fallback when the
     * real locator does not resolve in time, so a correct, present locator is never silently
     * replaced by a stale heal. On a visibility timeout it returns the present element if the
     * locator still resolves (so the caller's recovery path can handle a hidden/obscured element);
     * otherwise it surfaces {@link NoSuchElementException} for the heal path. Interactability is the
     * recovery's job, not this find's — gating the find on clickability would heal a
     * present-but-not-yet-clickable element.
     */
    private WebElement rawVisibleElement(By locator, long timeoutMillis, int polling) {
        try {
            return getFluentWaitMillis(timeoutMillis, polling)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException te) {
            By cached = HEALING_PORT.getCachedLocator(driver, locator);
            if (cached != null && !cached.equals(locator)) {
                try {
                    return getFluentWaitMillis(Math.min(timeoutMillis, CACHED_HEAL_FALLBACK_TIMEOUT_MS), polling)
                            .until(ExpectedConditions.visibilityOfElementLocated(cached));
                } catch (WebDriverException ignored) {}
            }
            List<WebElement> present = driver.findElements(locator);
            if (!present.isEmpty()) return present.get(0);
            throw new NoSuchElementException("No element located by " + locator, te);
        }
    }

    /**
     * Presence-waited find with NO healing, bounded by {@code timeoutMillis}. Unlike
     * {@link #rawVisibleElement}, does not require {@code isDisplayed()} — reading a DOM
     * attribute/property does not need the element to be visible, so gating on visibility here
     * only adds a redundant round trip and widens the window for a stale read against an
     * element whose attributes change on their own (an animation, a live status field).
     */
    private WebElement rawPresentElement(By locator, long timeoutMillis, int polling) {
        try {
            return getFluentWaitMillis(timeoutMillis, polling)
                    .until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (TimeoutException te) {
            By cached = HEALING_PORT.getCachedLocator(driver, locator);
            if (cached != null && !cached.equals(locator)) {
                try {
                    return getFluentWaitMillis(Math.min(timeoutMillis, CACHED_HEAL_FALLBACK_TIMEOUT_MS), polling)
                            .until(ExpectedConditions.presenceOfElementLocated(cached));
                } catch (WebDriverException ignored) {}
            }
            throw new NoSuchElementException("No element located by " + locator, te);
        }
    }

    /**
     * The single locator-healing call used by the retry helpers, fired at most once per operation.
     * Returns the healed element, or {@code null} when healing did not resolve one.
     */
    private WebElement healOnce(By locator) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        HealOutcome outcome = HEALING_PORT.heal(buildHealingRequest(locator, stack));
        return outcome != null ? outcome.element() : null;
    }

    /**
     * Per-element recovery for the multi-element loops: rethrows terminal failures
     * (session/alert/context) immediately, scrolls the current element into view for a
     * recoverable interaction failure, and is a no-op otherwise (the loop re-finds or skips).
     * Never heals — a non-empty list means the locator already resolved.
     */
    private void recoverInListOrRethrow(WebDriverException e, WebElement current, By locator) {
        if (recoverContextOrAlert(e, locator)) return;
        if (SeleniumFailurePolicy.isTerminal(e)) throw e;
        if (SeleniumFailurePolicy.classify(e) == SeleniumFailurePolicy.FailureClass.RECOVER_INTERACTION) {
            recovery.scrollToCenter(current);
        }
    }

    /**
     * Active recovery for the non-healable context failures: clears a blocking alert ({@code ALERT})
     * or restores a lost frame/window context ({@code CONTEXT}) so the original action can be retried.
     * Returns true only when such a failure was handled — the caller should then retry; otherwise the
     * caller proceeds with its normal heal/rethrow logic.
     */
    private boolean recoverContextOrAlert(WebDriverException e, By locator) {
        SeleniumFailurePolicy.FailureClass fc = SeleniumFailurePolicy.classify(e);
        if (fc == SeleniumFailurePolicy.FailureClass.ALERT) return recovery.handleUnexpectedAlert();
        if (fc == SeleniumFailurePolicy.FailureClass.CONTEXT) return recovery.recoverContext(locator, e);
        return false;
    }

    void performWithStaleRetry(By locator, int timeout, int polling,
                                         Consumer<WebElement> action) {
        locator = normalizeLocator(locator);
        long deadline = deadlineNanos(timeout);
        WebDriverException lastException = null;
        boolean healed = false;
        WebElement lastResolved = null;
        for (int attempt = 0; attempt <= STALE_MAX_RETRIES; attempt++) {
            WebElement el;
            try {
                el = rawVisibleElement(locator, remainingMillis(deadline), polling);
            } catch (StaleElementReferenceException e) {
                lastException = e;
                continue;
            } catch (WebDriverException e) {
                lastException = e;
                if (recoverContextOrAlert(e, locator)) continue;
                if (!SeleniumFailurePolicy.shouldHeal(e)) throw e;
                if (healed) continue;
                healed = true;
                el = healOnce(locator);
                if (el == null) continue;
            }
            lastResolved = el;
            try {
                action.accept(el);
                return;
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                lastException = e;
            } catch (UnexpectedTagNameException e) {
                if (healed) throw e;
                healed = true;
                WebElement healedEl = healOnce(locator);
                if (healedEl == null) throw e;
                lastResolved = healedEl;
                try {
                    action.accept(healedEl);
                    return;
                } catch (WebDriverException retryEx) {
                    throw e;
                }
            } catch (WebDriverException e) {
                lastException = e;
                SeleniumFailurePolicy.FailureClass fc = SeleniumFailurePolicy.classify(e);
                if (fc == SeleniumFailurePolicy.FailureClass.RECOVER_INTERACTION) {
                    // Recover on the element we actually resolved (which may be a healed element
                    // whose original locator no longer resolves), not just by re-querying the locator.
                    WebElement target = recovery.resolveInteractable(locator, remainingMillis(deadline), polling);
                    if (target == null) target = el;
                    recovery.scrollToCenter(target);
                    recovery.awaitClickable(target, remainingMillis(deadline), polling);
                    try { action.accept(target); return; }
                    catch (WebDriverException retryEx) { lastException = retryEx; lastResolved = target; }
                    if (!healed) {
                        healed = true;
                        WebElement healedEl = healOnce(locator);
                        if (healedEl != null) {
                            lastResolved = healedEl;
                            try { action.accept(healedEl); return; }
                            catch (WebDriverException he) { lastException = he; }
                        }
                    }
                } else if (fc == SeleniumFailurePolicy.FailureClass.ALERT
                        || fc == SeleniumFailurePolicy.FailureClass.CONTEXT) {
                    if (!recoverContextOrAlert(e, locator)) throw e;
                } else if (fc != SeleniumFailurePolicy.FailureClass.RELOCATE) {
                    throw e;
                }
            }
        }
        if (InteractiveElements.isPlainClick(extractActionFromStack(Thread.currentThread().getStackTrace()))) {
            WebElement target = recovery.resolveInteractable(locator, remainingMillis(deadline), polling);
            if (target == null) target = lastResolved;
            if (target == null && !healed) target = healOnce(locator);
            if (recovery.jsClick(target)) {
                Ellithium.core.reporting.Reporter.log(
                        "[RECOVER] JS-clicked after native click was blocked for " + locator,
                        Ellithium.core.logging.LogLevel.INFO_YELLOW);
                return;
            }
        }
        if (lastException != null) throw lastException;
    }

    <R> R performAndGet(By locator, int timeout, int polling,
                                   Function<WebElement, R> action) {
        return performAndGet(locator, timeout, polling, action, true);
    }

    /**
     * As {@link #performAndGet(By, int, int, Function)}, but with an explicit
     * {@code requireVisibility} switch. Pure attribute/property reads pass {@code false} — they
     * only need the element present, and skipping the visibility check removes a redundant round
     * trip on the hot path of every such read.
     */
    <R> R performAndGet(By locator, int timeout, int polling,
                                   Function<WebElement, R> action, boolean requireVisibility) {
        locator = normalizeLocator(locator);
        long deadline = deadlineNanos(timeout);
        WebDriverException lastException = null;
        boolean healed = false;
        for (int attempt = 0; attempt <= STALE_MAX_RETRIES; attempt++) {
            WebElement el;
            try {
                el = requireVisibility
                        ? rawVisibleElement(locator, remainingMillis(deadline), polling)
                        : rawPresentElement(locator, remainingMillis(deadline), polling);
            } catch (StaleElementReferenceException e) {
                lastException = e;
                continue;
            } catch (WebDriverException e) {
                lastException = e;
                if (recoverContextOrAlert(e, locator)) continue;
                if (!SeleniumFailurePolicy.shouldHeal(e)) throw e;
                if (healed) continue;
                healed = true;
                el = healOnce(locator);
                if (el == null) continue;
            }
            try {
                return action.apply(el);
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                lastException = e;
            } catch (UnexpectedTagNameException e) {
                if (healed) throw e;
                healed = true;
                WebElement healedEl = healOnce(locator);
                if (healedEl == null) throw e;
                try {
                    return action.apply(healedEl);
                } catch (WebDriverException retryEx) {
                    throw e;
                }
            } catch (WebDriverException e) {
                lastException = e;
                SeleniumFailurePolicy.FailureClass fc = SeleniumFailurePolicy.classify(e);
                if (fc == SeleniumFailurePolicy.FailureClass.RECOVER_INTERACTION) {
                    WebElement target = recovery.resolveInteractable(locator, remainingMillis(deadline), polling);
                    if (target != null) {
                        try { return action.apply(target); }
                        catch (WebDriverException retryEx) { lastException = retryEx; }
                    }
                    if (!healed) {
                        healed = true;
                        WebElement healedEl = healOnce(locator);
                        if (healedEl != null) {
                            try { return action.apply(healedEl); }
                            catch (WebDriverException he) { lastException = he; }
                        }
                    }
                } else if (fc == SeleniumFailurePolicy.FailureClass.ALERT
                        || fc == SeleniumFailurePolicy.FailureClass.CONTEXT) {
                    if (!recoverContextOrAlert(e, locator)) throw e;
                } else if (fc != SeleniumFailurePolicy.FailureClass.RELOCATE) {
                    throw e;
                }
            }
        }
        if (lastException != null) throw lastException;
        throw new IllegalStateException("performAndGet: loop exited without result or exception");
    }
}
