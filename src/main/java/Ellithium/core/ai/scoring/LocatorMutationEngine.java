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
        return tryMutations(brokenLocator, driver, baseline, null);
    }

    /**
     * As {@link #tryMutations(By, WebDriver, ElementFingerprint)}, additionally collecting the
     * near-miss candidates this pass had to abstain on into {@code hints} (token-vote ties and
     * cross-validation failures) so a later tier can differentiate them instead of rescanning
     * the page from scratch. {@code hints} may be null.
     */
    public static WebElement tryMutations(By brokenLocator, WebDriver driver,
                                          ElementFingerprint baseline,
                                          Ellithium.core.ai.models.HealingHints hints) {
        List<By> mutations = generateMutations(brokenLocator);
        if (mutations.isEmpty()) return null;

        List<List<By>> deferredGroups = (baseline == null) ? coldStartTokenGroups(brokenLocator) : List.of();
        java.util.Set<By> deferredSet = new java.util.HashSet<>();
        for (List<By> group : deferredGroups) deferredSet.addAll(group);

        Ellithium.core.execution.listener.seleniumListener.suppressLogging();
        try {
            for (By mutation : mutations) {
                if (deferredSet.contains(mutation)) continue;
                try {
                    WebElement found = driver.findElement(mutation);
                    if (baseline != null && !mutationCrossValidates(driver, baseline, found)) {
                        if (hints != null) hints.add(found, mutation, 0.0, "mutation resolved, cross-validation failed", 1);
                        continue;
                    }
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
            if (!deferredGroups.isEmpty()) {
                WebElement agreed = resolveByTokenAgreement(driver, deferredGroups, hints);
                if (agreed != null) {
                    Ellithium.core.execution.listener.seleniumListener.resumeLogging();
                    By agreedLocator = safeReconstruct(agreed);
                    Reporter.log("[TIER 1] mutation (token-agreement): " + brokenLocator + " → "
                            + (agreedLocator != null ? agreedLocator : "(element)"), LogLevel.INFO_GREEN);
                    return agreed;
                }
            }
        } finally {
            Ellithium.core.execution.listener.seleniumListener.resumeLogging();
        }
        return null;
    }

    /**
     * The {@code [id*=…]} / {@code [data-id*=…]} contains-fallbacks for an {@code id} locator
     * (mirrors {@link #addIdMutations}): the full value as one group, plus one group per token for
     * multi-token values, so the voter counts each piece of evidence exactly once. ALL contains
     * mutations are ambiguous by construction (a substring can match several elements), so at
     * cold-start none of them may resolve by blind first-match — a full-value substring like
     * {@code email} matching two real inputs must abstain, not pick DOM order. Empty for other
     * locator types, which do not emit contains-fallback mutations on id/data-id.
     */
    private static List<List<By>> coldStartTokenGroups(By brokenLocator) {
        Matcher m = BY_PARSE.matcher(brokenLocator.toString());
        if (!m.find() || !"id".equals(m.group(1))) return List.of();
        String value = m.group(2).trim();
        if (value.isEmpty()) return List.of();
        List<List<By>> out = new ArrayList<>();
        String escValue = cssEscape(value);
        out.add(List.of(By.cssSelector("[id*='" + escValue + "']"),
                        By.cssSelector("[data-id*='" + escValue + "']")));
        List<String> tokens = tokenize(value);
        if (tokens.size() >= 2) {
            for (String t : tokens) {
                if (t.length() < 3) continue;
                String esc = cssEscape(t);
                out.add(List.of(By.cssSelector("[id*='" + esc + "']"),
                                By.cssSelector("[data-id*='" + esc + "']")));
            }
        }
        return out;
    }

    private static final int COMMON_TOKEN_MAX = 3;

    /** Two weighted votes within this epsilon are considered equal (a tie). */
    private static final double VOTE_EPSILON = 1e-6;

    /**
     * Resolves the per-token contains groups by inverse-frequency-weighted voting: each token whose
     * selectors match a small, discriminative element set (each selector ≤ {@link #COMMON_TOKEN_MAX}
     * matches) contributes {@code 1/matchCount} to every distinct element it matches — a token
     * unique to one element ("named" → open-named-btn) outweighs a token shared by two
     * ("window" → two other buttons), exactly like IDF. An element carrying both {@code id} and
     * {@code data-id} is not double-counted against an {@code id}-only sibling. The element with
     * the strict-maximum weight wins; a tie (within {@link #VOTE_EPSILON}) means the tokens carry
     * genuinely equal evidence, so this returns null and hands the tied finalists to the next tier
     * via {@code hints}. Overly-common tokens (more than {@link #COMMON_TOKEN_MAX} matches) carry
     * no signal.
     */
    private static WebElement resolveByTokenAgreement(WebDriver driver, List<List<By>> tokenGroups,
                                                      Ellithium.core.ai.models.HealingHints hints) {
        java.util.Map<WebElement, Double> votes = new java.util.HashMap<>();
        for (List<By> group : tokenGroups) {
            java.util.Set<WebElement> tokenMatches = new java.util.LinkedHashSet<>();
            for (By sel : group) {
                List<WebElement> found;
                try {
                    found = driver.findElements(sel);
                } catch (Exception ignored) {
                    continue;
                }
                if (found.isEmpty() || found.size() > COMMON_TOKEN_MAX) continue;
                tokenMatches.addAll(found);
            }
            if (tokenMatches.isEmpty()) continue;
            double weight = 1.0 / tokenMatches.size();
            for (WebElement el : tokenMatches) {
                votes.merge(el, weight, Double::sum);
            }
        }
        WebElement best = null;
        double bestVotes = 0.0;
        boolean tie = false;
        for (java.util.Map.Entry<WebElement, Double> e : votes.entrySet()) {
            if (e.getValue() > bestVotes + VOTE_EPSILON) {
                bestVotes = e.getValue();
                best = e.getKey();
                tie = false;
            } else if (Math.abs(e.getValue() - bestVotes) <= VOTE_EPSILON) {
                tie = true;
            }
        }
        if (tie && hints != null && bestVotes > 0.0) {
            for (java.util.Map.Entry<WebElement, Double> e : votes.entrySet()) {
                if (Math.abs(e.getValue() - bestVotes) <= VOTE_EPSILON) {
                    hints.add(e.getKey(), safeReconstruct(e.getKey()), e.getValue(),
                            String.format(java.util.Locale.ROOT,
                                    "token-vote tie (weight %.2f)", e.getValue()), 1);
                }
            }
        }
        return tie ? null : best;
    }

    private static By safeReconstruct(WebElement el) {
        try {
            return ElementFingerprint.reconstructLocator(el);
        } catch (Exception e) {
            return null;
        }
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

    /** Clears the mutation cache. Call between suites to avoid stale mutations from a different AUT. */
    public static void resetCache() {
        MUTATION_CACHE.clear();
    }

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
            case "linkText", "partialLinkText" -> addLinkTextMutations(mutations, value);
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
        out.add(By.cssSelector("[data-testid='" + cssEscape(value) + "']"));
        for (String v : variants) {
            if (!v.isBlank()) out.add(By.cssSelector("[data-testid='" + cssEscape(v) + "']"));
        }
        // id → aria-label
        out.add(By.cssSelector("[aria-label='" + cssEscape(value) + "']"));
        // id → class (id demoted to a class during a refactor)
        if (isCssIdentifier(value)) out.add(By.className(value));
        for (String v : variants) {
            if (isCssIdentifier(v)) out.add(By.className(v));
        }
        // Contains fallback — full value and each distinctive token, on id and data-id.
        out.add(By.cssSelector("[id*='" + cssEscape(value) + "']"));
        out.add(By.cssSelector("[data-id*='" + cssEscape(value) + "']"));
        List<String> tokens = tokenize(value);
        if (tokens.size() >= 2) {
            for (String t : tokens) {
                if (t.length() < 3) continue;
                String esc = cssEscape(t);
                out.add(By.cssSelector("[id*='" + esc + "']"));
                out.add(By.cssSelector("[data-id*='" + esc + "']"));
            }
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
        out.add(By.cssSelector("[data-testid='" + cssEscape(value) + "']"));
        // Contains fallback
        out.add(By.cssSelector("[name*='" + cssEscape(value) + "']"));
    }

    private static void addCssMutations(List<By> out, String value) {
        // Class name mutations: ".login-btn" → ".loginBtn", ".login_btn"
        if (value.startsWith(".")) {
            String cls = value.substring(1);
            List<String> variants = generateValueMutations(cls);
            for (String v : variants) {
                if (!v.isBlank()) out.add(By.cssSelector("." + v));
            }
            out.add(By.cssSelector("[class*='" + cssEscape(cls) + "']"));
            List<String> tokens = tokenize(cls);
            if (!tokens.isEmpty()) {
                out.add(By.cssSelector("[class*='" + cssEscape(tokens.get(0)) + "']"));
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
                if (!v.isBlank()) out.add(By.cssSelector("[" + attr + "='" + cssEscape(v) + "']"));
            }
            // Contains fallback
            out.add(By.cssSelector("[" + attr + "*='" + cssEscape(attrVal) + "']"));
            // Attribute swap
            if (attr.equals("data-testid")) {
                out.add(By.cssSelector("[data-test='" + cssEscape(attrVal) + "']"));
                out.add(By.cssSelector("[data-cy='" + cssEscape(attrVal) + "']"));
                out.add(By.id(attrVal));
            } else if (attr.equals("id")) {
                out.add(By.cssSelector("[data-testid='" + cssEscape(attrVal) + "']"));
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
            out.add(By.cssSelector("[data-testid='" + cssEscape(idVal) + "']"));
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
            out.add(By.cssSelector("[" + attr + "='" + cssEscape(attrVal) + "']"));
            List<String> variants = generateValueMutations(attrVal);
            for (String v : variants) {
                if (!v.isBlank()) out.add(By.cssSelector("[" + attr + "='" + cssEscape(v) + "']"));
            }
        }
    }

    private static void addClassMutations(List<By> out, String value) {
        // By.className("login-btn") → try variants
        List<String> variants = generateValueMutations(value);
        for (String v : variants) {
            if (isCssIdentifier(v)) out.add(By.className(v));
        }
        out.add(By.cssSelector("[class*='" + cssEscape(value) + "']"));
    }

    /**
     * {@code linkText} requires an exact match and does not tolerate incidental leading/trailing
     * whitespace (a common copy-paste artifact from a rendered page); {@code partialLinkText} is
     * the natural fallback for either flavor when the exact text has drifted slightly.
     */
    private static void addLinkTextMutations(List<By> out, String value) {
        // value is already trimmed by generateMutations before dispatch — the whitespace-drift case
        // (By.linkText(" Sign In ") vs rendered "Sign In") is fixed for free by that shared trim; what
        // was missing is trying linkText/partialLinkText mutations AT ALL instead of falling to the
        // generic id/name-style guesses, which never apply to a link-text locator. Both forms are
        // tried regardless of which strategy the broken locator used.
        if (value.isBlank()) return;
        out.add(By.linkText(value));
        out.add(By.partialLinkText(value));
    }

    private static void addGenericMutations(List<By> out, String value) {
        out.add(By.id(value));
        out.add(By.name(value));
        out.add(By.cssSelector("[data-testid='" + cssEscape(value) + "']"));
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
     * Escapes a value for interpolation inside a single-quoted CSS attribute selector
     * ({@code [attr='value']}), so a value containing {@code '} or {@code \} produces a valid
     * selector instead of an {@link org.openqa.selenium.InvalidSelectorException} that silently
     * drops the mutation.
     */
    static String cssEscape(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    /**
     * Whether {@code value} is usable as a bare CSS class identifier for {@link By#className} —
     * non-blank, does not start with a digit, and contains only letters, digits, {@code -} or
     * {@code _} (a dot, space, or quote would form a compound/invalid selector).
     */
    static boolean isCssIdentifier(String value) {
        if (value == null || value.isEmpty() || Character.isDigit(value.charAt(0))) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_')) return false;
        }
        return true;
    }

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
