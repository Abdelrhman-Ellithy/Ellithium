package Ellithium.core.ai.codegen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import Ellithium.core.ai.codegen.InteractionRecorder;
import Ellithium.core.ai.codegen.LocatorCandidate;
import Ellithium.core.ai.codegen.RecordedStep;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
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
        // OVERLAY_SCRIPT is called via js.executeScript(OVERLAY_SCRIPT) — it uses "return" so the caller
        // can detect first injection; the IIFE itself carries no arguments.
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.startsWith("return (function(){"),
                "OVERLAY_SCRIPT must be a 'return (function(){...})();' expression for boolean result: "
                + s.substring(0, Math.min(40, s.length())));
        Assert.assertTrue(s.endsWith("})();"),
                "OVERLAY_SCRIPT must close with })(); got: " + s.substring(Math.max(0, s.length() - 10)));
    }

    @Test
    public void overlayScript_returnsBoolean_truOnFirstInjection() {
        // must set window.__ellOverlayDone=true and return true on first injection
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("window.__ellOverlayDone=true; return true;"),
                "overlay must set done flag and return true on first injection");
    }

    @Test
    public void captureScript_attrDialog_excludedFromRecording() {
        // The attribute dialog (#ell-attr-dlg) must be excluded by __ellInBar so that
        // choosing an attribute from the <select> inside the dialog does not record a 'select' step.
        String s = InteractionRecorder.CAPTURE_SCRIPT;
        Assert.assertTrue(s.contains("ell-attr-dlg"),
                "CAPTURE_SCRIPT __ellInBar must exclude #ell-attr-dlg: " + s.substring(
                        Math.max(0, s.indexOf("__ellInBar") - 0), Math.min(s.length(), s.indexOf("__ellInBar") + 200)));
    }

    @Test
    public void overlayScript_attrDialog_excludedFromRecording() {
        // window.__ellInBar in OVERLAY_SCRIPT must also exclude #ell-attr-dlg so the full
        // page-level guard is consistent with the CAPTURE_SCRIPT guard.
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("ell-attr-dlg"),
                "OVERLAY_SCRIPT window.__ellInBar must exclude #ell-attr-dlg");
    }

    @Test
    public void overlayScript_showAttrDialog_pusheAssertValueType() {
        // The Add button inside the attribute dialog must push type:'assertValue' into the log.
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("type:'assertValue'"),
                "attr dialog Add button must push assertValue event, not select");
        Assert.assertTrue(s.contains("assertAttr"),
                "attr dialog event must carry assertAttr field");
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
    public void processEvent_assertValue_setsAssertAttr() {
        // assertValue events from the attribute dialog carry assertAttr; it must be stored on the step.
        boolean result = InteractionRecorder.processEvent(java.util.Map.of(
                "type", "assertValue",
                "id", "av1",
                "tag", "input",
                "name", "Email",
                "value", "user@test.com",
                "assertAttr", "value",
                "frame", java.util.List.of(),
                "candidates", java.util.List.of()));
        Assert.assertTrue(result);
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 1);
        RecordedStep s = InteractionRecorder.STEPS.get(0);
        Assert.assertEquals(s.getActionType(), "assertValue");
        Assert.assertEquals(s.getAssertAttr(), "value");
        Assert.assertEquals(s.getData(), "user@test.com");
    }

    @Test
    public void processEvent_assertValue_withoutAssertAttr_stepStillAdded_butAttrIsNull() {
        // assertValue events without assertAttr are still added (PomCodeEmitter skips them at emit time).
        boolean result = InteractionRecorder.processEvent(java.util.Map.of(
                "type", "assertValue",
                "id", "av2",
                "tag", "div",
                "name", "X",
                "value", "something",
                "frame", java.util.List.of(),
                "candidates", java.util.List.of()));
        Assert.assertTrue(result);
        RecordedStep s = InteractionRecorder.STEPS.get(0);
        Assert.assertNull(s.getAssertAttr(), "missing assertAttr must remain null");
    }

    @Test
    public void processEvent_assertValue_textAttr_preserved() {
        // text() is a valid pseudo-attribute for assertValue
        InteractionRecorder.processEvent(java.util.Map.of(
                "type", "assertValue",
                "id", "av3",
                "tag", "h1",
                "name", "Title",
                "value", "Welcome",
                "assertAttr", "text()",
                "frame", java.util.List.of(),
                "candidates", java.util.List.of()));
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getAssertAttr(), "text()");
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

    @Test
    public void processEvent_inspect_setsLastPickedDoesNotAddStep() {
        List<Map<String, Object>> cands = List.of(Map.of(
                "type", "id", "sel", "#foo", "value", "foo", "tier", "id", "unique", true, "param", false));
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "inspect");
        ev.put("id", "ins1");
        ev.put("candidates", cands);
        boolean result = InteractionRecorder.processEvent(ev);
        Assert.assertTrue(result);
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 0, "inspect must not add a step");
        Assert.assertFalse(InteractionRecorder.lastPicked.isEmpty(), "inspect must populate lastPicked");
    }

    @Test
    public void processEvent_hover_addsHoverStep() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "hover");
        ev.put("id", "hov1");
        ev.put("tag", "a");
        ev.put("name", "Link");
        ev.put("value", (Object) null);
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        boolean result = InteractionRecorder.processEvent(ev);
        Assert.assertTrue(result);
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 1);
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "hover");
    }

    @Test
    public void processEvent_assertVisible_addsStep() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "assertVisible");
        ev.put("id", "av1");
        ev.put("tag", "div");
        ev.put("name", "");
        ev.put("value", (Object) null);
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        Assert.assertTrue(InteractionRecorder.processEvent(ev));
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "assertVisible");
    }

    @Test
    public void processEvent_assertEnabled_addsStep() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "assertEnabled");
        ev.put("id", "ae1");
        ev.put("tag", "button");
        ev.put("name", "");
        ev.put("value", (Object) null);
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        Assert.assertTrue(InteractionRecorder.processEvent(ev));
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "assertEnabled");
    }

    @Test
    public void processEvent_assertSelected_addsStep() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "assertSelected");
        ev.put("id", "as1");
        ev.put("tag", "input");
        ev.put("name", "");
        ev.put("value", (Object) null);
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        Assert.assertTrue(InteractionRecorder.processEvent(ev));
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "assertSelected");
    }

    @Test
    public void processEvent_assertText_addsStep() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "assertText");
        ev.put("id", "at1");
        ev.put("tag", "h1");
        ev.put("name", "");
        ev.put("value", "Welcome");
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        Assert.assertTrue(InteractionRecorder.processEvent(ev));
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "assertText");
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getData(), "Welcome");
    }

    @Test
    public void processEvent_rightClick_addsStep() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "rightClick");
        ev.put("id", "rc1");
        ev.put("tag", "div");
        ev.put("name", "");
        ev.put("value", (Object) null);
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        Assert.assertTrue(InteractionRecorder.processEvent(ev));
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "rightClick");
    }

    @Test
    public void processEvent_pressEnter_addsStep() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "pressEnter");
        ev.put("id", "pe1");
        ev.put("tag", "input");
        ev.put("name", "");
        ev.put("value", (Object) null);
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        Assert.assertTrue(InteractionRecorder.processEvent(ev));
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "pressEnter");
    }

    @Test
    public void processEvent_uploadFile_addsStep() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "uploadFile");
        ev.put("id", "uf1");
        ev.put("tag", "input");
        ev.put("name", "");
        ev.put("value", "photo.jpg");
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        Assert.assertTrue(InteractionRecorder.processEvent(ev));
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "uploadFile");
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getData(), "photo.jpg");
    }

    @Test
    public void processEvent_deselectByText_addsStep() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "deselectByText");
        ev.put("id", "dbt1");
        ev.put("tag", "select");
        ev.put("name", "");
        ev.put("value", "Option A");
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        Assert.assertTrue(InteractionRecorder.processEvent(ev));
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "deselectByText");
    }

    @Test
    public void processEvent_navigateRefresh_addsStep() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "navigateRefresh");
        ev.put("id", "nr1");
        ev.put("tag", "");
        ev.put("name", "");
        ev.put("value", (Object) null);
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        Assert.assertTrue(InteractionRecorder.processEvent(ev));
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "navigateRefresh");
    }

    @Test
    public void processEvent_dragAndDrop_addsStep() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "dragAndDrop");
        ev.put("id", "dd1");
        ev.put("tag", "div");
        ev.put("name", "");
        ev.put("value", (Object) null);
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        ev.put("targetCandidates", List.of());
        Assert.assertTrue(InteractionRecorder.processEvent(ev));
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "dragAndDrop");
    }

    @Test
    public void processEvent_select_addsStepWithValue() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "select");
        ev.put("id", "sel1");
        ev.put("tag", "select");
        ev.put("name", "");
        ev.put("value", "Option B");
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        Assert.assertTrue(InteractionRecorder.processEvent(ev));
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "select");
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getData(), "Option B");
    }

    @Test
    public void processEvent_duplicateId_secondEventIgnored() {
        Assert.assertTrue(InteractionRecorder.processEvent(clickEvent("dup1")));
        Assert.assertFalse(InteractionRecorder.processEvent(clickEvent("dup1")));
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 1, "duplicate id must be deduplicated");
    }

    @Test
    public void processEvent_recordEvent_clearsLastPicked() {
        List<Map<String, Object>> cands = List.of(Map.of(
                "type", "id", "sel", "#x", "value", "x", "tier", "id", "unique", true, "param", false));
        Map<String, Object> inspectEv = new HashMap<>();
        inspectEv.put("type", "inspect");
        inspectEv.put("id", "ins1");
        inspectEv.put("candidates", cands);
        InteractionRecorder.processEvent(inspectEv);
        Assert.assertFalse(InteractionRecorder.lastPicked.isEmpty());

        InteractionRecorder.processEvent(clickEvent("c1"));
        Assert.assertTrue(InteractionRecorder.lastPicked.isEmpty(),
                "recording a step must clear lastPicked");
    }

    @Test
    public void processEvent_clearAll_resetsLastPickedAndCache() {
        InteractionRecorder.processEvent(clickEvent("c1"));
        InteractionRecorder.cachedCodePreview = "SOME_CODE";
        InteractionRecorder.lastCodeRenderMs = 99999L;
        InteractionRecorder.processEvent(Map.of("type", "clearAll"));
        Assert.assertTrue(InteractionRecorder.STEPS.isEmpty());
        Assert.assertTrue(InteractionRecorder.BY_ID.isEmpty());
        Assert.assertTrue(InteractionRecorder.lastPicked.isEmpty());
        Assert.assertEquals(InteractionRecorder.cachedCodePreview, "");
        Assert.assertEquals(InteractionRecorder.lastCodeRenderMs, 0L);
    }

    @Test
    public void processEvent_delete_byIndex_lastStep() {
        InteractionRecorder.processEvent(clickEvent("a"));
        InteractionRecorder.processEvent(clickEvent("b"));
        InteractionRecorder.processEvent(clickEvent("c"));
        InteractionRecorder.processEvent(Map.of("type", "delete", "id", "c"));
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 2);
        Assert.assertEquals(InteractionRecorder.STEPS.get(1).getId(), "b");
    }

    @Test
    public void processEvent_delete_firstStep_remainingOrderCorrect() {
        InteractionRecorder.processEvent(clickEvent("first"));
        InteractionRecorder.processEvent(clickEvent("second"));
        InteractionRecorder.processEvent(Map.of("type", "delete", "id", "first"));
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 1);
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getId(), "second");
    }

    @Test
    public void processEvent_assertModeToggle_reflectedInRenderJson() {
        String jsonBefore = InteractionRecorder.renderJson();
        JsonObject before = new Gson().fromJson(jsonBefore, JsonObject.class);
        Assert.assertEquals(before.get("assertMode").getAsString(), "soft");

        InteractionRecorder.processEvent(Map.of("type", "assertModeToggle"));
        String jsonAfter = InteractionRecorder.renderJson();
        JsonObject after = new Gson().fromJson(jsonAfter, JsonObject.class);
        Assert.assertEquals(after.get("assertMode").getAsString(), "hard");
    }

    @Test
    public void processEvent_unknownType_withValidId_addsStep() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "futureCustomAction");
        ev.put("id", "fc1");
        ev.put("tag", "div");
        ev.put("name", "");
        ev.put("value", (Object) null);
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        Assert.assertTrue(InteractionRecorder.processEvent(ev),
                "unknown action types with valid id should pass through as steps");
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "futureCustomAction");
    }

    // ─── drainOnce exception isolation ──────────────────────────────────────

    @Test
    public void drainOnce_malformedEvent_doesNotPreventGoodEvents() {
        InteractionRecorder.processEvent(clickEvent("c1"));
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 1);

        Map<String, Object> bad = new HashMap<>();
        bad.put("type", "override");
        bad.put("id", null);

        try {
            InteractionRecorder.processEvent(bad);
        } catch (Exception e) {
            Assert.fail("processEvent must not throw for null id: " + e);
        }

        InteractionRecorder.processEvent(clickEvent("c2"));
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 2,
                "malformed event must not prevent subsequent good events");
    }

    @Test
    public void processEvent_nullEvent_returnsFalse() {
        Map<String, Object> empty = new HashMap<>();
        boolean result = InteractionRecorder.processEvent(empty);
        Assert.assertFalse(result, "empty map with no type must return false");
    }

    @Test
    public void processEvent_nullValue_clickStep_noNullPointer() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "click");
        ev.put("id", "nv1");
        ev.put("tag", "button");
        ev.put("name", "");
        ev.put("value", (Object) null);
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        Assert.assertTrue(InteractionRecorder.processEvent(ev),
                "null value in click event must not throw");
    }

    @Test
    public void processEvent_nullCandidates_clickStep_noNullPointer() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "click");
        ev.put("id", "nc1");
        ev.put("tag", "div");
        ev.put("name", "");
        ev.put("value", (Object) null);
        ev.put("frame", List.of());
        ev.put("candidates", (Object) null);
        Assert.assertTrue(InteractionRecorder.processEvent(ev),
                "null candidates must be treated as empty list");
    }

    @Test
    public void processEvent_emptyFrame_doesNotThrow() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "click");
        ev.put("id", "ef1");
        ev.put("tag", "button");
        ev.put("name", "");
        ev.put("value", (Object) null);
        ev.put("frame", List.of());
        ev.put("candidates", List.of());
        Assert.assertNotNull(InteractionRecorder.STEPS);
        InteractionRecorder.processEvent(ev);
    }

    @Test
    public void processEvent_frameChain_preserved() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "click");
        ev.put("id", "fr1");
        ev.put("tag", "button");
        ev.put("name", "");
        ev.put("value", (Object) null);
        ev.put("frame", List.of(0, 2));
        ev.put("candidates", List.of());
        InteractionRecorder.processEvent(ev);
        List<Integer> chain = InteractionRecorder.STEPS.get(0).getFrameChain();
        Assert.assertEquals(chain, List.of(0, 2), "frame chain must be preserved on step");
    }

    // ─── doubleClick trailing-click dedup ──────────────────────────────────

    @Test
    public void processEvent_doubleClick_dropsMatchingTrailingClicks() {
        List<Map<String, Object>> rawCands = List.of(Map.of(
                "type", "id", "sel", "#btn", "value", "btn", "tier", "id", "unique", true, "param", false));

        Map<String, Object> c1 = new HashMap<>();
        c1.put("type", "click"); c1.put("id", "cl1");
        c1.put("tag", "button"); c1.put("name", ""); c1.put("value", (Object) null);
        c1.put("frame", List.of()); c1.put("candidates", rawCands);
        InteractionRecorder.processEvent(c1);

        Map<String, Object> c2 = new HashMap<>();
        c2.put("type", "click"); c2.put("id", "cl2");
        c2.put("tag", "button"); c2.put("name", ""); c2.put("value", (Object) null);
        c2.put("frame", List.of()); c2.put("candidates", rawCands);
        InteractionRecorder.processEvent(c2);

        Assert.assertEquals(InteractionRecorder.STEPS.size(), 2);

        Map<String, Object> dbl = new HashMap<>();
        dbl.put("type", "doubleClick"); dbl.put("id", "dbl1");
        dbl.put("tag", "button"); dbl.put("name", ""); dbl.put("value", (Object) null);
        dbl.put("frame", List.of()); dbl.put("candidates", rawCands);
        InteractionRecorder.processEvent(dbl);

        Assert.assertEquals(InteractionRecorder.STEPS.size(), 1,
                "doubleClick must remove up to 2 trailing single-click steps on the same element");
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getActionType(), "doubleClick");
    }

    // ─── renderJson — picked locator ───────────────────────────────────────

    @Test
    public void renderJson_withLastPicked_includesPickedSection() {
        LocatorCandidate cand = new LocatorCandidate(By.id("foo"), "By.id(\"foo\")", 0.9, "id", true, false);
        InteractionRecorder.lastPicked = List.of(cand);
        String json = InteractionRecorder.renderJson();
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        Assert.assertTrue(root.has("picked"), "renderJson must include 'picked' when lastPicked is set");
        Assert.assertTrue(root.getAsJsonObject("picked").has("candidates"));
    }

    @Test
    public void renderJson_withNoLastPicked_omitsPicked() {
        InteractionRecorder.lastPicked = List.of();
        String json = InteractionRecorder.renderJson();
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        Assert.assertFalse(root.has("picked"), "renderJson must not include 'picked' when lastPicked is empty");
    }

    @Test
    public void renderJson_stepIncludesAssertAttr() {
        InteractionRecorder.processEvent(Map.of(
                "type", "assertValue",
                "id", "avx",
                "tag", "input",
                "name", "Email",
                "value", "admin@test.com",
                "assertAttr", "value",
                "frame", List.of(),
                "candidates", List.of()));
        String json = InteractionRecorder.renderJson();
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        JsonObject step = root.getAsJsonArray("steps").get(0).getAsJsonObject();
        Assert.assertEquals(step.get("assertAttr").getAsString(), "value");
    }

    @Test
    public void renderJson_stepIncludesFrameArray() {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "click"); ev.put("id", "frx");
        ev.put("tag", "button"); ev.put("name", ""); ev.put("value", (Object) null);
        ev.put("frame", List.of(1, 3)); ev.put("candidates", List.of());
        InteractionRecorder.processEvent(ev);
        String json = InteractionRecorder.renderJson();
        JsonObject step = new Gson().fromJson(json, JsonObject.class)
                .getAsJsonArray("steps").get(0).getAsJsonObject();
        JsonArray frame = step.getAsJsonArray("frame");
        Assert.assertEquals(frame.size(), 2);
        Assert.assertEquals(frame.get(0).getAsInt(), 1);
        Assert.assertEquals(frame.get(1).getAsInt(), 3);
    }

    // ─── OVERLAY_SCRIPT — toolbar button wiring ───────────────────────────

    @Test
    public void overlayScript_pauseButton_wiresClickListener() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("document.getElementById('ell-rec').addEventListener('click'"),
                "pause/resume button must have a click listener wired in OVERLAY_SCRIPT");
    }

    @Test
    public void overlayScript_pauseButton_togglesEllPaused() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("window.__ellPaused=!window.__ellPaused"),
                "pause button handler must toggle __ellPaused");
    }

    @Test
    public void overlayScript_pauseButton_updatesLabel_pauseAndResume() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("Resume"), "pause button must switch label to Resume when paused");
        Assert.assertTrue(s.contains("Pause"), "pause button must switch label back to Pause when resumed");
    }

    @Test
    public void overlayScript_clearButton_pushesLogEvent() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("document.getElementById('ell-clear').addEventListener('click'"),
                "clear button must have a click listener");
        Assert.assertTrue(s.contains("logPush({type:'clearAll'})"),
                "clear button must push clearAll event");
    }

    @Test
    public void overlayScript_stopButton_setsEllStop() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("document.getElementById('ell-stop').addEventListener('click'"),
                "stop button must have a click listener");
        Assert.assertTrue(s.contains("window.__ellStop=true"),
                "stop button must set __ellStop=true");
    }

    @Test
    public void overlayScript_assertToggle_pushesLogEvent() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("document.getElementById('ell-assert').addEventListener('click'"),
                "assert toggle must have a click listener");
        Assert.assertTrue(s.contains("logPush({type:'assertModeToggle'})"),
                "assert toggle must push assertModeToggle event");
    }

    @Test
    public void overlayScript_copyButton_usesClipboardApi() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("navigator.clipboard.writeText"),
                "copy button must use navigator.clipboard.writeText");
    }

    @Test
    public void overlayScript_expandButton_wiresExpandCode() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("document.getElementById('ell-expand').addEventListener('click', expandCode)"),
                "expand button must be wired to expandCode()");
    }

    @Test
    public void overlayScript_armFunction_wiresAllModeBtns() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("arm('ell-pick','inspect')"), "inspect button must be armed");
        Assert.assertTrue(s.contains("arm('ell-hover','hover')"), "hover button must be armed");
        Assert.assertTrue(s.contains("arm('ell-av','assertVisible')"), "assertVisible button must be armed");
        Assert.assertTrue(s.contains("arm('ell-at','assertText')"), "assertText button must be armed");
        Assert.assertTrue(s.contains("arm('ell-aval','assertValue')"), "assertValue button must be armed");
        Assert.assertTrue(s.contains("arm('ell-aen','assertEnabled')"), "assertEnabled button must be armed");
        Assert.assertTrue(s.contains("arm('ell-asel','assertSelected')"), "assertSelected button must be armed");
    }

    @Test
    public void overlayScript_armFunction_togglesEllArmedClass() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("classList.add('ell-armed')"),
                "arm() must add ell-armed class to activate button");
        Assert.assertTrue(s.contains("classList.remove('ell-armed')"),
                "arm() must remove ell-armed class from other buttons on toggle");
    }

    @Test
    public void overlayScript_armFunction_togglesModeOnSecondClick() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("window.__ellMode===mode"),
                "arm() must detect if button is already in active mode");
        Assert.assertTrue(s.contains("window.__ellMode=on?'record':mode"),
                "arm() must toggle mode back to record on second click");
    }

    // ─── RENDER_SCRIPT — step action buttons ─────────────────────────────

    @Test
    public void renderScript_deleteButton_pushesDeleteEvent() {
        String s = InteractionRecorder.RENDER_SCRIPT;
        Assert.assertTrue(s.contains("type:'delete'"),
                "delete button handler must push type:delete");
        Assert.assertTrue(s.contains("data-del"),
                "delete button must carry data-del attribute for step id");
    }

    @Test
    public void renderScript_deleteButton_queriesEllDelClass() {
        String s = InteractionRecorder.RENDER_SCRIPT;
        Assert.assertTrue(s.contains("querySelectorAll('.ell-del')"),
                "RENDER_SCRIPT must query .ell-del buttons to wire up delete");
    }

    @Test
    public void renderScript_radioOverride_pushesOverrideEvent() {
        String s = InteractionRecorder.RENDER_SCRIPT;
        Assert.assertTrue(s.contains("type:'override'"),
                "radio change handler must push type:override");
        Assert.assertTrue(s.contains("data-idx"),
                "override event must carry data-idx for chosen locator index");
    }

    @Test
    public void renderScript_generatorDropdown_pushesAutoGenerate() {
        String s = InteractionRecorder.RENDER_SCRIPT;
        Assert.assertTrue(s.contains("type:'autoGenerate'"),
                "generator dropdown change must push autoGenerate event");
        Assert.assertTrue(s.contains("method:this.value"),
                "autoGenerate event must include the selected method value");
    }

    // ─── CAPTURE_SCRIPT — inBar guard & gesture suppression ──────────────

    @Test
    public void captureScript_inBar_returnsEarlyWithoutStopPropagation() {
        String s = InteractionRecorder.CAPTURE_SCRIPT;
        int clickHandlerIdx = s.indexOf("doc.addEventListener('click'");
        Assert.assertTrue(clickHandlerIdx >= 0, "must have capture-phase click handler");
        String handler = s.substring(clickHandlerIdx, Math.min(clickHandlerIdx + 250, s.length()));
        Assert.assertTrue(handler.contains("if(inBar(raw))return;"),
                "click handler must return cleanly for toolbar elements without stopPropagation");
        Assert.assertFalse(handler.substring(0, handler.indexOf("if(inBar(raw))return;") + 22)
                .contains("e.stopPropagation()"),
                "stopPropagation must NOT appear before the inBar guard — it would block toolbar button events");
    }

    @Test
    public void captureScript_gestureGuard_afterInBarCheck() {
        String s = InteractionRecorder.CAPTURE_SCRIPT;
        int inBarIdx = s.indexOf("if(inBar(raw))return;");
        int gestureIdx = s.indexOf("W.__ellResizing||(W.__ellGestureTs");
        Assert.assertTrue(inBarIdx >= 0, "must have inBar check");
        Assert.assertTrue(gestureIdx >= 0, "must have gesture guard");
        Assert.assertTrue(gestureIdx > inBarIdx,
                "gesture guard must appear AFTER the inBar early-return, not before it");
    }

    @Test
    public void captureScript_gestureGuard_callsStopPropagation() {
        String s = InteractionRecorder.CAPTURE_SCRIPT;
        int gestureIdx = s.indexOf("W.__ellResizing||(W.__ellGestureTs");
        Assert.assertTrue(gestureIdx >= 0, "must have gesture guard");
        String gestureBlock = s.substring(gestureIdx, Math.min(gestureIdx + 100, s.length()));
        Assert.assertTrue(gestureBlock.contains("e.stopPropagation()"),
                "gesture guard must call stopPropagation to suppress phantom page-level clicks");
    }

    @Test
    public void overlayScript_resizeHandle_mousedown_setsEllResizing() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("window.__ellResizing=true"),
                "resize handle mousedown must set window.__ellResizing=true");
    }

    @Test
    public void overlayScript_mouseup_clearsEllResizing() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("window.__ellResizing=false"),
                "document mouseup must clear window.__ellResizing");
    }

    @Test
    public void overlayScript_mouseup_stampsGestureTs() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("window.__ellGestureTs=Date.now()"),
                "mouseup must stamp __ellGestureTs after drag or resize gesture");
    }

    @Test
    public void overlayScript_resizeHandle_mousedown_anchorsExplicitHeight() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("bar.style.height=(r.bottom-r.top)+'px'"),
                "resize mousedown must anchor an explicit height from getBoundingClientRect so vertical drags work even when content is shorter than the new size");
        Assert.assertTrue(s.contains("bar.style.maxHeight='none'"),
                "resize mousedown must clear max-height so the explicit height is not capped");
    }

    @Test
    public void overlayScript_southResize_setsHeightNotMaxHeight() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        int sIdx = s.indexOf("rsDir.indexOf('s')>=0");
        Assert.assertTrue(sIdx >= 0, "must have south resize branch");
        String sBlock = s.substring(sIdx, Math.min(sIdx + 80, s.length()));
        Assert.assertTrue(sBlock.contains("bar.style.height="),
                "south resize must set bar.style.height (not max-height) so dragging down actually grows the toolbar");
        Assert.assertFalse(sBlock.contains("bar.style.maxHeight="),
                "south resize must not use maxHeight — it only caps, it does not force height");
    }

    @Test
    public void overlayScript_northResize_setsHeightNotMaxHeight() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        int nIdx = s.indexOf("rsDir.indexOf('n')>=0");
        Assert.assertTrue(nIdx >= 0, "must have north resize branch");
        String nBlock = s.substring(nIdx, Math.min(nIdx + 100, s.length()));
        Assert.assertTrue(nBlock.contains("bar.style.height="),
                "north resize must set bar.style.height (not max-height) so dragging the top border actually resizes the toolbar");
        Assert.assertFalse(nBlock.contains("bar.style.maxHeight="),
                "north resize must not use maxHeight — it only caps, it does not force height");
    }

    @Test
    public void overlayScript_resizeHandle_click_stopsAndPrevents() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("h.addEventListener('click',function(e){ e.stopPropagation(); e.preventDefault();"),
                "resize handle click must stopPropagation+preventDefault to block accidental page clicks");
    }

    @Test
    public void overlayScript_innerLayout_twoSectionFlexLayout() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("flex:1;overflow:auto;padding:8px 12px;min-height:0"),
                "steps section must be a scrollable flex:1 div so it takes a proportional share of the available bar height");
        Assert.assertTrue(s.contains("display:flex;flex-direction:column;flex:2;min-height:0"),
                "code section must be a flex column with flex:2 so it fills 2/3 of inner space and ell-code inside it can grow to use all of that");
        Assert.assertFalse(s.contains("overflow:auto;flex:1;padding:8px 12px"),
                "no single-scroll wrapper must exist — that layout makes ell-code share a scrollable container with steps so it never fills empty space");
    }

    @Test
    public void overlayScript_jsHeightInit_setsExplicitHeightFromViewport() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("bar.style.height=Math.max(400,Math.min(600,Math.floor(window.innerHeight*.55)))+'px'"),
                "overlay must set an explicit initial bar height (55% of viewport, clamped 400-600px) so the two-section flex layout has a concrete parent height to split between steps and code sections");
        Assert.assertFalse(s.contains("ResizeObserver"),
                "ResizeObserver is not needed — the two-section flex layout with explicit bar height keeps ell-code filling its flex:1 share automatically at any size");
    }

    @Test
    public void overlayScript_codePreview_flexOneInsideCodeSection() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("id=\"ell-code\""),
                "must have ell-code pre element");
        Assert.assertTrue(s.contains("flex:1;min-height:0;overflow:auto;margin:0;"),
                "ell-code must use flex:1 (not max-height) so it fills its containing code section at every bar height without needing a JS observer");
        Assert.assertFalse(s.contains("max-height:26vh"),
                "ell-code must not have a hard max-height — with flex:1 and an explicit bar height the pre element fills available space automatically");
    }

    @Test
    public void overlayScript_dragHandle_mousedown_setsDragTrue() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("drag=true"),
                "drag handle mousedown must set drag=true");
        Assert.assertTrue(s.contains("head.addEventListener('mousedown'"),
                "drag must be initiated from the header mousedown handler");
    }

    @Test
    public void overlayScript_mouseup_clearsDragAndRsz() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("drag=false; rsz=false;"),
                "mouseup must clear both drag and rsz flags");
    }

    // ─── OVERLAY_SCRIPT — toolbar DOM structure ───────────────────────────

    @Test
    public void overlayScript_containsAllToolbarButtons() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        for (String id : List.of("ell-rec", "ell-pick", "ell-hover", "ell-av", "ell-at",
                "ell-aval", "ell-aen", "ell-asel", "ell-assert", "ell-clear", "ell-stop",
                "ell-copy", "ell-expand", "ell-eval")) {
            Assert.assertTrue(s.contains("'"+id+"'") || s.contains("\""+id+"\""),
                    "OVERLAY_SCRIPT must reference toolbar element: " + id);
        }
    }

    @Test
    public void overlayScript_evalInput_stopsPropagation_forKeyEvents() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("ell-eval"),
                "overlay must contain eval input");
        Assert.assertTrue(s.contains("e.stopPropagation()") && s.contains("'keydown'"),
                "keydown events on eval input must be stopped to prevent recording them");
    }

    @Test
    public void overlayScript_expandCode_createsOverlayDiv() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("ell-code-overlay"),
                "expandCode must create the full-screen code overlay");
    }

    @Test
    public void overlayScript_expandCode_escapeKeyClosesOverlay() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("e.key==='Escape'") && s.contains("expandCode"),
                "Escape key must close the code overlay");
    }

    // ─── renderJson structure ─────────────────────────────────────────────

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

    // ─── OVERLAY_SCRIPT — JS height initialisation ───────────────────────

    @Test
    public void overlayScript_barHasMaxHeightCss() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("max-height:86vh"),
                "bar CSS must have max-height:86vh so it never overflows the viewport before the user manually resizes it");
    }

    @Test
    public void overlayScript_jsHeightInit_isClampedNotFullscreen() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("bar.style.height=Math.max(400,Math.min(600,Math.floor(window.innerHeight*.55)))+'px'"),
                "OVERLAY_SCRIPT must set bar height to 55% of viewport clamped between 400 and 600px — tall enough for the two-section layout to work, small enough not to dominate the screen");
        Assert.assertFalse(s.contains("Math.floor(window.innerHeight*.86)"),
                "OVERLAY_SCRIPT must not use 86vh as the height factor — that makes the toolbar fullscreen and unusable");
    }

    @Test
    public void overlayScript_persistsGeometryRatioOnGestureEnd() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("localStorage.setItem('__ellToolbarGeom'"),
                "mouseup must persist the toolbar geometry to localStorage so it survives page navigation");
        Assert.assertTrue(s.contains("w:r.width/window.innerWidth") && s.contains("h:r.height/window.innerHeight"),
                "geometry must be stored as viewport ratios (not absolute px) so it scales correctly across pages with different viewport sizes");
        Assert.assertTrue(s.contains("l:r.left/window.innerWidth") && s.contains("t:r.top/window.innerHeight"),
                "position must also be stored as ratios so the toolbar reappears where the user left it");
    }

    @Test
    public void overlayScript_restoresGeometryRatioOnInjection() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;
        Assert.assertTrue(s.contains("localStorage.getItem('__ellToolbarGeom')"),
                "injection must read the stored geometry so the toolbar restores its last adjusted size instead of the default");
        Assert.assertTrue(s.contains("Math.round(g.w*window.innerWidth)") && s.contains("Math.round(g.h*window.innerHeight)"),
                "stored ratios must be multiplied back by the current viewport to recompute absolute size");
        Assert.assertTrue(s.contains("else { bar.style.height=Math.max(400,Math.min(600,Math.floor(window.innerHeight*.55)))+'px'; }"),
                "the 55%-viewport default must only apply when no stored geometry exists");
    }

    // ─── Compound / integration tests ────────────────────────────────────

    @Test
    public void compound_threeEventSequence_stepsAndByIdAndRenderJsonAllConsistent() {
        InteractionRecorder.processEvent(clickEvent("c1"));
        InteractionRecorder.processEvent(inputEvent("i1", "hello"));
        Map<String, Object> hoverEv = new HashMap<>();
        hoverEv.put("type", "hover"); hoverEv.put("id", "h1");
        hoverEv.put("tag", "a"); hoverEv.put("name", "Link");
        hoverEv.put("value", (Object) null); hoverEv.put("frame", List.of()); hoverEv.put("candidates", List.of());
        InteractionRecorder.processEvent(hoverEv);

        Assert.assertEquals(InteractionRecorder.STEPS.size(), 3, "all three events must be in STEPS");
        Assert.assertTrue(InteractionRecorder.BY_ID.containsKey("c1"), "BY_ID must contain c1");
        Assert.assertTrue(InteractionRecorder.BY_ID.containsKey("i1"), "BY_ID must contain i1");
        Assert.assertTrue(InteractionRecorder.BY_ID.containsKey("h1"), "BY_ID must contain h1");

        String json = InteractionRecorder.renderJson();
        JsonArray steps = new Gson().fromJson(json, JsonObject.class).getAsJsonArray("steps");
        Assert.assertEquals(steps.size(), 3, "renderJson must emit exactly 3 steps");
        Assert.assertEquals(steps.get(0).getAsJsonObject().get("action").getAsString(), "click");
        Assert.assertEquals(steps.get(1).getAsJsonObject().get("action").getAsString(), "input");
        Assert.assertEquals(steps.get(2).getAsJsonObject().get("action").getAsString(), "hover");
    }

    @Test
    public void compound_deleteMiddleStep_renderJsonReflectsCorrectOrder() {
        InteractionRecorder.processEvent(clickEvent("a"));
        InteractionRecorder.processEvent(clickEvent("b"));
        InteractionRecorder.processEvent(clickEvent("c"));

        InteractionRecorder.processEvent(Map.of("type", "delete", "id", "b"));

        Assert.assertEquals(InteractionRecorder.STEPS.size(), 2);
        Assert.assertFalse(InteractionRecorder.BY_ID.containsKey("b"), "deleted step must be removed from BY_ID");

        String json = InteractionRecorder.renderJson();
        JsonArray steps = new Gson().fromJson(json, JsonObject.class).getAsJsonArray("steps");
        Assert.assertEquals(steps.size(), 2, "renderJson must show 2 steps after delete");
        Assert.assertEquals(steps.get(0).getAsJsonObject().get("id").getAsString(), "a");
        Assert.assertEquals(steps.get(1).getAsJsonObject().get("id").getAsString(), "c");
    }

    @Test
    public void compound_malformedEventBetweenGoodEvents_doesNotCorruptState() {
        InteractionRecorder.processEvent(clickEvent("good1"));
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 1);

        Map<String, Object> badOverride = new HashMap<>();
        badOverride.put("type", "override");
        badOverride.put("id", null);
        try { InteractionRecorder.processEvent(badOverride); } catch (Exception ignored) {}

        InteractionRecorder.processEvent(clickEvent("good2"));

        Assert.assertEquals(InteractionRecorder.STEPS.size(), 2,
                "malformed override must not corrupt state — both good steps must be present");
        Assert.assertTrue(InteractionRecorder.BY_ID.containsKey("good1"));
        Assert.assertTrue(InteractionRecorder.BY_ID.containsKey("good2"));
        String json = InteractionRecorder.renderJson();
        Assert.assertEquals(new Gson().fromJson(json, JsonObject.class).getAsJsonArray("steps").size(), 2);
    }

    @Test
    public void compound_assertValueWithAttrThenOverrideLocator_bothChangesPersistInRenderJson() {
        LocatorCandidate c0 = new LocatorCandidate(By.id("x"), "By.id(\"x\")", 0.9, "id", true, false);
        LocatorCandidate c1 = new LocatorCandidate(By.cssSelector(".y"), "By.cssSelector(\".y\")", 0.7, "css", true, false);

        InteractionRecorder.processEvent(Map.of(
                "type", "assertValue", "id", "av1", "tag", "input", "name", "Email",
                "value", "test@test.com", "assertAttr", "placeholder",
                "frame", List.of(), "candidates", List.of()));

        RecordedStep step = InteractionRecorder.BY_ID.get("av1");
        step.getCandidates().clear();
        step.getCandidates().add(c0);
        step.getCandidates().add(c1);

        InteractionRecorder.processEvent(Map.of("type", "override", "id", "av1", "index", 1.0));

        String json = InteractionRecorder.renderJson();
        JsonObject stepJson = new Gson().fromJson(json, JsonObject.class)
                .getAsJsonArray("steps").get(0).getAsJsonObject();
        Assert.assertEquals(stepJson.get("assertAttr").getAsString(), "placeholder",
                "assertAttr set by assertValue event must survive subsequent locator override");
        Assert.assertEquals(stepJson.get("chosenIndex").getAsInt(), 1,
                "chosenIndex updated by override event must appear in renderJson");
    }

    @Test
    public void compound_clearAllThenReAdd_freshSequenceWithNoLeftovers() {
        InteractionRecorder.processEvent(clickEvent("old1"));
        InteractionRecorder.processEvent(clickEvent("old2"));
        InteractionRecorder.processEvent(Map.of("type", "clearAll"));

        Assert.assertEquals(InteractionRecorder.STEPS.size(), 0);
        Assert.assertTrue(InteractionRecorder.BY_ID.isEmpty());

        InteractionRecorder.processEvent(clickEvent("new1"));
        InteractionRecorder.processEvent(inputEvent("new2", "value"));

        Assert.assertEquals(InteractionRecorder.STEPS.size(), 2);
        Assert.assertFalse(InteractionRecorder.BY_ID.containsKey("old1"), "cleared steps must not reappear in BY_ID");
        String json = InteractionRecorder.renderJson();
        JsonArray steps = new Gson().fromJson(json, JsonObject.class).getAsJsonArray("steps");
        Assert.assertEquals(steps.size(), 2);
        Assert.assertEquals(steps.get(0).getAsJsonObject().get("id").getAsString(), "new1");
    }

    @Test
    public void compound_assertModeToggleAndRenderJson_optionsAndJsonBothFlipTogether() {
        Assert.assertTrue(InteractionRecorder.options.isSoftAssert(), "must start soft");
        InteractionRecorder.processEvent(clickEvent("cx"));
        InteractionRecorder.processEvent(Map.of("type", "assertModeToggle"));

        Assert.assertFalse(InteractionRecorder.options.isSoftAssert(), "options must be hard after toggle");
        String json = InteractionRecorder.renderJson();
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        Assert.assertEquals(root.get("assertMode").getAsString(), "hard",
                "renderJson assertMode must match the toggled options state");
        Assert.assertEquals(root.getAsJsonArray("steps").size(), 1,
                "the click step recorded before the toggle must still be present in renderJson");
    }

    @Test
    public void compound_resizeLifecycle_allPhasesPresent() {
        String s = InteractionRecorder.OVERLAY_SCRIPT;

        int mousedownIdx = s.indexOf("h.addEventListener('mousedown',function(e){");
        Assert.assertTrue(mousedownIdx >= 0, "must have resize handle mousedown handler");
        String mousedownBlock = s.substring(mousedownIdx, Math.min(mousedownIdx + 400, s.length()));
        Assert.assertTrue(mousedownBlock.contains("window.__ellResizing=true"),
                "mousedown must set __ellResizing");
        Assert.assertTrue(mousedownBlock.contains("bar.style.height=(r.bottom-r.top)+'px'"),
                "mousedown must anchor explicit height");
        Assert.assertTrue(mousedownBlock.contains("bar.style.maxHeight='none'"),
                "mousedown must clear max-height so explicit height is not capped");

        int mousemoveIdx = s.indexOf("document.addEventListener('mousemove'");
        Assert.assertTrue(mousemoveIdx >= 0, "must have mousemove handler");
        String mousemoveBlock = s.substring(mousemoveIdx, Math.min(mousemoveIdx + 800, s.length()));
        Assert.assertTrue(mousemoveBlock.contains("bar.style.height=Math.max(200,e.clientY-rsTop0)+'px'"),
                "south resize mousemove must set bar.style.height (not maxHeight)");
        Assert.assertTrue(mousemoveBlock.contains("bar.style.height=nh+'px'"),
                "north resize mousemove must set bar.style.height (not maxHeight)");

        int mouseupIdx = s.indexOf("document.addEventListener('mouseup'");
        Assert.assertTrue(mouseupIdx >= 0, "must have mouseup handler");
        String mouseupBlock = s.substring(mouseupIdx, Math.min(mouseupIdx + 500, s.length()));
        Assert.assertTrue(mouseupBlock.contains("window.__ellGestureTs=Date.now()"),
                "mouseup must stamp gesture timestamp");
        Assert.assertTrue(mouseupBlock.contains("window.__ellResizing=false"),
                "mouseup must clear __ellResizing");
        Assert.assertTrue(mouseupBlock.contains("drag=false; rsz=false;"),
                "mouseup must clear drag and rsz flags");

        Assert.assertTrue(mousedownIdx < mousemoveIdx && mousemoveIdx < mouseupIdx,
                "event handler registration order must be: mousedown → mousemove → mouseup");
    }

    @Test
    public void compound_multipleActionTypes_renderJsonPreservesAllActionsAndOrder() {
        String[] types = {"click", "input", "hover", "assertVisible", "pressEnter"};
        String[] ids   = {"t1",    "t2",    "t3",    "t4",             "t5"};
        for (int i = 0; i < types.length; i++) {
            Map<String, Object> ev = new HashMap<>();
            ev.put("type", types[i]); ev.put("id", ids[i]);
            ev.put("tag", "div"); ev.put("name", ""); ev.put("value", (Object) null);
            ev.put("frame", List.of()); ev.put("candidates", List.of());
            InteractionRecorder.processEvent(ev);
        }

        Assert.assertEquals(InteractionRecorder.STEPS.size(), 5);
        String json = InteractionRecorder.renderJson();
        JsonArray steps = new Gson().fromJson(json, JsonObject.class).getAsJsonArray("steps");
        Assert.assertEquals(steps.size(), 5);
        for (int i = 0; i < types.length; i++) {
            JsonObject step = steps.get(i).getAsJsonObject();
            Assert.assertEquals(step.get("id").getAsString(), ids[i],
                    "step " + i + " id must match insertion order");
            Assert.assertEquals(step.get("action").getAsString(), types[i],
                    "step " + i + " action must match recorded type");
        }
    }

    @Test
    public void compound_overrideLocatorThenDelete_byIdConsistentWithSteps() {
        LocatorCandidate c0 = new LocatorCandidate(By.id("p"), "By.id(\"p\")", 0.9, "id", true, false);
        LocatorCandidate c1 = new LocatorCandidate(By.cssSelector(".q"), "By.cssSelector(\".q\")", 0.8, "css", true, false);
        InteractionRecorder.processEvent(clickEvent("x1"));
        InteractionRecorder.processEvent(clickEvent("x2"));

        RecordedStep x1 = InteractionRecorder.BY_ID.get("x1");
        x1.getCandidates().add(c0); x1.getCandidates().add(c1);
        InteractionRecorder.processEvent(Map.of("type", "override", "id", "x1", "index", 1.0));
        Assert.assertEquals(x1.getChosenIndex(), 1, "locator override must be reflected on step");

        InteractionRecorder.processEvent(Map.of("type", "delete", "id", "x1"));
        Assert.assertFalse(InteractionRecorder.BY_ID.containsKey("x1"),
                "deleted step must be removed from BY_ID even after locator override");
        Assert.assertEquals(InteractionRecorder.STEPS.size(), 1);
        Assert.assertEquals(InteractionRecorder.STEPS.get(0).getId(), "x2");
    }
}
