package ai;

import Ellithium.core.ai.SemanticQueryBuilder;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Train/serve PARITY guard for the Tier 3 query string.
 *
 * <p>The bi-encoder is fine-tuned on queries produced by {@code build_query_v2} in
 * {@code kaggle-finetune/01_data_generation.py}; at runtime the SAME query must be produced by
 * {@link SemanticQueryBuilder#build}. If the two diverge, the model is served a different string
 * than it trained on (train/serve skew) and Tier 3 silently degrades — this class has bitten the
 * project repeatedly (action-expansion skew, the missing token dedup).
 *
 * <p>The {@code expected} strings below are GROUND TRUTH captured by running the real Python
 * {@code build_query_v2} (see the cases in the data provider). Each Java call uses the SERVE-form
 * Ellithium action (e.g. {@code sendData}) whose {@code expandAction} must map to the same tokens as
 * the TRAIN-form action ({@code type}) — that mapping equality is exactly what we are guarding.
 *
 * <p>To regenerate the fixtures after a deliberate format change, run in {@code kaggle-finetune/}:
 * <pre>
 *   python -c "import importlib.util as u; s=u.spec_from_file_location('dg','01_data_generation.py'); \
 *     m=u.module_from_spec(s); s.loader.exec_module(m); print(m.build_query_v2(...))"
 * </pre>
 * and update both sides in the same commit.
 */
public class SemanticQueryParityTest {

    /**
     * Columns: label, serve action, broken locator, method, then last-known fingerprint fields
     * (text, id, ariaLabel, placeholder, dataTestId, tag) as build() takes them, then the expected
     * query string from Python build_query_v2. READABLE cases pass a null lastText (buildFromContext
     * drops it for READABLE actions — that de-poisoning is part of the parity).
     */
    @DataProvider(name = "parityCases")
    public Object[][] parityCases() {
        return new Object[][]{
                // click, By-prefixed locator, cold start (no fingerprint)
                {"click cold", "clickOnElement", "By.id: loginBtn", "clickLoginButton",
                        null, null, null, null, null, null,
                        "click press button login btn"},
                // click, raw locator, with fingerprint (id+tag)
                {"click hot", "clickOnElement", "loginBtn", "clickLoginButton",
                        null, "loginBtn", null, null, null, "button",
                        "click press button login btn"},
                // type (serve sendData), id+placeholder+tag, UI-suffix method (Field kept, not stripped)
                {"type field", "sendData", "By.id: userEmail", "sendUserEmailField",
                        null, "userEmail", null, "Email", null, "input",
                        "type input enter text user email field"},
                // type, snake_case locator, cold start
                {"type snake", "sendData", "user_name", "setUserName",
                        null, null, null, null, null, null,
                        "type input enter text user name"},
                // read (serve getText), READABLE -> lastText dropped
                {"read readable", "getText", "By.id: message", "getSecureAreaMessage",
                        null, "message", null, null, null, "div",
                        "read text label value message secure area div"},
                // select (serve selectDropdown)
                {"select", "selectDropdown", "By.id: countryDropdown", "selectCountry",
                        null, "countryDropdown", null, null, null, "select",
                        "select dropdown option choose country"},
                // click, css locator with dot+dash, cold start
                {"click css", "clickOnElement", "By.cssSelector: button.submit-btn", "submitForm",
                        null, null, null, null, null, null,
                        "click press button button.submit btn form"},
                // type, xpath locator, id+aria+tag (id BEFORE aria — field order check)
                {"type xpath", "sendData", "By.xpath: //input[@id=\"q\"]", "searchProducts",
                        null, "q", "Search", null, null, "input",
                        "type input enter text //input[@id=\"q\"] products q search"},
                // read, raw locator, READABLE, method 'value' token deduped against action expansion
                {"read value dedup", "getText", "totalAmount", "getTotalAmountValue",
                        null, "totalAmount", null, null, null, "span",
                        "read text label value total amount span"},
                // is* method (serve clickOnElement), id+data-testid+tag
                {"is enabled", "clickOnElement", "By.name: submit", "isCheckoutEnabled",
                        null, "submit", null, null, "checkout-btn", "button",
                        "click press button submit checkout enabled btn"},
                // READ-family serve skew fix: is*/getAttribute actions categorize READABLE → must
                // expand to "read text label value" (what training used for the collapsed 'read' key),
                // NOT the old bespoke "check visible display" / "read attribute property". READABLE
                // drops lastText (null), mirroring buildFromContext.
                {"isDisplayed→read", "isElementDisplayed", "By.id: loginBtn", "isLoginButtonVisible",
                        null, "loginBtn", null, null, null, "button",
                        "read text label value login btn button visible"},
                {"getAttr→read", "getAttributeValue", "By.id: email", "getEmailFieldValue",
                        null, "email", null, null, null, "input",
                        "read text label value email field input"},
        };
    }

    @Test(dataProvider = "parityCases")
    public void buildMatchesPythonBuildQueryV2(String label, String action, String locator,
                                               String method, String lastText, String lastId,
                                               String lastAria, String lastPlaceholder,
                                               String lastDataTestId, String lastTag,
                                               String expected) {
        String actual = SemanticQueryBuilder.build(action, locator, method, null,
                lastText, lastId, lastAria, lastPlaceholder, lastDataTestId, lastTag);
        Assert.assertEquals(actual, expected,
                "Query parity broken for case '" + label + "' — Java SemanticQueryBuilder.build "
                        + "diverged from Python build_query_v2. Fix both sides in the same commit.");
    }

    /**
     * Mobile (Appium) parity: the resource-id / accessibility-id / content-desc slots must produce
     * the SAME query as Python build_query_v2. Expected strings are GROUND TRUTH captured by running
     * the real Python with train-form actions (click/type/read) and a mobile fingerprint dict.
     * Columns: label, serve action, broken locator, method, lastTag, resourceId, accessibilityId,
     * contentDesc, expected. (Web slots id/aria/placeholder/data-testid/text are null for these.)
     */
    @DataProvider(name = "mobileParityCases")
    public Object[][] mobileParityCases() {
        return new Object[][]{
                {"click mobile", "clickOnElement", "By.id: loginBtn", "clickLoginButton",
                        "android.widget.button", "com.app:id/loginBtn", "loginButton", "Login",
                        "click press button login btn android.widget.button"},
                {"input mobile", "sendData", "By.id: userEmail", "setUserEmail",
                        "android.widget.edittext", "com.app:id/userEmail", "emailField", null,
                        "type input enter text user email android.widget.edittext field"},
                // READABLE drops content-desc (buildFromContext nulls it) — pass null to mirror that.
                {"read mobile (content-desc dropped)", "getText", "By.id: status", "getStatusText",
                        "android.widget.textview", null, "statusBanner", null,
                        "read text label value status android.widget.textview banner"},
        };
    }

    @Test(dataProvider = "mobileParityCases")
    public void buildMobileMatchesPythonBuildQueryV2(String label, String action, String locator,
                                                     String method, String lastTag, String resourceId,
                                                     String accessibilityId, String contentDesc,
                                                     String expected) {
        String actual = SemanticQueryBuilder.build(action, locator, method, null,
                null, null, null, null, null, lastTag,
                resourceId, accessibilityId, contentDesc);
        Assert.assertEquals(actual, expected,
                "Mobile query parity broken for '" + label + "' — Java diverged from Python "
                        + "build_query_v2 mobile slots. Fix both sides in the same commit.");
    }

    /** Tokens must never repeat — the dedup is the headline parity fix. */
    @Test
    public void noDuplicateTokens() {
        String q = SemanticQueryBuilder.build("clickOnElement", "By.id: loginBtn", "clickLoginButton",
                null, null, "loginBtn", null, null, null, "button");
        String[] toks = q.split(" ");
        java.util.Set<String> uniq = new java.util.HashSet<>(java.util.Arrays.asList(toks));
        Assert.assertEquals(uniq.size(), toks.length, "Query has duplicate tokens: " + q);
    }
}
