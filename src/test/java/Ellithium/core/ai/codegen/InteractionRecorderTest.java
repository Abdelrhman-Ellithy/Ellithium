package Ellithium.core.ai.codegen;

import com.google.gson.Gson;
import com.google.gson.JsonObject;


import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

public class InteractionRecorderTest {

    @BeforeMethod
    @AfterMethod
    public void clean() {
        InteractionRecorder.resetForTest();
    }

    // ─── Script structure ────────────────────────────────────────────────────

    @Test
    public void captureScript_isIIFE_withPickArgument() {
        String s = InteractionRecorder.CAPTURE_SCRIPT;
        Assert.assertTrue(s.startsWith("(function(pick){"),
                "CAPTURE_SCRIPT must be an IIFE taking pick: " + s.substring(0, Math.min(40, s.length())));
        Assert.assertTrue(s.endsWith(")(arguments[0]);"),
                "CAPTURE_SCRIPT must close with )(arguments[0]); got: " + s.substring(Math.max(0, s.length() - 20)));
    }

    @Test
    public void captureScript_doesNotMaskPasswordValues() {
        Assert.assertFalse(InteractionRecorder.CAPTURE_SCRIPT.contains("__ELL_SECRET__"),
                "CAPTURE_SCRIPT must not mask password values — passwords are stored at face value");
        Assert.assertFalse(InteractionRecorder.CAPTURE_SCRIPT.contains("pw?'__ELL"),
                "CAPTURE_SCRIPT must not branch on password field type for masking");
    }

    @Test
    public void captureScript_containsRequiredFunctions() {
        String s = InteractionRecorder.CAPTURE_SCRIPT;
        Assert.assertTrue(s.contains("function emit("), "must define emit()");
        Assert.assertTrue(s.contains("function inp("), "must define inp() for input capture");
        Assert.assertTrue(s.contains("function attach("), "must define attach() for event registration");
        Assert.assertTrue(s.contains("function cands("), "must define cands() for candidate collection");
        Assert.assertTrue(s.contains("function walk("), "must define walk() for iframe traversal");
        Assert.assertTrue(s.contains("__ellRecInit"), "must guard against double-initialisation");
        Assert.assertTrue(s.contains("__ellLastVal"), "must maintain last-value dedup map");
    }

