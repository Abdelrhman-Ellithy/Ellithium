package Ellithium.core.ai.codegen;

import Ellithium.core.ai.DriverProfile;
import Ellithium.core.ai.SemanticLocatorResolver;
import Ellithium.core.ai.dom.CandidateAttributeBatcher;
import Ellithium.core.ai.healing.AISelfHealer;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class UniqueLocatorGenerator {

    private UniqueLocatorGenerator() {}

    @FunctionalInterface
    public interface UniquenessProbe {
        int matchCount(By by);
    }

    private static final Pattern DYNAMIC = Pattern.compile(
            "\\d{4,}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}|_\\d+$|:id/|[0-9a-f]{16,}");

    private static final double W_TESTID = 1.00, W_ID = 0.90, W_NAME = 0.85, W_ARIA = 0.80,
            W_TEXT = 0.78, W_DATA = 0.70, W_CSS = 0.60, W_XPATH = 0.40;
    private static final double NON_UNIQUE_FACTOR = 0.35;
    private static final double DYNAMIC_FACTOR = 0.70;

    private static final Set<String> SKIP_GENERIC = Set.of(
            "id", "name", "class", "style", "data-testid", "data-test", "data-cy", "data-qa",
            "aria-label", "role", "data-ellithium-pick");

    public static List<LocatorCandidate> generate(WebDriver driver, WebElement target) {
        if (DriverProfile.detect(driver).isNativeMobile()) {
            return rank(buildMobileDrafts(target), by -> matchCount(driver, by, target));
        }
        Map<String, Object> attrs = readAttributes(driver, target);
        List<Draft> drafts = buildDrafts(attrs);
        drafts.addAll(buildGenericAttrDrafts(readAllAttributes(driver, target)));
        drafts.addAll(buildPathDrafts(driver, target));
        return rank(drafts, by -> matchCount(driver, by, target));
    }

    static List<LocatorCandidate> rankMobile(WebElement el, UniquenessProbe probe) {
        return rank(buildMobileDrafts(el), probe);
    }

    private static List<Draft> buildMobileDrafts(WebElement el) {
        List<Draft> out = new ArrayList<>();
        String resId = attr(el, "resource-id");
        if (isPresent(resId)) addMobile(out, AppiumBy.id(resId), "resource-id", W_TESTID, looksDynamic(resId));
        String acc = attr(el, "accessibility-id");
        if (!isPresent(acc)) acc = attr(el, "content-desc");
        if (isPresent(acc)) addMobile(out, AppiumBy.accessibilityId(acc), "accessibility-id", 0.92, looksDynamic(acc));
        String name = attr(el, "name");
        if (isPresent(name)) addMobile(out, AppiumBy.accessibilityId(name), "name", W_NAME, looksDynamic(name));
        String text = attr(el, "text");
        if (isPresent(text)) {
            addMobile(out, AppiumBy.androidUIAutomator("new UiSelector().text(\"" + text + "\")"),
                    "ui-text", W_CSS, false);
        }
        addMobileXpath(out, attr(el, "class"), text, acc, resId, name);
        return out;
    }

    private static void addMobileXpath(List<Draft> out, String cls, String text,
                                       String acc, String resId, String name) {
        String tag = isPresent(cls) ? cls : "*";
        if (isPresent(text)) {
            addMobile(out, By.xpath("//" + tag + "[@text=" + SemanticLocatorResolver.xpathLiteral(text) + "]"),
                    "xpath-text", W_XPATH + 0.05, false);
        }
        if (isPresent(acc)) {
            addMobile(out, By.xpath("//" + tag + "[@content-desc=" + SemanticLocatorResolver.xpathLiteral(acc) + "]"),
                    "xpath-content-desc", W_XPATH + 0.04, false);
        }
        if (isPresent(name)) {
            addMobile(out, By.xpath("//" + tag + "[@name=" + SemanticLocatorResolver.xpathLiteral(name) + "]"),
                    "xpath-name", W_XPATH + 0.03, false);
        }
        if (isPresent(resId)) {
            addMobile(out, By.xpath("//*[@resource-id=" + SemanticLocatorResolver.xpathLiteral(resId) + "]"),
                    "xpath-resource-id", W_XPATH, looksDynamic(resId));
        }
    }

    private static void addMobile(List<Draft> out, By by, String tier, double weight, boolean parameterizable) {
        String expr = AISelfHealer.byToJavaExpression(by);
        if (expr == null) return;
        out.add(new Draft(by, expr, tier, weight, parameterizable));
    }

    private static String attr(WebElement el, String name) {
        try {
            String v = el.getAttribute(name);
            return (v == null || v.isBlank()) ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    public static List<LocatorCandidate> rank(Map<String, Object> attrs, UniquenessProbe probe) {
        return rank(attrs, Map.of(), probe);
    }

    public static List<LocatorCandidate> rank(Map<String, Object> attrs, Map<String, String> allAttrs,
                                              UniquenessProbe probe) {
        List<Draft> drafts = buildDrafts(attrs);
        drafts.addAll(buildGenericAttrDrafts(allAttrs));
        return rank(drafts, probe);
    }

    private static List<LocatorCandidate> rank(List<Draft> drafts, UniquenessProbe probe) {
        List<LocatorCandidate> out = new ArrayList<>(drafts.size());
        for (Draft d : drafts) {
            int count = (probe == null) ? -1 : safeCount(probe, d.by);
            boolean unique = count == 1;
            out.add(new LocatorCandidate(d.by, d.expr, score(d, unique), d.tier, unique, d.parameterizable));
        }
        out.sort((a, b) -> Double.compare(b.score(), a.score()));
        return out;
    }

    private static int safeCount(UniquenessProbe probe, By by) {
        try { return probe.matchCount(by); } catch (Exception e) { return -1; }
    }

    private static double score(Draft d, boolean unique) {
        return scoreOf(d.weight, unique, d.parameterizable, d.expr.length());
    }

    private static double scoreOf(double weight, boolean unique, boolean parameterizable, int exprLen) {
        double w = parameterizable ? weight * DYNAMIC_FACTOR : weight;
        if (!unique) w *= NON_UNIQUE_FACTOR;
        double brevity = 1.0 / (1.0 + exprLen / 120.0);
        return w * (0.9 + 0.1 * brevity);
    }

    private static double tierWeight(String tier) {
        if (tier == null) return W_CSS;
        return switch (tier) {
            case "data-testid", "data-test", "data-cy", "data-qa" -> W_TESTID;
            case "id" -> W_ID;
            case "name" -> W_NAME;
            case "aria-label", "aria-labelledby" -> W_ARIA;
            case "href-css" -> 0.76;
            case "attr-css" -> 0.75;
            case "link-text" -> W_ARIA;
            case "role-text", "text", "text-class", "text-attr" -> W_TEXT;
            case "tag-attr-css" -> 0.72;
            case "text-contains" -> 0.71;
            case "combo-css", "tag-class-css" -> 0.68;
            case "ancestor-css" -> 0.65;
            case "xpath-indexed", "text-contains-indexed" -> 0.62;
            case "class-css" -> 0.58;
            case "partial-link-text", "class-name" -> 0.45;
            case "css-path" -> 0.10;
            case "tag-name" -> 0.10;
            default -> tier.startsWith("data-") ? W_DATA : (tier.startsWith("xpath") ? W_XPATH : W_CSS);
        };
    }

    public static LocatorCandidate fromCapture(String type, String sel, String value,
                                               String tier, boolean unique, boolean parameterizable) {
        By by;
        String expr;
        switch (type == null ? "css" : type) {
            case "id" -> { by = By.id(value); expr = "By.id(\"" + esc(value) + "\")"; }
            case "name" -> { by = By.name(value); expr = "By.name(\"" + esc(value) + "\")"; }
            case "linkText" -> { by = By.linkText(value); expr = "By.linkText(\"" + esc(value) + "\")"; }
            case "partialLinkText" -> { by = By.partialLinkText(value); expr = "By.partialLinkText(\"" + esc(value) + "\")"; }
            case "className" -> { by = By.className(value); expr = "By.className(\"" + esc(value) + "\")"; }
            case "tagName" -> { by = By.tagName(value); expr = "By.tagName(\"" + esc(value) + "\")"; }
            case "xpath" -> { by = By.xpath(sel); expr = "By.xpath(\"" + esc(sel) + "\")"; }
            default -> { by = By.cssSelector(sel); expr = "By.cssSelector(\"" + esc(sel) + "\")"; }
        }
        return new LocatorCandidate(by, expr, scoreOf(tierWeight(tier), unique, parameterizable, expr.length()),
                tier, unique, parameterizable);
    }

    private static List<Draft> buildDrafts(Map<String, Object> attrs) {
        List<Draft> out = new ArrayList<>();
        if (attrs == null || attrs.isEmpty()) return out;

        addAttrEquals(out, attrs, "data-testid", "data-testid", W_TESTID);
        addAttrEquals(out, attrs, "data-test", "data-test", W_TESTID);
        addAttrEquals(out, attrs, "data-cy", "data-cy", W_TESTID);
        addAttrEquals(out, attrs, "data-qa", "data-qa", W_TESTID);

        String id = str(attrs.get("id"));
        if (isPresent(id)) {
            out.add(new Draft(By.id(id), "By.id(\"" + esc(id) + "\")", "id", W_ID, looksDynamic(id)));
        }
        String name = str(attrs.get("name"));
        if (isPresent(name)) {
            out.add(new Draft(By.name(name), "By.name(\"" + esc(name) + "\")", "name", W_NAME, looksDynamic(name)));
        }
        addAttrEquals(out, attrs, "aria-label", "aria-label", W_ARIA);

        String role = str(attrs.get("role"));
        String text = str(attrs.get("text"));
        if (isPresent(text)) {
            String lit = SemanticLocatorResolver.xpathLiteral(text);
            if (isPresent(role)) {
                String xp = "//*[@role='" + role + "' and normalize-space(.)=" + lit + "]";
                out.add(new Draft(By.xpath(xp), "By.xpath(\"" + esc(xp) + "\")", "role-text", W_TEXT, false));
            }
            String tag = str(attrs.get("tag"));
            if (isClickableTag(tag)) {
                String xp = "//" + tag + "[normalize-space(.)=" + lit + "]";
                out.add(new Draft(By.xpath(xp), "By.xpath(\"" + esc(xp) + "\")", "text", W_TEXT, false));
            }
        }

        String tag = str(attrs.get("tag"));
        String cls = firstStableClass(str(attrs.get("class")));
        if (isPresent(tag) && isPresent(cls)) {
            String css = tag + "." + cls;
            out.add(new Draft(By.cssSelector(css), "By.cssSelector(\"" + esc(css) + "\")", "css-scoped", W_CSS, false));
        }
        return out;
    }

    private static void addAttrEquals(List<Draft> out, Map<String, Object> attrs,
                                      String attr, String tier, double weight) {
        String v = str(attrs.get(attr));
        if (!isPresent(v)) return;
        String css = "[" + attr + "='" + v.replace("'", "\\'") + "']";
        out.add(new Draft(By.cssSelector(css), "By.cssSelector(\"" + esc(css) + "\")", tier, weight, looksDynamic(v)));
    }

    private static List<Draft> buildPathDrafts(WebDriver driver, WebElement target) {
        List<Draft> out = new ArrayList<>();
        if (!(driver instanceof JavascriptExecutor js)) return out;
        try {
            Object res = js.executeScript(PATH_SCRIPT, target);
            if (res instanceof Map<?, ?> m) {
                String css = str(m.get("css"));
                if (isPresent(css)) {
                    out.add(new Draft(By.cssSelector(css), "By.cssSelector(\"" + esc(css) + "\")",
                            "css-path", W_CSS - 0.05, false));
                }
                String xp = str(m.get("xpath"));
                if (isPresent(xp)) {
                    out.add(new Draft(By.xpath(xp), "By.xpath(\"" + esc(xp) + "\")", "xpath-rel", W_XPATH, false));
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    private static final String PATH_SCRIPT =
            "var el=arguments[0];"
            + "function stable(a){return a==='id'||a==='name'||a.indexOf('data-')===0;}"
            + "function seg(n){var t=n.tagName.toLowerCase();"
            + " var p=n.parentElement; if(!p) return t;"
            + " var same=Array.prototype.filter.call(p.children,function(c){return c.tagName===n.tagName;});"
            + " if(same.length===1) return t;"
            + " return t+':nth-of-type('+(Array.prototype.indexOf.call(same,n)+1)+')';}"
            + "function ancestor(n){var c=n;while(c&&c!==document.body){"
            + " if(c.id&&document.querySelectorAll('#'+CSS.escape(c.id)).length===1) return c;"
            + " var dt=c.getAttribute&&c.getAttribute('data-testid');"
            + " if(dt&&document.querySelectorAll('[data-testid=\"'+dt+'\"]').length===1) return c;"
            + " c=c.parentElement;} return null;}"
            + "var root=ancestor(el), parts=[], cur=el, base='';"
            + "if(root&&root!==el){ if(root.id){base='#'+CSS.escape(root.id);}"
            + " else {base='[data-testid=\"'+root.getAttribute('data-testid')+'\"]';} }"
            + "while(cur&&cur!==document.body&&cur!==root){parts.unshift(seg(cur));cur=cur.parentElement;}"
            + "var css=(base?base+' ':'')+parts.join(' > ');"
            + "return {'css': css, 'xpath': null};";

    private static List<Draft> buildGenericAttrDrafts(Map<String, String> all) {
        List<Draft> out = new ArrayList<>();
        if (all == null) return out;
        for (Map.Entry<String, String> e : all.entrySet()) {
            String n = e.getKey();
            String v = e.getValue();
            if (n == null || v == null || v.isBlank() || SKIP_GENERIC.contains(n)) continue;
            boolean eligible = n.startsWith("data-") || n.equals("placeholder") || n.equals("title") || n.equals("type");
            if (!eligible) continue;
            String css = "[" + n + "='" + v.replace("'", "\\'") + "']";
            out.add(new Draft(By.cssSelector(css), "By.cssSelector(\"" + esc(css) + "\")",
                    n, W_DATA, looksDynamic(v)));
        }
        return out;
    }

    private static Map<String, String> readAllAttributes(WebDriver driver, WebElement target) {
        if (!(driver instanceof JavascriptExecutor js)) return Map.of();
        try {
            Object res = js.executeScript(
                    "var el=arguments[0], o={}, a=el.attributes;"
                    + "for(var i=0;i<a.length;i++){o[a[i].name]=a[i].value;} return o;", target);
            if (res instanceof Map<?, ?> m) {
                Map<String, String> out = new java.util.LinkedHashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        out.put(e.getKey().toString(), e.getValue().toString());
                    }
                }
                return out;
            }
        } catch (Exception ignored) {}
        return Map.of();
    }

    private static int matchCount(WebDriver driver, By by, WebElement target) {
        try {
            List<WebElement> found = driver.findElements(by);
            if (found.size() != 1) return found.size();
            try { return found.get(0).equals(target) ? 1 : 2; }
            catch (Exception e) { return 1; }
        } catch (Exception e) {
            return -1;
        }
    }

    private static Map<String, Object> readAttributes(WebDriver driver, WebElement target) {
        try {
            List<Map<String, Object>> batch = CandidateAttributeBatcher.fetch(driver, List.of(target));
            if (batch != null && !batch.isEmpty() && batch.get(0) != null) return batch.get(0);
        } catch (Exception ignored) {}
        return Map.of();
    }

    static boolean looksDynamic(String v) {
        if (v == null || v.isBlank()) return false;
        if (v.trim().matches("\\d+")) return true;
        return DYNAMIC.matcher(v).find();
    }

    private static boolean isClickableTag(String tag) {
        return "a".equals(tag) || "button".equals(tag) || "label".equals(tag) || "summary".equals(tag);
    }

    private static String firstStableClass(String cls) {
        if (cls == null || cls.isBlank()) return null;
        for (String c : cls.trim().split("\\s+")) {
            if (!c.isEmpty() && !looksDynamic(c)) return c;
        }
        return null;
    }

    private static String str(Object o) { return o != null ? o.toString() : null; }
    private static boolean isPresent(String s) { return s != null && !s.isBlank(); }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class Draft {
        final By by;
        final String expr;
        final String tier;
        final double weight;
        final boolean parameterizable;

        Draft(By by, String expr, String tier, double weight, boolean parameterizable) {
            this.by = by;
            this.expr = expr;
            this.tier = tier;
            this.weight = weight;
            this.parameterizable = parameterizable;
        }
    }
}
