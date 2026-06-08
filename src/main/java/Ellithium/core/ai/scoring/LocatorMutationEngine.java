package Ellithium.core.ai.scoring;

import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tier 1 pre-pass and Tier 2 pre-pass: tries O(1) cheap mutations of a broken locator
 * before any full DOM scan or semantic strategy generation.
 *
 * <p>Covers the vast majority of real-world locator breakage:
 * team renames, coding convention changes (camelCase ↔ kebab ↔ snake),
 * attribute migrations (id → data-testid), and partial-name drift.</p>
 *
 * <p>No DOM scan — each mutation is a direct WebDriver lookup.
 * Zero cost when the locator is simply wrong (no elements found).</p>
 */
public class LocatorMutationEngine {

    // Regex: splits camelCase and PascalCase into tokens
    private static final Pattern CAMEL_SPLIT = Pattern.compile(
            "(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|[-_]");

    // Regex: extracts (method, value) from By.toString() e.g. "By.id: loginBtn"
    private static final Pattern BY_PARSE = Pattern.compile("By\\.([a-zA-Z]+):\\s*(.*)");

    // ──────────────────────── Public API ────────────────────────

    /**
     * Tries O(1) mutations of the broken locator before any DOM scan.
     * Returns the first mutation that finds a single element passing baseline validation.
     *
     * @param brokenLocator  The locator that failed
     * @param driver         The WebDriver
     * @param baseline       Stored baseline fingerprint for cross-validation (may be null)
     * @return The healed WebElement, or null if no mutation succeeded
     */
    public static WebElement tryMutations(By brokenLocator, WebDriver driver,
                                          ElementFingerprint baseline) {
        List<By> mutations = generateMutations(brokenLocator);
        if (mutations.isEmpty()) return null;

        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            for (By mutation : mutations) {
                try {
                    WebElement found = driver.findElement(mutation);
                    if (baseline != null && !mutationCrossValidates(driver, baseline, found)) continue;
                    Ellithium.core.execution.listener.seleniumListener.resumeLogging();
                    Reporter.log("[TIER 1] mutation: " + brokenLocator + " → " + mutation, LogLevel.INFO_GREEN);
                    return found;
                } catch (org.openqa.selenium.StaleElementReferenceException e) {
                    try {
                        WebElement retried = driver.findElement(mutation);
                        if (baseline == null || mutationCrossValidates(driver, baseline, retried)) {
                            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
                            Reporter.log("[TIER 1] mutation (stale-retry): " + brokenLocator + " → " + mutation, LogLevel.INFO_GREEN);
                            return retried;
                        }
                    } catch (Exception ignored) {}
                } catch (NoSuchElementException | org.openqa.selenium.InvalidSelectorException ignored) {}
            }
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }
        return null;
    }

    /**
     * A mutation match is trusted only with real baseline agreement: similarity ≥ 0.55, and when the
     * stored tag is known and the candidate's tag differs (button↔a, input↔div), a stronger ≥ 0.75 so a
     * same-named element of a different KIND is not accepted on a weak partial-id overlap.
     */
    private static final double MUTATION_ACCEPT_MIN = 0.55;
    private static final double MUTATION_ACCEPT_TAG_MISMATCH = 0.75;

    private static boolean mutationCrossValidates(WebDriver driver,
                                                   ElementFingerprint baseline,
                                                   WebElement found) {
        java.util.List<java.util.Map<String, Object>> batch =
                Ellithium.core.ai.dom.CandidateAttributeBatcher.fetch(driver, java.util.List.of(found));
        double score;
        String foundTag = null;
        if (batch != null && !batch.isEmpty() && batch.get(0) != null) {
            java.util.Map<String, Object> attrs = batch.get(0);
            score = baseline.scoreSimilarity(attrs, null);
            Object t = attrs.get("tag");
            if (t != null) foundTag = t.toString();
        } else {
            score = baseline.scoreSimilarity(found);
            try { foundTag = found.getTagName(); } catch (Exception ignored) {}
        }
        String baseTag = baseline.getTagName();
        if (baseTag != null && !baseTag.isBlank() && foundTag != null && !baseTag.equalsIgnoreCase(foundTag)) {
            return score >= MUTATION_ACCEPT_TAG_MISMATCH;
        }
        return score >= MUTATION_ACCEPT_MIN;
    }

    /**
     * Generates all By mutations for a broken locator without touching the DOM.
     *
     * @param brokenLocator The failed locator
     * @return Ordered list of By mutations to try (most specific first)
     */
    private static final int MUTATION_CACHE_MAX = 500;
    private static final java.util.Map<String, List<By>> MUTATION_CACHE =
            java.util.Collections.synchronizedMap(
                    new java.util.LinkedHashMap<>(MUTATION_CACHE_MAX, 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(java.util.Map.Entry<String, List<By>> e) {
                            return size() > MUTATION_CACHE_MAX;
                        }
                    });

    public static List<By> generateMutations(By brokenLocator) {
        String key = brokenLocator.toString();
        List<By> cached = MUTATION_CACHE.get(key);
        if (cached != null) return cached;

        Matcher m = BY_PARSE.matcher(key);
        if (!m.find()) {
            MUTATION_CACHE.put(key, List.of());
            return List.of();
        }

        String method = m.group(1);
        String value  = m.group(2).trim();
        if (value.isBlank()) {
            MUTATION_CACHE.put(key, List.of());
            return List.of();
        }

        List<By> mutations = new ArrayList<>();
        switch (method) {
            case "id"    -> addIdMutations(mutations, value);
            case "name"  -> addNameMutations(mutations, value);
            case "cssSelector" -> addCssMutations(mutations, value);
            case "xpath" -> addXpathMutations(mutations, value);
            case "className" -> addClassMutations(mutations, value);
            default -> addGenericMutations(mutations, value);
        }
        List<By> immut = List.copyOf(mutations);
        MUTATION_CACHE.put(key, immut);
        return immut;
    }

    /**
     * Generates string mutations of an identifier value.
     * "loginBtn" → ["login-btn", "login_btn", "loginbutton", "login", "btn-login", ...]
     */
    public static List<String> generateValueMutations(String value) {
        List<String> tokens = tokenize(value);
        LinkedHashSet<String> results = new LinkedHashSet<>();

        if (tokens.isEmpty() || (tokens.size() == 1 && tokens.get(0).equals(value.toLowerCase()))) {
            return List.of();
        }

        // Convention variants
        results.add(String.join("-", tokens));                          // kebab-case
        results.add(String.join("_", tokens));                          // snake_case
        results.add(String.join("", tokens));                           // nospace
        results.add(toCamelCase(tokens));                               // camelCase (may equal original)
        results.add(toPascalCase(tokens));                              // PascalCase

        // Suffix/prefix strip: remove last token (often a UI suffix like "Btn", "Input")
        if (tokens.size() > 1) {
            List<String> withoutLast  = tokens.subList(0, tokens.size() - 1);
            List<String> withoutFirst = tokens.subList(1, tokens.size());
            results.add(String.join("-", withoutLast));
            results.add(String.join("_", withoutLast));
            results.add(toCamelCase(withoutLast));
            results.add(String.join("-", withoutFirst));
            results.add(String.join("_", withoutFirst));
            results.add(toCamelCase(withoutFirst));
        }

        // Reversed token order: "submitLogin" → "login-submit"
        if (tokens.size() == 2) {
            List<String> reversed = List.of(tokens.get(1), tokens.get(0));
            results.add(String.join("-", reversed));
            results.add(toCamelCase(reversed));
        }

        // Remove the original value itself (no point retrying what already failed)
        results.remove(value);
        results.remove(value.toLowerCase());

        return new ArrayList<>(results);
    }

    // ──────────────────────── Mutation Generators by Locator Type ────────────────────────

    private static void addIdMutations(List<By> out, String value) {
        List<String> variants = generateValueMutations(value);
        // Same attribute, different naming convention
        for (String v : variants) {
            if (!v.isBlank()) out.add(By.id(v));
        }
        // Attribute swap: id → name (id often migrated to name)
        out.add(By.name(value));
        for (String v : variants) {
            if (!v.isBlank()) out.add(By.name(v));
        }
        // id → data-testid (common refactor in modern apps)
        out.add(By.cssSelector("[data-testid='" + value + "']"));
        for (String v : variants) {
            if (!v.isBlank()) out.add(By.cssSelector("[data-testid='" + v + "']"));
        }
        // id → aria-label
        out.add(By.cssSelector("[aria-label='" + value + "']"));
        // Contains fallback
        out.add(By.cssSelector("[id*='" + value + "']"));
        List<String> tokens = tokenize(value);
        if (tokens.size() >= 2) {
            out.add(By.cssSelector("[id*='" + tokens.get(0) + "']"));
        }
    }

    private static void addNameMutations(List<By> out, String value) {
        List<String> variants = generateValueMutations(value);
        for (String v : variants) {
            if (!v.isBlank()) out.add(By.name(v));
        }
        // name → id swap
        out.add(By.id(value));
        for (String v : variants) {
            if (!v.isBlank()) out.add(By.id(v));
        }
        // name → data-testid
        out.add(By.cssSelector("[data-testid='" + value + "']"));
        // Contains fallback
        out.add(By.cssSelector("[name*='" + value + "']"));
    }

    private static void addCssMutations(List<By> out, String value) {
        // Class name mutations: ".login-btn" → ".loginBtn", ".login_btn"
        if (value.startsWith(".")) {
            String cls = value.substring(1);
            List<String> variants = generateValueMutations(cls);
            for (String v : variants) {
                if (!v.isBlank()) out.add(By.cssSelector("." + v));
            }
            out.add(By.cssSelector("[class*='" + cls + "']"));
            List<String> tokens = tokenize(cls);
            if (!tokens.isEmpty()) {
                out.add(By.cssSelector("[class*='" + tokens.get(0) + "']"));
            }
            return;
        }

        // Attribute selector mutations: [data-testid='loginBtn'] → variants
        Pattern attrPat = Pattern.compile("\\[([\\w-]+)[*^$~]?=['\"]([^'\"]+)['\"]\\]");
        Matcher am = attrPat.matcher(value);
        if (am.find()) {
            String attr = am.group(1);
            String attrVal = am.group(2);
            List<String> variants = generateValueMutations(attrVal);
            for (String v : variants) {
                if (!v.isBlank()) out.add(By.cssSelector("[" + attr + "='" + v + "']"));
            }
            // Contains fallback
            out.add(By.cssSelector("[" + attr + "*='" + attrVal + "']"));
            // Attribute swap
            if (attr.equals("data-testid")) {
                out.add(By.cssSelector("[data-test='" + attrVal + "']"));
                out.add(By.cssSelector("[data-cy='" + attrVal + "']"));
                out.add(By.id(attrVal));
            } else if (attr.equals("id")) {
                out.add(By.cssSelector("[data-testid='" + attrVal + "']"));
                out.add(By.name(attrVal));
            }
        }
    }

    private static void addXpathMutations(List<By> out, String value) {
        // If simple id-based: //*[@id='foo'] → try By.id() shorthand first
        Pattern xpathId = Pattern.compile("@id=['\"]([^'\"]+)['\"]");
        Matcher xm = xpathId.matcher(value);
        if (xm.find()) {
            String idVal = xm.group(1);
            out.add(By.id(idVal));
            List<String> variants = generateValueMutations(idVal);
            for (String v : variants) {
                if (!v.isBlank()) out.add(By.id(v));
            }
            // Also try data-testid
            out.add(By.cssSelector("[data-testid='" + idVal + "']"));
        }

        // Contains → exact: //input[contains(@id,'foo')] → //input[@id='foo']
        Pattern containsPat = Pattern.compile("contains\\(@([\\w-]+),\\s*['\"]([^'\"]+)['\"]\\)");
        Matcher cm = containsPat.matcher(value);
        if (cm.find()) {
            String attr = cm.group(1);
            String attrVal = cm.group(2);
            out.add(By.xpath(value.replace(
                    "contains(@" + attr + ",'" + attrVal + "')",
                    "@" + attr + "='" + attrVal + "'")));
            out.add(By.cssSelector("[" + attr + "='" + attrVal + "']"));
            List<String> variants = generateValueMutations(attrVal);
            for (String v : variants) {
                if (!v.isBlank()) out.add(By.cssSelector("[" + attr + "='" + v + "']"));
            }
        }
    }

    private static void addClassMutations(List<By> out, String value) {
        // By.className("login-btn") → try variants
        List<String> variants = generateValueMutations(value);
        for (String v : variants) {
            if (!v.isBlank()) out.add(By.className(v));
        }
        out.add(By.cssSelector("[class*='" + value + "']"));
    }

    private static void addGenericMutations(List<By> out, String value) {
        out.add(By.id(value));
        out.add(By.name(value));
        out.add(By.cssSelector("[data-testid='" + value + "']"));
        List<String> variants = generateValueMutations(value);
        for (String v : variants) {
            if (!v.isBlank()) {
                out.add(By.id(v));
                out.add(By.name(v));
            }
        }
    }

    // ──────────────────────── String Utilities ────────────────────────

    /**
     * Splits an identifier into lowercase tokens.
     * "loginSubmitBtn" → ["login", "submit", "btn"]
     * "login-submit-btn" → ["login", "submit", "btn"]
     * "login_submit_btn" → ["login", "submit", "btn"]
     */
    static List<String> tokenize(String value) {
        if (value == null || value.isBlank()) return List.of();
        String[] parts = CAMEL_SPLIT.split(value);
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            String t = p.strip().toLowerCase();
            if (!t.isEmpty()) tokens.add(t);
        }
        return tokens;
    }

    private static String toCamelCase(List<String> tokens) {
        if (tokens.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(tokens.get(0));
        for (int i = 1; i < tokens.size(); i++) {
            String t = tokens.get(i);
            if (!t.isEmpty()) {
                sb.append(Character.toUpperCase(t.charAt(0)));
                sb.append(t.substring(1));
            }
        }
        return sb.toString();
    }

    private static String toPascalCase(List<String> tokens) {
        if (tokens.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String t : tokens) {
            if (!t.isEmpty()) {
                sb.append(Character.toUpperCase(t.charAt(0)));
                sb.append(t.substring(1));
            }
        }
        return sb.toString();
    }
}
