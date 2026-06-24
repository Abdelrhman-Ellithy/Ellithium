package Ellithium.Utilities.interactions;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

class BaseActions<T extends WebDriver> {

    private static final ElementHealingPort HEALING_PORT = HealingOrchestrator.get();

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
        By effective = HEALING_PORT.getCachedLocator(driver, locator);
        if (effective == null) effective = locator;
        try {
            WebElement element = getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfElementLocated(effective));
            return element;
        } catch (WebDriverException e) {
            if (recoverContextOrAlert(e, locator)) return findWebElement(locator);
            if (SeleniumFailurePolicy.isTerminal(e)) throw e;
            return findWebElement(locator);
        }
    }

    /**
     * Waits for all elements to be visible, returning them.
     * If a TimeoutException occurs, attempts to heal the locator before querying again.
     */
    List<WebElement> waitForVisibilityAndFindElements(By locator, int timeout, int pollingEvery) {
        By effective = HEALING_PORT.getCachedLocator(driver, locator);
        if (effective == null) effective = locator;
        try {
            getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(effective));
            return driver.findElements(effective);
        } catch (WebDriverException e) {
            if (recoverContextOrAlert(e, locator)) {
                try { return driver.findElements(locator); } catch (WebDriverException ignored) {}
                return new ArrayList<>();
            }
            if (SeleniumFailurePolicy.isTerminal(e)) throw e;
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
     * Finds all WebElements matching the given locator.
     * Respects the AI healing cache if the locator was previously healed.
     * @param locator Element locator
     * @return List of found WebElements
     */
    public List<WebElement> findWebElements(By locator) {
        By effective = HEALING_PORT.getCachedLocator(driver, locator);
        if (effective == null) effective = locator;
        try {
            return driver.findElements(effective);
        } catch (WebDriverException e) {
            if (recoverContextOrAlert(e, locator)) {
                try { return driver.findElements(locator); } catch (WebDriverException ignored) {}
                return new ArrayList<>();
            }
            if (SeleniumFailurePolicy.isTerminal(e)) throw e;
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
                if (SeleniumFailurePolicy.isTerminal(e)) throw e;
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
     * Infrastructure classes whose methods are internal plumbing and must NEVER be reported as the
     * user-facing action sent to the healing model. Excluding by class (not by method name) means
     * every current and future helper in these classes is automatically kept out of the stack trace
     * — no manual maintenance, no leak risk when a new helper is added.
     */
    private static final Set<String> INFRA_CLASSES = Set.of(
            "Ellithium.Utilities.interactions.BaseActions",
            "Ellithium.Utilities.interactions.InteractionRecovery",
            "Ellithium.Utilities.interactions.SeleniumFailurePolicy");

    private static boolean isInfraFrame(String className) {
        for (String infra : INFRA_CLASSES) {
            if (className.equals(infra) || className.startsWith(infra + "$")) return true;
        }
        return false;
    }

    /**
     * Extracts the Ellithium interaction method name from the stack (e.g., "sendData", "clickOnElement"),
     * skipping all infrastructure-class frames so only the genuine user-facing action surfaces.
     */
    static String extractActionFromStack(StackTraceElement[] stack) {
        for (StackTraceElement frame : stack) {
            String cls = frame.getClassName();
            if (cls.startsWith("Ellithium.Utilities.interactions.") && !isInfraFrame(cls)) {
                return frame.getMethodName();
            }
        }
        return "unknown";
    }

    /**
     * Extracts the caller's POM method name from the stack (e.g., "setUserEmail", "clickLoginBtn").
     */
    private static String extractCallerMethodName(StackTraceElement[] stack) {
        for (StackTraceElement frame : stack) {
            String cls = frame.getClassName();
            if (cls.startsWith("Ellithium.") || cls.startsWith("org.openqa.selenium")
                    || cls.startsWith("java.") || cls.startsWith("sun.")
                    || cls.startsWith("io.cucumber") || cls.startsWith("io.qameta")
                    || cls.startsWith("org.testng") || cls.startsWith("net.bytebuddy")) {
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
     * Executes a probe function on an element and returns the result, or {@code defaultValue}
     * if the element cannot be found or interacted with after all healing attempts.
     * All probe methods (isDisplayed, isEnabled, isClickable, getText checks, etc.) delegate here
     * instead of each having their own try/catch.
     */
    protected <R> R performAndGetOrDefault(By locator, int timeout, int pollingEvery,
                                            Function<WebElement, R> action, R defaultValue) {
        try {
            return performAndGet(locator, timeout, pollingEvery, action);
        } catch (Exception e) {
            return defaultValue;
        }
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
            WebElement frame = findWebElement(locator);
            try {
                return driver.switchTo().frame(frame);
            } catch (WebDriverException ignored) {
                throw e;
            }
        }
    }

    // ──────────────────────── Stale-element retry helpers ────────────────────────

    protected static final int STALE_MAX_RETRIES = 2;

    /**
     * Visibility-waited find with NO healing, respecting a previously cached healed locator.
     * On a visibility timeout it returns the present element if the locator still resolves — so the
     * caller's recovery path can handle a hidden/obscured element — otherwise it surfaces
     * {@link NoSuchElementException} for the heal path.
     */
    private WebElement rawVisibleElement(By locator, int timeout, int polling) {
        By effective = HEALING_PORT.getCachedLocator(driver, locator);
        if (effective == null) effective = locator;
        try {
            return getFluentWait(timeout, polling)
                    .until(ExpectedConditions.visibilityOfElementLocated(effective));
        } catch (TimeoutException te) {
            List<WebElement> present = driver.findElements(effective);
            if (!present.isEmpty()) return present.get(0);
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
        WebDriverException lastException = null;
        boolean healed = false;
        for (int attempt = 0; attempt <= STALE_MAX_RETRIES; attempt++) {
            WebElement el;
            try {
                el = rawVisibleElement(locator, timeout, polling);
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
                action.accept(el);
                return;
            } catch (StaleElementReferenceException e) {
                lastException = e;
            } catch (WebDriverException e) {
                lastException = e;
                SeleniumFailurePolicy.FailureClass fc = SeleniumFailurePolicy.classify(e);
                if (fc == SeleniumFailurePolicy.FailureClass.RECOVER_INTERACTION) {
                    WebElement target = recovery.resolveInteractable(locator, timeout, polling);
                    if (target != null) {
                        try { action.accept(target); return; }
                        catch (WebDriverException retryEx) { lastException = retryEx; }
                    } else if (!healed) {
                        healed = true;
                        WebElement healedEl = healOnce(locator);
                        if (healedEl != null) {
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
            WebElement target = recovery.resolveInteractable(locator, timeout, polling);
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
        WebDriverException lastException = null;
        boolean healed = false;
        for (int attempt = 0; attempt <= STALE_MAX_RETRIES; attempt++) {
            WebElement el;
            try {
                el = rawVisibleElement(locator, timeout, polling);
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
            } catch (StaleElementReferenceException e) {
                lastException = e;
            } catch (WebDriverException e) {
                lastException = e;
                SeleniumFailurePolicy.FailureClass fc = SeleniumFailurePolicy.classify(e);
                if (fc == SeleniumFailurePolicy.FailureClass.RECOVER_INTERACTION) {
                    WebElement target = recovery.resolveInteractable(locator, timeout, polling);
                    if (target != null) {
                        try { return action.apply(target); }
                        catch (WebDriverException retryEx) { lastException = retryEx; }
                    } else if (!healed) {
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
