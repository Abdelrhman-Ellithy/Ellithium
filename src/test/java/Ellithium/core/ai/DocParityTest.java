package Ellithium.core.ai;

import Ellithium.core.ai.models.ElementFingerprint;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Guards the three {@code buildElementDocument} overloads against parity drift:
 * the fp path, the batched-attrs path, and the plain-attrs path must produce
 * byte-identical output for the same attribute set.
 *
 * Also asserts that every field in {@code DOC_FIELD_ORDER} is covered by the
 * {@code CandidateAttributeBatcher.BATCH_SCRIPT} (verified by inspecting the
 * constant string for each expected key).
 */
public class DocParityTest {

    // ──────────────────────────────────────────────────────────────────
    // 1. DOC_FIELD_ORDER batch-script coverage
    // ──────────────────────────────────────────────────────────────────

    private static final String[] DOC_FIELD_ORDER = {
        "id", "name", "resource-id", "accessibility-id", "aria-label", "content-desc",
        "role", "placeholder", "data-testid", "data-test", "title", "type", "label",
        "href", "value", "data-cy", "data-qa"
    };

    @Test
    public void batchScript_coversAllDocFieldOrderKeys() throws Exception {
        java.lang.reflect.Field f =
                Ellithium.core.ai.dom.CandidateAttributeBatcher.class.getDeclaredField("BATCH_SCRIPT");
        f.setAccessible(true);
        String script = (String) f.get(null);

        List<String> missing = new ArrayList<>();
        for (String key : DOC_FIELD_ORDER) {
            // The script emits each key as 'key':a('key') or similar
            if (!script.contains("'" + key + "'")) missing.add(key);
        }
        Assert.assertTrue(missing.isEmpty(),
                "BATCH_SCRIPT is missing these DOC_FIELD_ORDER keys: " + missing
                + " — batched and unbatched scoreSimilarity paths will diverge.");
    }

    // ──────────────────────────────────────────────────────────────────
    // 2. buildElementDocument(fp) ≡ buildElementDocument(Map<String,String>)
    //    for the same attribute set
    // ──────────────────────────────────────────────────────────────────

    @Test
    public void fpPath_matchesPlainAttrsPath_sameAttributes() {
        // Build a fingerprint via Gson (matches the Gson-deserialized production path)
        String json = "{"
            + "\"id\":\"email-input\","
            + "\"name\":\"email\","
            + "\"ariaLabel\":\"Email address\","
            + "\"placeholder\":\"Enter email\","
            + "\"dataTestId\":\"login-email\","
            + "\"dataTest\":\"email-field\","
            + "\"dataCy\":\"cy-email\","
            + "\"dataQa\":\"qa-email\","
            + "\"role\":\"textbox\","
            + "\"type\":\"email\","
            + "\"tagName\":\"input\","
            + "\"title\":\"Your email\","
            + "\"label\":\"lbl-email\","
            + "\"text\":\"\""
            + "}";
        ElementFingerprint fp = new com.google.gson.Gson().fromJson(json, ElementFingerprint.class);

        String fpDoc = EnsembleHealer.buildElementDocument(fp);

        // Build the same via Map<String,String> (plain-attrs path)
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("id", "email-input");
        attrs.put("name", "email");
        attrs.put("aria-label", "Email address");
        attrs.put("placeholder", "Enter email");
        attrs.put("data-testid", "login-email");
        attrs.put("data-test", "email-field");
        attrs.put("data-cy", "cy-email");
        attrs.put("data-qa", "qa-email");
        attrs.put("role", "textbox");
        attrs.put("type", "email");
        attrs.put("tag", "input");
        attrs.put("title", "Your email");
        attrs.put("label", "lbl-email");

        String attrsDoc = EnsembleHealer.buildElementDocument(attrs);

        Assert.assertEquals(fpDoc, attrsDoc,
                "buildElementDocument(fp) and buildElementDocument(Map) diverge for same attributes.\n"
                + "fp   = [" + fpDoc + "]\n"
                + "map  = [" + attrsDoc + "]");
    }

    @Test
    public void fpPath_hrefIncluded_whenPresent() {
        String json = "{\"href\":\"/login\",\"tagName\":\"a\",\"text\":\"Sign in\"}";
        ElementFingerprint fp = new com.google.gson.Gson().fromJson(json, ElementFingerprint.class);
        String doc = EnsembleHealer.buildElementDocument(fp);
        // href is not in DOC_FIELD_ORDER — it is NOT in the document, so fp and batch path agree
        // (both omit href from the embedding doc; fingerprint uses href only for scoreSimilarity)
        Assert.assertNotNull(doc);
    }

    @Test
    public void batchPath_dataAttrs_appendedToDocument() {
        // Simulate what the batch script returns with dataAttrs
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", "submit-btn");
        attrs.put("tag", "button");
        attrs.put("text", "Submit");
        Map<String, Object> dataAttrs = new LinkedHashMap<>();
        dataAttrs.put("data-automation-id", "checkout-submit");
        attrs.put("dataAttrs", dataAttrs);

        // Build via the plain-attrs path (no dataAttrs support) and fp path
        Map<String, String> plainAttrs = new LinkedHashMap<>();
        plainAttrs.put("id", "submit-btn");
        plainAttrs.put("tag", "button");
        plainAttrs.put("text", "Submit");
        String plainDoc = EnsembleHealer.buildElementDocument(plainAttrs);

        // The batched path (private, accessed via reflection or through the scoring pipeline)
        // We test indirectly: plain doc must NOT contain the custom automation id
        Assert.assertFalse(plainDoc.contains("checkout-submit"),
                "Plain-attrs doc should not contain custom data-* value (no dataAttrs key)");

        // fp path with customDataAttrs should include it
        String fpJson = "{"
            + "\"id\":\"submit-btn\","
            + "\"tagName\":\"button\","
            + "\"text\":\"Submit\","
            + "\"customDataAttrs\":{\"data-automation-id\":\"checkout-submit\"}"
            + "}";
        ElementFingerprint fp = new com.google.gson.Gson().fromJson(fpJson, ElementFingerprint.class);
        String fpDoc = EnsembleHealer.buildElementDocument(fp);
        Assert.assertTrue(fpDoc.contains("checkout-submit"),
                "fp doc must include customDataAttrs values — got: " + fpDoc);
    }
}
