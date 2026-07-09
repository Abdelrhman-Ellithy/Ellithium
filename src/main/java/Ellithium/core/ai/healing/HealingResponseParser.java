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
        int open = expression.indexOf('(');
        int close = expression.lastIndexOf(')');
        if (open < 0 || close <= open) return "";
        String inner = expression.substring(open + 1, close).trim();
        if (inner.length() >= 2) {
            char q = inner.charAt(0);
            if ((q == '"' || q == '\'') && inner.charAt(inner.length() - 1) == q) {
                return unescapeJava(inner.substring(1, inner.length() - 1));
            }
        }
        return inner;
    }

    /**
     * Inverse of the escaping used to build a Java string literal (see
     * {@code AISelfHealer#escapeJava}). The LLM returns locator expressions as literal Java source
     * (e.g. {@code By.xpath("//div[@id=\"x\"]")}); without unescaping, the literal {@code \"} stays
     * in the parsed value and produces an XPath/CSS string that differs from what the model intended.
     */
    private static String unescapeJava(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"'  -> { out.append('"');  i++; }
                    case '\'' -> { out.append('\''); i++; }
                    case '\\' -> { out.append('\\'); i++; }
                    case 'n'  -> { out.append('\n'); i++; }
                    case 'r'  -> { out.append('\r'); i++; }
                    case 't'  -> { out.append('\t'); i++; }
                    default   -> out.append(c);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

}
