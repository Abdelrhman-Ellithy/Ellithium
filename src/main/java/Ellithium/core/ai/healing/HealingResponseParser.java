package Ellithium.core.ai.healing;

import Ellithium.core.ai.models.HealingResult;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;

class HealingResponseParser {

    static List<HealingResult> parseMultiCandidateResponse(String response) {
        List<HealingResult> results = new ArrayList<>();
        if (response == null || response.trim().isEmpty()) return results;

        String cleaned = response.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
        }

        try {
            com.google.gson.JsonElement root = com.google.gson.JsonParser.parseString(cleaned);

            if (root.isJsonObject()) {
                com.google.gson.JsonObject obj = root.getAsJsonObject();

                if (obj.has("candidates") && obj.get("candidates").isJsonArray()) {
                    com.google.gson.JsonArray arr = obj.getAsJsonArray("candidates");
                    for (com.google.gson.JsonElement el : arr) {
                        HealingResult r = parseSingleCandidate(el.getAsJsonObject());
                        if (r != null) results.add(r);
                    }
                } else if (obj.has("locator")) {
                    HealingResult r = parseSingleCandidate(obj);
                    if (r != null) results.add(r);
                }
            } else if (root.isJsonArray()) {
                for (com.google.gson.JsonElement el : root.getAsJsonArray()) {
                    HealingResult r = parseSingleCandidate(el.getAsJsonObject());
                    if (r != null) results.add(r);
                }
            }
        } catch (Exception e) {
            Reporter.log("Failed to parse AI healing response: " + e.getMessage(), LogLevel.ERROR);
        }
        return results;
    }

    static HealingResult parseSingleCandidate(com.google.gson.JsonObject json) {
        try {
            String locator = json.get("locator").getAsString();
            double confidence = json.has("confidence") ? json.get("confidence").getAsDouble() : 0.5;
            String reasoning = json.has("reasoning") ? json.get("reasoning").getAsString() : "";
            return new HealingResult(locator, confidence, reasoning);
        } catch (Exception e) {
            return null;
        }
    }

    static By parseByFromExpression(String expression) {
        try {
            if (expression.startsWith("By.id(")) return By.id(extractValue(expression));
            else if (expression.startsWith("By.cssSelector(")) return By.cssSelector(extractValue(expression));
            else if (expression.startsWith("By.xpath(")) return By.xpath(extractValue(expression));
            else if (expression.startsWith("By.name(")) return By.name(extractValue(expression));
            else if (expression.startsWith("By.className(")) return By.className(extractValue(expression));
            else if (expression.startsWith("By.linkText(")) return By.linkText(extractValue(expression));
            else if (expression.startsWith("By.partialLinkText(")) return By.partialLinkText(extractValue(expression));
            else if (expression.startsWith("By.tagName(")) return By.tagName(extractValue(expression));
            else if (expression.startsWith("AppiumBy.accessibilityId(")) return AppiumBy.accessibilityId(extractValue(expression));
            else if (expression.startsWith("AppiumBy.androidUIAutomator(")) return AppiumBy.androidUIAutomator(extractValue(expression));
            else if (expression.startsWith("AppiumBy.androidViewTag(")) return AppiumBy.androidViewTag(extractValue(expression));
            else if (expression.startsWith("AppiumBy.androidDataMatcher(")) return AppiumBy.androidDataMatcher(extractValue(expression));
            else if (expression.startsWith("AppiumBy.iOSClassChain(")) return AppiumBy.iOSClassChain(extractValue(expression));
            else if (expression.startsWith("AppiumBy.iOSNsPredicateString(")) return AppiumBy.iOSNsPredicateString(extractValue(expression));
            else if (expression.startsWith("AppiumBy.image(")) return AppiumBy.image(extractValue(expression));
            else if (expression.startsWith("AppiumBy.custom(")) return AppiumBy.custom(extractValue(expression));
        } catch (Exception e) {
            Reporter.log("Failed to parse By expression: " + expression, LogLevel.ERROR);
        }
        return null;
    }

    static String extractValue(String expression) {
        int start = expression.indexOf('"') + 1;
        int end = expression.lastIndexOf('"');
        return expression.substring(start, end);
    }

    static boolean isStableLocatorStrategy(By locator) {
        if (locator == null) return false;
        String s = locator.toString();
        if (s.startsWith("By.id:") || s.startsWith("By.name:")
                || s.startsWith("AppiumBy.accessibilityId:")) return true;
        if (s.startsWith("By.cssSelector:")) {
            return s.contains("[data-testid") || s.contains("[data-test") || s.contains("[aria-label")
                    || s.contains("#") || s.contains("[name=");
        }
        return false;
    }
}
