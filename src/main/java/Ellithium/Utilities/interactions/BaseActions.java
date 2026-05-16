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

            // TIER 1: Algorithmic match against stored baseline (fast, free, deterministic)
            WebElement algorithmicMatch = BaselineStore.tryAlgorithmicHeal(driver, locator);
            if (algorithmicMatch != null) {
                // Save baseline + queue source patch for POM correction
                BaselineStore.capture(driver, locator, algorithmicMatch);
                AISelfHealer.queueSourcePatch(locator, algorithmicMatch, stack);
                return algorithmicMatch;
            }

            // TIER 1.5: Semantic Strategy Search (zero cost, uses method/field name context)
            String actionType = extractActionFromStack(stack);
            String callerMethod = extractCallerMethodName(stack);
            String fieldName = extractFieldNameFromStack(stack, locator);
            String locatorValue = extractLocatorValue(locator);
            ElementFingerprint baseline = BaselineStore.getBaseline(locator.toString());
            WebElement semanticMatch = SemanticLocatorResolver.trySemanticHeal(
                    driver, callerMethod, fieldName, actionType, locatorValue, baseline);
            if (semanticMatch != null) {
                // Save baseline under the ORIGINAL broken locator key
                // so next test heals via Tier 1 (instant) instead of Tier 1.5
                BaselineStore.capture(driver, locator, semanticMatch);
                AISelfHealer.queueSourcePatch(locator, semanticMatch, stack);
                return semanticMatch;
            }

            // TIER 2: LLM-based semantic healing (slow, costly, but handles drastic changes)
            WebElement llmMatch = AISelfHealer.attemptHeal(driver, locator, stack);
            if (llmMatch != null) {
                // Save baseline under original key for Tier 1 reuse
                BaselineStore.capture(driver, locator, llmMatch);
                return llmMatch;
            }

            // All healing tiers failed — throw AssertionError so Allure marks as "failed" not "broken"
            throw new AssertionError("Element not found and could not be healed: " + locator
                    + " | All healing tiers exhausted (Tier 1: Baseline, Tier 1.5: Semantic, Tier 2: LLM)", e);
        }
    }

    /**
     * Waits for an element to be visible, returning it. 
     * If a TimeoutException or InvalidSelectorException occurs, it falls back to findWebElement()
     * which triggers AI Self-Healing if the element is missing or the locator is invalid.
     */
    protected WebElement waitForVisibilityAndFindElement(By locator, int timeout, int pollingEvery) {
        try {
            getFluentWait(timeout, pollingEvery)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return driver.findElement(locator);
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
            // List is missing entirely. Attempt to heal the locator.
            By healedLocator = AISelfHealer.healLocator(driver, locator, Thread.currentThread().getStackTrace());
            if (healedLocator != null) {
                // If the cache was hit or AI found it, log it
                AISelfHealer.CachedLocator cached = AISelfHealer.getGlobalHealedCache().get(locator.toString());
                if (cached != null) {
                    Ellithium.core.reporting.Reporter.log("AI Self-Healing (cached list): reusing healed locator " 
                            + cached.newLocator + " for field '" + cached.originalField 
                            + "' (original: " + locator.toString() + ")", Ellithium.core.logging.LogLevel.INFO_YELLOW);
                }
                return driver.findElements(healedLocator);
            }
            // If healing fails, return the empty list (standard Selenium behavior for findElements)
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

    // ──────────────────────── Context Extraction for Tier 1.5 ────────────────────────

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
