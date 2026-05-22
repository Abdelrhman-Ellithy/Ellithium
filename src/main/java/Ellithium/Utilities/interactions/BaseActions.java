package Ellithium.Utilities.interactions;

import Ellithium.Utilities.ai.AISelfHealer;
import Ellithium.Utilities.ai.BaselineStore;
import Ellithium.Utilities.ai.SemanticLocatorResolver;
import Ellithium.Utilities.ai.models.ElementFingerprint;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class BaseActions<T extends WebDriver> {
    protected final T driver;
    protected BaseActions(T driver) {
        this.driver = driver;
    }
    /**
     * Gets a FluentWait instance with specified timeout and polling interval.
     * @param timeoutInSeconds Maximum wait time in seconds
     * @param pollingEveryInMillis Polling interval in milliseconds
     * @return FluentWait instance
     */
    protected FluentWait<T> getFluentWait(int timeoutInSeconds, int pollingEveryInMillis) {
        return WaitManager.getFluentWait(driver, timeoutInSeconds, pollingEveryInMillis);
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
            // Silently capture fingerprint for Tier 1 baseline healing
            BaselineStore.capture(driver, locator, element);
            return element;
        } catch (NoSuchElementException | org.openqa.selenium.InvalidSelectorException e) {
            // InvalidSelectorException covers empty/blank locators (e.g. By.tagName(""))
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();

            // Extract context once — used by all tiers and container resolution
            String actionType   = extractActionFromStack(stack);
            String callerMethod = extractCallerMethodName(stack);
            String fieldName    = extractFieldNameFromStack(stack, locator);
            String locatorValue = extractLocatorValue(locator);
            ElementFingerprint baseline = BaselineStore.getBaseline(locator.toString());

            // TIER 1: Algorithmic match against stored baseline (fast, free, deterministic)
            WebElement algorithmicMatch = BaselineStore.tryAlgorithmicHeal(driver, locator);
            algorithmicMatch = resolveInteractiveElement(algorithmicMatch, actionType, "TIER 1");
            if (algorithmicMatch != null) {
                double t1Score = BaselineStore.getLastHealScore();
                BaselineStore.capture(driver, locator, algorithmicMatch, t1Score, 1);
                AISelfHealer.queueSourcePatch(locator, algorithmicMatch, stack, t1Score, 1);
                return guardStaleHeal(algorithmicMatch);
            }

            // TIER 2: Semantic Strategy Search — TEMPORARILY DISABLED
            // WebElement semanticMatch = SemanticLocatorResolver.trySemanticHeal(
            //         driver, callerMethod, fieldName, actionType, locatorValue, baseline);
            // semanticMatch = resolveInteractiveElement(semanticMatch, actionType, "TIER 2");
            // if (semanticMatch != null) {
            //     BaselineStore.capture(driver, locator, semanticMatch);
            //     AISelfHealer.queueSourcePatch(locator, semanticMatch, stack);
            //     return semanticMatch;
            // }

            // TIER 3: Local ONNX Embedding (active when fine-tuned model is present)
            if (Ellithium.Utilities.ai.ONNXEmbeddingHealer.isAvailable()) {
                WebElement onnxMatch = Ellithium.Utilities.ai.ONNXEmbeddingHealer.tryEmbeddingHeal(
                        driver, locator, actionType, callerMethod, fieldName, locatorValue, baseline);
                // Tier 3 already filters to interactive elements — resolve is a safety net
                onnxMatch = resolveInteractiveElement(onnxMatch, actionType, "TIER 3");
                if (onnxMatch != null) {
                    double t3Score = Ellithium.Utilities.ai.ONNXEmbeddingHealer.getLastHealScore();
                    BaselineStore.capture(driver, locator, onnxMatch, t3Score, 3);
                    AISelfHealer.queueSourcePatch(locator, onnxMatch, stack, t3Score, 3);
                    return guardStaleHeal(onnxMatch);
                }
            }

            // TIER 4: LLM-based semantic healing (slow, costly, but handles drastic changes).
            // attemptHeal performs its own gated capture + patch (it owns the LLM confidence).
            WebElement llmMatch = AISelfHealer.attemptHeal(driver, locator, stack);
            llmMatch = resolveInteractiveElement(llmMatch, actionType, "TIER 4");
            if (llmMatch != null) {
                return guardStaleHeal(llmMatch);
            }

            // All healing tiers failed — throw AssertionError so Allure marks as "failed" not "broken"
            throw new AssertionError("Element not found and could not be healed: " + locator
                    + " | All healing tiers exhausted (Tier 1: Algorithmic, Tier 2: Semantic, Tier 3: ONNX, Tier 4: LLM)", e);
        }
    }

    /**
     * For click-type actions, resolves container elements (form, div, section…) to their
     * inner interactive child (submit button, button, input[type=submit]).
     *
     * Rationale: The LLM and other tiers sometimes return a container element whose ID or
     * text matches the semantic intent (e.g. {@code <form id="login">}) but clicking a
     * container does not trigger form submission. This method descends one level to the
     * actual clickable element inside.
     *
     * Returns the original element unchanged when:
     *  - the action is not a click type
     *  - the element is already interactive (button, input, a, or role=button)
     *  - the element is null
     *
     * Returns null when the element IS a container but contains no visible interactive child
     * (signals the tier to fall through rather than click something wrong).
     */
    private static WebElement resolveInteractiveElement(WebElement healed, String actionType,
                                                         String tierLabel) {
        if (healed == null || !isClickLikeAction(actionType)) return healed;
        try {
            String tag = healed.getTagName().toLowerCase();
            // Already an interactive leaf — nothing to resolve
            if (INTERACTIVE_TAGS.contains(tag)) return healed;

            // div/span with explicit interactive role is fine as-is
            String role = healed.getAttribute("role");
            if (role != null && INTERACTIVE_ROLES.contains(role)) return healed;

            // Container element — resolve to inner interactive child
            for (String selector : INNER_INTERACTIVE_SELECTORS) {
                try {
                    WebElement inner = healed.findElement(By.cssSelector(selector));
                    if (inner.isDisplayed()) {
                        Ellithium.core.reporting.Reporter.log(
                                "[" + tierLabel + "] Resolved container <" + tag
                                + "> → inner interactive <" + inner.getTagName() + ">",
                                Ellithium.core.logging.LogLevel.INFO_YELLOW);
                        return inner;
                    }
                } catch (NoSuchElementException ignored) {}
            }

            // Container with no interactive child — reject
            Ellithium.core.reporting.Reporter.log(
                    "[" + tierLabel + "] Healed element is container <" + tag
                    + "> with no interactive child for click action — skipping",
                    Ellithium.core.logging.LogLevel.WARN);
            return null;
        } catch (Exception ex) {
            return healed; // Defensive: never block healing on an unexpected exception
        }
    }

    /**
     * Guards against a healed element going stale between heal and use. The healing cascade
     * (especially the two-stage Tier 3 retrieve+rerank and Tier 4 LLM round-trips) widens the
     * window in which an SPA can re-render and detach the returned element. We reconstruct a
     * locator while the element is still fresh, probe for staleness, and re-resolve once if needed.
     */
    private WebElement guardStaleHeal(WebElement healed) {
        if (healed == null) return null;
        By reconstructed = ElementFingerprint.reconstructLocator(healed); // captured while fresh
        try {
            healed.isEnabled();   // cheap probe — throws if the element is detached/stale
            return healed;
        } catch (org.openqa.selenium.StaleElementReferenceException stale) {
            if (reconstructed != null) {
                try {
                    WebElement refreshed = driver.findElement(reconstructed);
                    Ellithium.core.reporting.Reporter.log(
                            "Healed element went stale before use — re-resolved via "
                            + reconstructed, Ellithium.core.logging.LogLevel.INFO_YELLOW);
                    return refreshed;
                } catch (Exception ignored) {}
            }
            return healed; // best effort — let the caller's own retry handle it
        } catch (Exception other) {
            return healed;
        }
    }

    private static final java.util.Set<String> INTERACTIVE_TAGS = new java.util.HashSet<>(
            java.util.Arrays.asList("button", "a", "input", "select", "textarea", "option"));

    private static final java.util.Set<String> INTERACTIVE_ROLES = new java.util.HashSet<>(
            java.util.Arrays.asList("button", "link", "menuitem", "menuitemcheckbox",
                    "menuitemradio", "tab", "option", "checkbox", "radio"));

    // Tried in priority order: submit button first (most specific), then any button, then input
    private static final String[] INNER_INTERACTIVE_SELECTORS = {
            "button[type='submit']",
            "input[type='submit']",
            "button",
            "input[type='button']",
            "a"
    };

    private static boolean isClickLikeAction(String actionType) {
        if (actionType == null || actionType.equals("unknown")) return false;
        String lower = actionType.toLowerCase();
        return lower.contains("click") || lower.contains("tap")
                || lower.contains("press") || lower.contains("hover");
    }

    /**
     * Waits for an element to be visible, returning it. 
     * If a TimeoutException or InvalidSelectorException occurs, it falls back to findWebElement()
     * which triggers AI Self-Healing if the element is missing or the locator is invalid.
     */
    protected WebElement waitForVisibilityAndFindElement(By locator, int timeout, int pollingEvery) {
        try {
            // W1 fix: use the WebElement returned directly by the condition — avoids a
            // second findElement() call that races with page changes after the wait resolves.
            WebElement element = getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return element;
        } catch (org.openqa.selenium.TimeoutException | org.openqa.selenium.InvalidSelectorException e) {
            // Covers both missing elements and invalid/empty locators
            return findWebElement(locator);
        }
    }

    /**
     * Waits for all elements to be visible, returning them.
     * If a TimeoutException occurs, attempts to heal the locator before querying again.
     */
    protected List<WebElement> waitForVisibilityAndFindElements(By locator, int timeout, int pollingEvery) {
        try {
            getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
            return driver.findElements(locator);
        } catch (org.openqa.selenium.TimeoutException e) {
            StackTraceElement[] stack   = Thread.currentThread().getStackTrace();
            String actionType           = extractActionFromStack(stack);
            String callerMethod         = extractCallerMethodName(stack);
            String fieldName            = extractFieldNameFromStack(stack, locator);
            String locatorValue         = extractLocatorValue(locator);
            ElementFingerprint baseline = BaselineStore.getBaseline(locator.toString());

            // TIER 1: Algorithmic baseline match
            WebElement t1 = BaselineStore.tryAlgorithmicHeal(driver, locator);
            t1 = resolveInteractiveElement(t1, actionType, "TIER 1");
            if (t1 != null) {
                double t1Score = BaselineStore.getLastHealScore();
                BaselineStore.capture(driver, locator, t1, t1Score, 1);
                AISelfHealer.queueSourcePatch(locator, t1, stack, t1Score, 1);
                By healed = ElementFingerprint.reconstructLocator(t1);
                return driver.findElements(healed != null ? healed : locator);
            }

            // TIER 3: Local ONNX embedding
            if (Ellithium.Utilities.ai.ONNXEmbeddingHealer.isAvailable()) {
                WebElement t3 = Ellithium.Utilities.ai.ONNXEmbeddingHealer.tryEmbeddingHeal(
                        driver, locator, actionType, callerMethod, fieldName, locatorValue, baseline);
                if (t3 != null) {
                    By healed = ElementFingerprint.reconstructLocator(t3);
                    if (healed != null) {
                        double t3Score = Ellithium.Utilities.ai.ONNXEmbeddingHealer.getLastHealScore();
                        BaselineStore.capture(driver, locator, t3, t3Score, 3);
                        AISelfHealer.queueSourcePatch(locator, t3, stack, t3Score, 3);
                        return driver.findElements(healed);
                    }
                }
            }

            // TIER 4: LLM — last resort
            By healedLocator = AISelfHealer.healLocator(driver, locator, stack);
            if (healedLocator != null) {
                AISelfHealer.CachedLocator cached = AISelfHealer.getGlobalHealedCache().get(locator.toString());
                if (cached != null) {
                    Ellithium.core.reporting.Reporter.log("AI Self-Healing (cached list): reusing healed locator "
                            + cached.newLocator + " for field '" + cached.originalField
                            + "' (original: " + locator.toString() + ")", Ellithium.core.logging.LogLevel.INFO_YELLOW);
                }
                return driver.findElements(healedLocator);
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
        AISelfHealer.CachedLocator cached = AISelfHealer.getGlobalHealedCache().get(locator.toString());
        if (cached != null) {
            return driver.findElements(cached.newLocator);
        }
        return driver.findElements(locator);
    }

    /**
     * Safely iterates over elements using a consumer function.
     * Re-locates elements on each iteration to prevent stale element exceptions.
     * Handles dynamic list size changes gracefully.
     * 
     * @param locator Element locator
     * @param action Consumer function to apply to each element
     */
    protected void forEachElementSafely(By locator, Consumer<WebElement> action) {
        int currentIndex = 0;
        int consecutiveFailures = 0;
        final int maxConsecutiveFailures = 3;
        
        while (true) {
            List<WebElement> currentElements = findWebElements(locator);
            if (currentIndex >= currentElements.size()) {
                break; // No more elements or list shrunk
            }
            
            try {
                WebElement element = currentElements.get(currentIndex);
                action.accept(element);
                currentIndex++;
                consecutiveFailures = 0; // Reset on success
            } catch (StaleElementReferenceException e) {
                // Element became stale, re-query and retry same index
                consecutiveFailures++;
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    // Too many consecutive failures, move to next index
                    currentIndex++;
                    consecutiveFailures = 0;
                }
                // Continue to retry same index
            } catch (IndexOutOfBoundsException e) {
                // List shrunk during iteration, exit gracefully
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
    protected <R> List<R> mapElementsSafely(By locator, Function<WebElement, R> mapper) {
        List<R> results = new ArrayList<>();
        int currentIndex = 0;
        int consecutiveFailures = 0;
        final int maxConsecutiveFailures = 3;
        
        while (true) {
            List<WebElement> currentElements = findWebElements(locator);
            if (currentIndex >= currentElements.size()) {
                break;
            }
            
            try {
                WebElement element = currentElements.get(currentIndex);
                R result = mapper.apply(element);
                results.add(result);
                currentIndex++;
                consecutiveFailures = 0; // Reset on success
            } catch (StaleElementReferenceException e) {
                // Re-query and retry same index
                consecutiveFailures++;
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    // Too many consecutive failures, skip this index
                    currentIndex++;
                    consecutiveFailures = 0;
                }
                // Continue to retry same index
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
    protected WebElement findElementByIndexSafely(By locator, int index) {
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
    protected int getElementCount(By locator) {
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
    protected <R> List<R> mapSelectOptionsSafely(By locator, Function<WebElement, R> mapper) {
        List<R> results = new ArrayList<>();
        int currentIndex = 0;
        int consecutiveFailures = 0;
        final int maxConsecutiveFailures = 3;
        
        while (true) {
            try {
                org.openqa.selenium.support.ui.Select dropDown = new org.openqa.selenium.support.ui.Select(findWebElement(locator));
                List<WebElement> currentOptions = dropDown.getAllSelectedOptions();
                if (currentIndex >= currentOptions.size()) {
                    break;
                }
                WebElement option = currentOptions.get(currentIndex);
                R result = mapper.apply(option);
                results.add(result);
                currentIndex++;
                consecutiveFailures = 0; // Reset on success
            } catch (StaleElementReferenceException | IndexOutOfBoundsException e) {
                // Re-query and retry same index
                consecutiveFailures++;
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    // Too many consecutive failures, skip this index
                    currentIndex++;
                    consecutiveFailures = 0;
                }
                // Continue to retry same index
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
    protected void forEachSelectElementSafely(By locator, Consumer<org.openqa.selenium.support.ui.Select> action) {
        int currentIndex = 0;
        int consecutiveFailures = 0;
        final int maxConsecutiveFailures = 3;
        
        while (true) {
            try {
                List<WebElement> currentElements = findWebElements(locator);
                if (currentIndex >= currentElements.size()) {
                    break; // No more elements or list shrunk
                }
                
                WebElement element = currentElements.get(currentIndex);
                org.openqa.selenium.support.ui.Select select = new org.openqa.selenium.support.ui.Select(element);
                action.accept(select);
                currentIndex++;
                consecutiveFailures = 0; // Reset on success
            } catch (StaleElementReferenceException e) {
                // Element became stale, re-query and retry same index
                consecutiveFailures++;
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    // Too many consecutive failures, move to next index
                    currentIndex++;
                    consecutiveFailures = 0;
                }
                // Continue to retry same index
            } catch (IndexOutOfBoundsException e) {
                // List shrunk during iteration, exit gracefully
                break;
            }
        }
    }

    // ──────────────────────── Context Extraction for Tier 2 ────────────────────────

    /**
     * Extracts the Ellithium interaction method name from the stack (e.g., "sendData", "clickOnElement").
     */
    private static String extractActionFromStack(StackTraceElement[] stack) {
        for (StackTraceElement frame : stack) {
            String cls = frame.getClassName();
            if (cls.startsWith("Ellithium.Utilities.interactions.")) {
                String method = frame.getMethodName();
                if (!method.equals("findWebElement") && !method.equals("waitForVisibilityAndFindElement")
                        && !method.equals("getFluentWait") && !method.equals("findWebElements")
                        && !method.equals("waitForVisibilityAndFindElements")
                        && !method.equals("extractActionFromStack")) {
                    return method;
                }
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
        return null;
    }

    /**
     * Attempts to extract the By field name from source code at the call site.
     */
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
                        java.util.List<String> lines = java.nio.file.Files.readAllLines(
                                java.nio.file.Paths.get(path));
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
}
