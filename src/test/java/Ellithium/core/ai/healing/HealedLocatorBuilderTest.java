package Ellithium.core.ai.healing;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import Ellithium.core.ai.healing.HealedLocatorBuilder;
import Ellithium.core.ai.healing.HealedLocatorBuilder.Candidate;

public class HealedLocatorBuilderTest {

    private static Map<String, Object> attrs(String... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private static Candidate cand(By by, String kind, String sel, String tier, double weight) {
        return new Candidate(by, kind, sel, tier, weight);
    }

    // ──────────────────────── rankAndPick ────────────────────────

    @Test
    public void rankAndPick_dropsInvalidKeepsValid() {
        Candidate fragile = cand(By.xpath("//div[3]"), "xpath", "//div[3]", "css-path", 0.10);
        Candidate strong  = cand(By.id("saveBtn"), "css", "#saveBtn", "tag-class", 0.62);
        By best = HealedLocatorBuilder.rankAndPick(List.of(fragile, strong), by ->
                by.equals(By.id("saveBtn")) ? 1 : 0);   // fragile invalid, strong unique
        Assert.assertEquals(best, By.id("saveBtn"));
    }

    @Test
    public void rankAndPick_prefersUniqueOverAmbiguousSameWeight() {
        Candidate unique    = cand(By.cssSelector("a.x"), "css", "a.x", "tag-class", 0.62);
        Candidate ambiguous = cand(By.cssSelector("a.y"), "css", "a.y", "tag-class", 0.62);
        By best = HealedLocatorBuilder.rankAndPick(List.of(ambiguous, unique), by ->
                by.equals(By.cssSelector("a.x")) ? 1 : 5);
        Assert.assertEquals(best, By.cssSelector("a.x"), "unique (1) must beat ambiguous (5) at equal weight");
    }

    @Test
    public void rankAndPick_ambiguousStillReturnedWhenOnlyValidOption() {
        Candidate ambiguous = cand(By.cssSelector("li.row"), "css", "li.row", "class", 0.58);
        By best = HealedLocatorBuilder.rankAndPick(List.of(ambiguous), by -> 4);  // valid but ambiguous
        Assert.assertEquals(best, By.cssSelector("li.row"),
                "uniqueness is a factor, not a hard gate — an ambiguous-but-valid candidate is still returned");
    }

    @Test
    public void rankAndPick_higherTierWinsWhenBothUnique() {
        Candidate aria = cand(By.cssSelector("[aria-label='Save']"), "css", "[aria-label='Save']", "aria-label", 0.80);
        Candidate cls  = cand(By.cssSelector("button.b"), "css", "button.b", "tag-class", 0.62);
        By best = HealedLocatorBuilder.rankAndPick(List.of(cls, aria), by -> 1);
        Assert.assertEquals(best, By.cssSelector("[aria-label='Save']"), "higher-weight tier wins when both unique");
    }

    @Test
    public void rankAndPick_allInvalid_returnsNull() {
        Candidate a = cand(By.cssSelector("a.x"), "css", "a.x", "class", 0.58);
        Assert.assertNull(HealedLocatorBuilder.rankAndPick(List.of(a), by -> 0));
        Assert.assertNull(HealedLocatorBuilder.rankAndPick(List.of(), by -> 1));
    }

    @Test
    public void rankAndPick_probeCalledExactlyOncePerCandidate() {
        AtomicInteger calls = new AtomicInteger();
        List<Candidate> cands = List.of(
                cand(By.id("a"), "css", "#a", "tag-class", 0.62),
                cand(By.id("b"), "css", "#b", "class", 0.58),
                cand(By.id("c"), "xpath", "//c", "text", 0.78));
        HealedLocatorBuilder.rankAndPick(cands, by -> { calls.incrementAndGet(); return 1; });
        Assert.assertEquals(calls.get(), 3, "NFR: each candidate probed exactly once (single collected batch)");
    }

    // ──────────────────────── fastPathLocator (0 validation) ────────────────────────

    @Test
    public void fastPath_dataTestid_returnsImmediately() {
        By by = HealedLocatorBuilder.fastPathLocator(attrs("data-testid", "save-btn", "id", "x123"));
        Assert.assertEquals(by, By.cssSelector("[data-testid='save-btn']"));
    }

    @Test
    public void fastPath_stableId_returnsById() {
        By by = HealedLocatorBuilder.fastPathLocator(attrs("id", "loginBtn"));
        Assert.assertEquals(by, By.id("loginBtn"));
    }

    @Test
    public void fastPath_dynamicIdSkipped_returnsNullWhenNoStableIdentity() {
        // id is dynamic (trailing digits run) and no testid/name → no fast path; rich path will run.
        Assert.assertNull(HealedLocatorBuilder.fastPathLocator(attrs("id", "input_12345", "class", "form-control")));
    }

    @Test
    public void fastPath_nameUsedWhenNoTestidOrId() {
        By by = HealedLocatorBuilder.fastPathLocator(attrs("name", "email"));
        Assert.assertEquals(by, By.name("email"));
    }

    // ──────────────────────── buildWebCandidates (rich path) ────────────────────────

    @Test
    public void buildWebCandidates_emitsTextAndAttrXpath() {
        List<Candidate> cs = HealedLocatorBuilder.buildWebCandidates(
                attrs("tag", "button", "text", "Submit", "type", "submit"));
        boolean hasAndXpath = cs.stream().anyMatch(c ->
                c.sel.contains("normalize-space(.)") && c.sel.contains("and @type='submit'"));
        Assert.assertTrue(hasAndXpath, "must emit a text + attribute 'and'-xpath: " + sels(cs));
    }

    @Test
    public void buildWebCandidates_emitsProgressiveMultiClassCss() {
        List<Candidate> cs = HealedLocatorBuilder.buildWebCandidates(
                attrs("tag", "div", "class", "icon-buttons has-border-right"));
        boolean hasTwoClass = cs.stream().anyMatch(c -> c.sel.equals("div.icon-buttons.has-border-right"));
        boolean hasOneClass = cs.stream().anyMatch(c -> c.sel.equals("div.icon-buttons"));
        Assert.assertTrue(hasTwoClass, "must emit a 2-class CSS: " + sels(cs));
        Assert.assertTrue(hasOneClass, "must emit a tag.class CSS: " + sels(cs));
    }

    @Test
    public void buildWebCandidates_skipsDynamicAndCssUnsafeClasses() {
        List<Candidate> cs = HealedLocatorBuilder.buildWebCandidates(
                attrs("tag", "div", "class", "md:flex w-1/2 css-1a2b3c4d5e6f7890"));
        boolean anyUnsafe = cs.stream().anyMatch(c ->
                c.sel.contains("md:flex") || c.sel.contains("w-1/2") || c.sel.contains("css-1a2b3c4d5e6f7890"));
        Assert.assertFalse(anyUnsafe, "CSS-unsafe / dynamic class tokens must be skipped: " + sels(cs));
    }

    @Test
    public void buildWebCandidates_cappedAtTen() {
        Map<String, Object> a = attrs("tag", "button", "text", "Go", "aria-label", "Go action",
                "type", "submit", "placeholder", "ph", "title", "tt", "name", "n",
                "data-testid", "dt", "role", "button", "class", "a b");
        List<Candidate> cs = HealedLocatorBuilder.buildWebCandidates(a);
        Assert.assertTrue(cs.size() <= 10, "candidate set must be capped at 10, was " + cs.size());
    }

    private static String sels(List<Candidate> cs) {
        StringBuilder sb = new StringBuilder();
        for (Candidate c : cs) sb.append(c.sel).append(" | ");
        return sb.toString();
    }
}