    @Test
    public void overlayScript_isIIFE_noArguments() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.startsWith("(function(){"),
                "OVERLAY_SCRIPT must be an IIFE: " + s.substring(0, Math.min(30, s.length())));
        Assert.assertTrue(s.endsWith("})();"),
                "OVERLAY_SCRIPT must close with })(); got: " + s.substring(Math.max(0, s.length() - 10)));
    }

    @Test
    public void overlayScript_containsRequiredDomIds() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("ell-steps"), "overlay must contain ell-steps div");
        Assert.assertTrue(s.contains("ell-code"), "overlay must contain ell-code pre");
        Assert.assertTrue(s.contains("ell-stop"), "overlay must contain stop button");
        Assert.assertTrue(s.contains("ell-picked"), "overlay must contain picked-locator div");
        Assert.assertTrue(s.contains("__ellOverlayDone"), "overlay must set done flag to prevent re-injection");
    }

    @Test
    public void renderScript_isIIFE_withJsonArgument() {
        String s = InteractionRecorder.RENDER_SCRIPT;
        Assert.assertTrue(s.startsWith("(function(json){"),
                "RENDER_SCRIPT must be an IIFE taking json: " + s.substring(0, Math.min(40, s.length())));
        Assert.assertTrue(s.endsWith(")(arguments[0]);"),
                "RENDER_SCRIPT must close with )(arguments[0]);");
    }

    @Test
    public void renderScript_genBadge_usesIfElse_notDeadTernaryFirst() {
        String s = InteractionRecorder.RENDER_SCRIPT;
        Assert.assertTrue(s.contains("if(st.generatorMethod) genBadge="),
                "genBadge must use if/else pattern");
        Assert.assertTrue(s.contains("else genBadge=st.data?"),
                "else branch must fall back to data value");
        // Verify the dead ternary line (the BUG-1 pattern) is gone
        Assert.assertFalse(s.contains("'(st.data?(\\' \\u2192"),
                "dead first genBadge ternary with complex \\\\' escaping must not be present");
    }

    @Test
    public void renderScript_usesTdgMethods_andGeneratorDropdown() {
        String s = InteractionRecorder.RENDER_SCRIPT;
        Assert.assertTrue(s.contains("TDG_METHODS"), "must define TDG_METHODS list for generator dropdown");
        Assert.assertTrue(s.contains("tdgOptions"), "must define tdgOptions() function");
        Assert.assertTrue(s.contains("ell-gen"), "generator select must use ell-gen CSS class");
        Assert.assertTrue(s.contains("autoGenerate"), "must emit autoGenerate event on dropdown change");
    }

    @Test
    public void renderScript_htmlEntitiesForEmoji_notSurrogatePair() {
        String s = InteractionRecorder.RENDER_SCRIPT;
        Assert.assertTrue(s.contains("&#x1F3B2;"),
                "emoji must use HTML entity &#x1F3B2; (safe for innerHTML), not surrogate-pair \\uD83C");
        Assert.assertFalse(s.contains("\\uD83C"),
                "surrogate-pair \\uD83C must not appear — use &#x1F3B2; instead");
    }

    // ─── processEvent — pure logic ───────────────────────────────────────────

    private static Map<String, Object> clickEvent(String id) {
        return Map.of(
                "type", "click",
                "id", id,
                "tag", "button",
                "name", "Submit",
                "value", (Object) "null",
                "frame", List.of(),
                "candidates", List.of());
    }

    private static Map<String, Object> inputEvent(String id, String value) {
        return Map.of(
                "type", "input",
                "id", id,
                "tag", "input",
                "name", "Username",
                "value", value,
                "frame", List.of(),
                "candidates", List.of());
    }

    @Test
    public void processEvent_click_addsStep_returnsTrue() {
        boolean result = InteractionRecorder.processEvent(clickEvent("c1"));
        Assert.assertTrue(result);
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 1);
        RecordedStep s = InteractionRecorder.STEPS.get(0);
        Assert.assertEquals(s.getId(), "c1");
        Assert.assertEquals(s.getActionType(), "click");
    }

    @Test
    public void processEvent_input_addsStepWithValue() {
        boolean result = InteractionRecorder.processEvent(inputEvent("i1", "tomsmith"));
        Assert.assertTrue(result);
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 1);
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getData(), "tomsmith");
    }

    @Test
    public void processEvent_nullType_returnsFalseDoesNotAddStep() {
        boolean result = InteractionRecorder.processEvent(Map.of("id", "x"));
        Assert.assertFalse(result);
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 0);
    }

    @Test
    public void processEvent_missingId_returnsFalse() {
        boolean result = InteractionRecorder.processEvent(Map.of("type", "click"));
        Assert.assertFalse(result);
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 0);
    }

    @Test
    public void processEvent_clearAll_emptiesSteps() {
        InteractionRecorder.processEvent(clickEvent("c1"));
        InteractionRecorder.processEvent(clickEvent("c2"));
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 2);

        boolean result = InteractionRecorder.processEvent(Map.of("type", "clearAll"));
        Assert.assertTrue(result);
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 0);
        Assert.assertTrue(InteractionRecorder.BY_ID.isEmpty());
    }

    @Test
    public void processEvent_override_changesChosenIndex() {
        LocatorCandidate c0 = new LocatorCandidate(By.id("a"), "By.id(\"a\")", 0.9, "id", true, false);
        LocatorCandidate c1 = new LocatorCandidate(By.id("b"), "By.id(\"b\")", 0.8, "id", true, false);
        RecordedStep step = new RecordedStep("s1", "click", null, "button", "Btn", List.of(c0, c1));
        InteractionRecorder.STEPS.add(step);
        InteractionRecorder.BY_ID.put("s1", step);

        boolean result = InteractionRecorder.processEvent(
                Map.of("type", "override", "id", "s1", "index", 1.0));
        Assert.assertTrue(result);
        Assert.assertEquals(step.getChosenIndex(), 1);
    }

    @Test
    public void processEvent_override_unknownId_returnsFalse() {
        boolean result = InteractionRecorder.processEvent(
                Map.of("type", "override", "id", "nonexistent", "index", 0.0));
        Assert.assertFalse(result);
    }

    @Test
    public void processEvent_delete_removesStep() {
        InteractionRecorder.processEvent(clickEvent("c1"));
        InteractionRecorder.processEvent(clickEvent("c2"));
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 2);

        boolean result = InteractionRecorder.processEvent(Map.of("type", "delete", "id", "c1"));
        Assert.assertTrue(result);
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 1);
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getId(), "c2");
        Assert.assertFalse(InteractionRecorder.BY_ID.containsKey("c1"));
    }

    @Test
    public void processEvent_delete_unknownId_returnsFalse() {
        boolean result = InteractionRecorder.processEvent(Map.of("type", "delete", "id", "ghost"));
        Assert.assertFalse(result);
    }

    @Test
    public void processEvent_assertModeToggle_flipsMode() {
        Assert.assertTrue(InteractionRecorder.options.isSoftAssert());
        boolean result = InteractionRecorder.processEvent(Map.of("type", "assertModeToggle"));
        Assert.assertTrue(result);
        Assert.assertFalse(InteractionRecorder.options.isSoftAssert());

        InteractionRecorder.processEvent(Map.of("type", "assertModeToggle"));
        Assert.assertTrue(InteractionRecorder.options.isSoftAssert());
    }

    @Test
    public void processEvent_autoGenerate_setsGeneratorMethod() {
        InteractionRecorder.processEvent(inputEvent("i1", "test@example.com"));
        RecordedStep step = InteractionRecorder.BY_ID.get("i1");
        Assert.assertNull(step.getGeneratorMethod());

        boolean result = InteractionRecorder.processEvent(
                Map.of("type", "autoGenerate", "id", "i1", "method", "getRandomEmail"));
        Assert.assertTrue(result);
        Assert.assertEquals(step.getGeneratorMethod(), "getRandomEmail");
    }

    @Test
    public void processEvent_autoGenerate_nullMethod_clearsGenerator() {
        InteractionRecorder.processEvent(inputEvent("i1", "x"));
        RecordedStep step = InteractionRecorder.BY_ID.get("i1");
        step.setGeneratorMethod("getRandomEmail");

        InteractionRecorder.processEvent(
                Map.of("type", "autoGenerate", "id", "i1", "method", "null"));
        Assert.assertNull(step.getGeneratorMethod());
    }

    @Test
    public void processEvent_autoGenerate_unknownId_returnsFalse() {
        boolean result = InteractionRecorder.processEvent(
                Map.of("type", "autoGenerate", "id", "ghost", "method", "getRandomEmail"));
        Assert.assertFalse(result);
    }

    @Test
    public void processEvent_navigate_addsNavigateStep() {
        InteractionRecorder.processEvent(Map.of(
                "type", "navigate",
                "id", "nav1",
                "tag", (Object) "",
                "name", (Object) "",
                "value", "https://example.com",
                "frame", List.of(),
                "candidates", List.of()));
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 1);
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "navigate");
    }

    // ─── renderJson structure ────────────────────────────────────────────────

    @Test
    public void renderJson_emptySteps_producesValidJsonWithStepsArray() {
        String json = InteractionRecorder.renderJson();
        Assert.assertNotNull(json);
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        Assert.assertTrue(root.has("steps"), "must contain 'steps' key");
        Assert.assertTrue(root.get("steps").isJsonArray(), "steps must be a JSON array");
        Assert.assertEquals(root.get("steps").getAsJsonArray().size(), 0);
        Assert.assertTrue(root.has("assertMode"), "must contain assertMode");
        Assert.assertTrue(root.has("code"), "must contain code preview");
    }

    @Test
    public void renderJson_withOneStep_includesStepFields() {
        InteractionRecorder.processEvent(clickEvent("c1"));
        String json = InteractionRecorder.renderJson();
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        var steps = root.getAsJsonArray("steps");
        Assert.assertEquals(steps.size(), 1);
        JsonObject step = steps.get(0).getAsJsonObject();
        Assert.assertEquals(step.get("id").getAsString(), "c1");
        Assert.assertEquals(step.get("action").getAsString(), "click");
        Assert.assertTrue(step.has("candidates"), "step must have candidates array");
        Assert.assertTrue(step.has("generatorMethod"), "step must have generatorMethod field");
    }

    @Test
    public void renderJson_debounce_skipsExpensivePreviewWithin1s() {
        InteractionRecorder.lastCodeRenderMs = System.currentTimeMillis();
        InteractionRecorder.cachedCodePreview = "CACHED_CODE";

        String json = InteractionRecorder.renderJson();
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        Assert.assertEquals(root.get("code").getAsString(), "CACHED_CODE",
                "within debounce window, cached preview must be returned without re-running build()");
    }

    @Test
    public void renderJson_debounce_refreshesAfterWindow() throws InterruptedException {
        InteractionRecorder.lastCodeRenderMs = System.currentTimeMillis() - 2_000L;
        InteractionRecorder.cachedCodePreview = "STALE_CODE";

        String json = InteractionRecorder.renderJson();
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        String code = root.get("code").getAsString();
        Assert.assertNotEquals(code, "STALE_CODE",
                "after debounce window, preview must be regenerated");
    }

    @Test
    public void resetForTest_clearsAllState() {
        InteractionRecorder.processEvent(clickEvent("c1"));
        InteractionRecorder.options = InteractionRecorder.options.withAssertMode("hard");
        InteractionRecorder.startUrl = "https://example.com";
        InteractionRecorder.cachedCodePreview = "something";

        InteractionRecorder.resetForTest();

        Assert.assertEquals(InteractionRecorder.STEPS.size(), 0);
        Assert.assertTrue(InteractionRecorder.BY_ID.isEmpty());
        Assert.assertTrue(InteractionRecorder.options.isSoftAssert(), "options must be reset to defaults");
        Assert.assertNull(InteractionRecorder.startUrl);
        Assert.assertEquals(InteractionRecorder.cachedCodePreview, "");
        Assert.assertEquals(InteractionRecorder.lastCodeRenderMs, 0L);
    }
}
