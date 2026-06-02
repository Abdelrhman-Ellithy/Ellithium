package Ellithium.core.ai.codegen;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import Ellithium.core.ai.codegen.LocatorCandidate;
import Ellithium.core.ai.codegen.PomCodeEmitter;
import Ellithium.core.ai.codegen.RecordedStep;

import java.util.List;

public class PomCodeEmitterTest {

    private static RecordedStep navigate(String url) {
        return new RecordedStep("nav", "navigate", url, null, null, List.of());
    }

    private static RecordedStep step(String action, String data, String name, LocatorCandidate c) {
        return new RecordedStep("s", action, data, "el", name, List.of(c));
    }

    @Test
    public void emitsGroundedInteractionsApiAndParameterizesDynamicLocator() {
        LocatorCandidate click = new LocatorCandidate(
                By.id("loginBtn"), "By.id(\"loginBtn\")", 0.9, "id", true, false);
        LocatorCandidate select = new LocatorCandidate(
                By.cssSelector("[data-choice-index='0']"),
                "By.cssSelector(\"[data-choice-index='0']\")", 0.4, "data-choice-index", true, true);

        List<RecordedStep> steps = List.of(
                navigate("https://app.test/forecast"),
                step("click", null, "Login", click),
                step("select", "January", "Month", select));

        PomCodeEmitter.EmitResult r = PomCodeEmitter.build(steps, "ForecastPlansPage");
        String stmts = String.join("\n", r.statements());

        Assert.assertTrue(stmts.contains(
                "driverActions.navigation().navigateToUrl(\"https://app.test/forecast\");"), stmts);
        Assert.assertTrue(stmts.contains("driverActions.elements().clickOnElement(login);"), stmts);
        Assert.assertTrue(stmts.contains(
                "driverActions.select().selectDropdownByText(By.cssSelector(String.format("
                + "\"[data-choice-index='%s']\", value0)), \"January\");"), stmts);

        Assert.assertTrue(r.locatorFields().contains("private final By login = By.id(\"loginBtn\");"),
                r.locatorFields().toString());
        Assert.assertTrue(r.methods().get(0).contains("public ForecastPlansPage perform(String value0)"),
                r.methods().get(0));
    }

    private static List<RecordedStep> assertSteps() {
        LocatorCandidate c = new LocatorCandidate(By.id("flash"), "By.id(\"flash\")", 0.9, "id", true, false);
        return List.of(
                step("assertVisible", null, "Flash", c),
                step("assertText", "Logged in", "Flash", c),
                step("assertValue", "john@x.com", "Email", c));
    }

    @Test
    public void hardMode_emitsStaticAssertionExecutor() {
        String stmts = String.join("\n", PomCodeEmitter.build(assertSteps(), "P", false, false).statements());
        Assert.assertTrue(stmts.contains(
                "Ellithium.Utilities.assertion.AssertionExecutor.hard.assertTrue("
                + "driverActions.elements().isElementDisplayed(flash)"), stmts);
        Assert.assertTrue(stmts.contains(
                "Ellithium.Utilities.assertion.AssertionExecutor.hard.assertContains("
                + "driverActions.elements().getText(flash), \"Logged in\");"), stmts);
        Assert.assertTrue(stmts.contains(
                "Ellithium.Utilities.assertion.AssertionExecutor.hard.assertEquals("
                + "driverActions.elements().getAttributeValue(flash, \"value\"), \"john@x.com\");"), stmts);
    }

    @Test
    public void softMode_isDefault_usesSoftAssertAndAssertAll() {
        PomCodeEmitter.EmitResult r = PomCodeEmitter.build(assertSteps(), "P");
        String stmts = String.join("\n", r.statements());
        Assert.assertTrue(stmts.contains("softAssert.assertTrue(driverActions.elements().isElementDisplayed(flash)"), stmts);
        Assert.assertTrue(stmts.contains("softAssert.assertContains(driverActions.elements().getText(flash), \"Logged in\");"), stmts);
        Assert.assertTrue(stmts.contains("softAssert.assertEquals(driverActions.elements().getAttributeValue(flash, \"value\"), \"john@x.com\");"), stmts);
        String method = r.methods().get(0);
        Assert.assertTrue(method.contains("AssertionExecutor.soft softAssert = new Ellithium.Utilities.assertion.AssertionExecutor.soft();"), method);
        Assert.assertTrue(method.contains("softAssert.assertAll();"), method);
    }

    @Test
    public void wrapsFramedStepWithFrameSwitches() {
        LocatorCandidate c = new LocatorCandidate(By.id("inner"), "By.id(\"inner\")", 0.9, "id", true, false);
        RecordedStep framed = new RecordedStep("f", "click", null, "button", "Inner", List.of(c), List.of(0, 2));
        List<String> stmts = PomCodeEmitter.build(List.of(framed), "P").statements();

        Assert.assertEquals(stmts.get(0), "driverActions.frames().switchToFrameByIndex(0);");
        Assert.assertEquals(stmts.get(1), "driverActions.frames().switchToFrameByIndex(2);");
        Assert.assertEquals(stmts.get(2), "driverActions.elements().clickOnElement(inner);");
        Assert.assertEquals(stmts.get(3), "driverActions.frames().switchToDefaultContent();");
    }

    @Test
    public void previewTestSource_generatesRunnableTestNgClass() {
        LocatorCandidate user = new LocatorCandidate(By.id("username"), "By.id(\"username\")", 0.9, "id", true, false);
        List<RecordedStep> steps = List.of(
                step("click", null, "Username", user),
                step("input", "tomsmith", "Username", user));

        String src = PomCodeEmitter.previewTestSource(steps, "LoginPage", "Pages",
                "https://the-internet.herokuapp.com/login", "Chrome");

        Assert.assertTrue(src.contains("package Pages;"), src);
        Assert.assertTrue(src.contains("public class LoginPageTest {"), src);
        Assert.assertTrue(src.contains("import org.testng.annotations.Test;"), src);
        Assert.assertTrue(src.contains("LocalDriverConfig driverConfig = new LocalDriverConfig(LocalDriverType.Chrome,"), src);
        Assert.assertTrue(src.contains("driver = DriverFactory.getNewDriver(driverConfig);"), src);
        Assert.assertTrue(src.contains(
                "driverActions.navigation().navigateToUrl(\"https://the-internet.herokuapp.com/login\");"), src);
        Assert.assertTrue(src.contains("@Test\n    public void perform() {"), src);
        Assert.assertTrue(src.contains(
                "driverActions.elements().sendData(username, Ellithium.Utilities.helpers.JsonHelper"
                + ".getJsonKeyValue(\"src/test/resources/TestData/LoginPage.json\", \"username\"));"), src);
        Assert.assertTrue(src.contains("DriverFactory.quitDriver();"), src);
        Assert.assertFalse(src.contains("return this;"), "a test method must not return this");
    }

    @Test
    public void fieldNames_avoidJavaKeywordsAndDriverFields() {
        LocatorCandidate a = new LocatorCandidate(By.id("a"), "By.id(\"a\")", 0.9, "id", true, false);
        LocatorCandidate b = new LocatorCandidate(By.id("b"), "By.id(\"b\")", 0.9, "id", true, false);
        List<RecordedStep> steps = List.of(
                step("click", null, "class", a),
                step("click", null, "driver", b));
        List<String> fields = PomCodeEmitter.build(steps, "P").locatorFields();
        String all = String.join("\n", fields);
        Assert.assertFalse(all.contains("By class ="), "must not use Java keyword as field name: " + all);
        Assert.assertFalse(all.contains("By driver ="), "must not collide with the driver field: " + all);
        Assert.assertTrue(all.contains("classField"), all);
        Assert.assertTrue(all.contains("driverField"), all);
    }

    @Test
    public void pressEnter_emitsKeysEnterSendData() {
        LocatorCandidate c = new LocatorCandidate(By.id("q"), "By.id(\"q\")", 0.9, "id", true, false);
        List<RecordedStep> steps = List.of(
                step("input", "playwright", "Search", c),
                step("pressEnter", null, "Search", c));
        String stmts = String.join("\n", PomCodeEmitter.build(steps, "P").statements());
        Assert.assertTrue(stmts.contains(
                "driverActions.elements().sendData(search, Ellithium.Utilities.helpers.JsonHelper"
                + ".getJsonKeyValue(\"src/test/resources/TestData/P.json\", \"search\"));"), stmts);
        Assert.assertTrue(stmts.contains("driverActions.elements().sendData(search, org.openqa.selenium.Keys.ENTER);"), stmts);
    }

    @Test
    public void passwordValue_isEmittedAsEnvLookupNotPlaintext() {
        LocatorCandidate c = new LocatorCandidate(By.id("pw"), "By.id(\"pw\")", 0.9, "id", true, false);
        PomCodeEmitter.EmitResult r = PomCodeEmitter.build(List.of(step("input", "__ELL_SECRET__", "Password", c)), "P");
        String stmts = String.join("\n", r.statements());
        Assert.assertTrue(stmts.contains(
                "driverActions.elements().sendData(password, Ellithium.Utilities.helpers.JsonHelper"
                + ".getJsonKeyValue(\"src/test/resources/TestData/P.json\", \"password\"));"), stmts);
        Assert.assertFalse(stmts.contains("__ELL_SECRET__"), "the sentinel must not leak into code: " + stmts);
        Assert.assertEquals(r.testData().get("password"), "", "secret stored as empty string for manual fill-in");
    }

    @Test
    public void emptyAssertText_isSkipped() {
        LocatorCandidate c = new LocatorCandidate(By.id("x"), "By.id(\"x\")", 0.9, "id", true, false);
        List<String> stmts = PomCodeEmitter.build(List.of(step("assertText", "", "X", c)), "P").statements();
        Assert.assertTrue(stmts.isEmpty(), "a blank-text assertion is vacuous and must be skipped: " + stmts);
    }

    @Test
    public void mapsMouseAndUploadActions() {
        LocatorCandidate c = new LocatorCandidate(By.id("x"), "By.id(\"x\")", 0.9, "id", true, false);
        List<RecordedStep> steps = List.of(
                step("doubleClick", null, "Box", c),
                step("hover", null, "Box", c),
                step("rightClick", null, "Box", c),
                step("uploadFile", "photo.png", "Box", c));
        String stmts = String.join("\n", PomCodeEmitter.build(steps, "P").statements());
        Assert.assertTrue(stmts.contains("driverActions.mouse().doubleClick(box);"), stmts);
        Assert.assertTrue(stmts.contains("driverActions.mouse().hoverOverElement(box);"), stmts);
        Assert.assertTrue(stmts.contains("driverActions.mouse().rightClick(box);"), stmts);
        Assert.assertTrue(stmts.contains("driverActions.elements().uploadFile(box, \"photo.png\");"), stmts);
    }

    @Test
    public void unmappedAction_isSkippedNotEmittedInvalid() {
        LocatorCandidate c = new LocatorCandidate(By.id("x"), "By.id(\"x\")", 0.9, "id", true, false);
        PomCodeEmitter.EmitResult r = PomCodeEmitter.build(
                List.of(step("teleport", null, "X", c)), "P");
        Assert.assertTrue(r.statements().isEmpty(), "no API mapping → no statement emitted");
    }

    @Test
    public void navigateBack_emitsNavigateBackCall() {
        RecordedStep back = new RecordedStep("b1", "navigateBack", null, null, null, List.of());
        List<String> stmts = PomCodeEmitter.build(List.of(back), "P").statements();
        Assert.assertEquals(stmts.size(), 1);
        Assert.assertEquals(stmts.get(0), "driverActions.navigation().navigateBack();");
    }

    @Test
    public void navigateForward_emitsNavigateForwardCall() {
        RecordedStep fwd = new RecordedStep("f1", "navigateForward", null, null, null, List.of());
        List<String> stmts = PomCodeEmitter.build(List.of(fwd), "P").statements();
        Assert.assertEquals(stmts.size(), 1);
        Assert.assertEquals(stmts.get(0), "driverActions.navigation().navigateForward();");
    }

    @Test
    public void generatorMethod_emitsTestDataGeneratorCall_notJsonHelper() {
        LocatorCandidate c = new LocatorCandidate(By.id("email"), "By.id(\"email\")", 0.9, "id", true, false);
        RecordedStep s = new RecordedStep("s1", "input", "test@example.com", "input", "Email", List.of(c));
        s.setGeneratorMethod("getRandomEmail");
        PomCodeEmitter.EmitResult r = PomCodeEmitter.build(List.of(s), "P");
        String stmts = String.join("\n", r.statements());
        Assert.assertTrue(stmts.contains("TestDataGenerator.getRandomEmail()"), stmts);
        Assert.assertFalse(stmts.contains("JsonHelper"), "classified field must not use JsonHelper: " + stmts);
        Assert.assertTrue(r.hasTestDataGen());
        Assert.assertFalse(r.testData().containsKey("email"), "classified field must not go to JSON");
    }

    @Test
    public void generatorMethod_null_fallsBackToJsonHelper() {
        LocatorCandidate c = new LocatorCandidate(By.id("email"), "By.id(\"email\")", 0.9, "id", true, false);
        RecordedStep s = new RecordedStep("s1", "input", "test@example.com", "input", "Email", List.of(c));
        String stmts = String.join("\n", PomCodeEmitter.build(List.of(s), "P").statements());
        Assert.assertTrue(stmts.contains("JsonHelper.getJsonKeyValue"), stmts);
        Assert.assertFalse(stmts.contains("TestDataGenerator"), stmts);
    }

    @Test
    public void sameLocatorReusedInTwoSteps_fieldEmittedOnce() {
        LocatorCandidate c = new LocatorCandidate(By.id("btn"), "By.id(\"btn\")", 0.9, "id", true, false);
        List<RecordedStep> steps = List.of(
                step("click", null, "Btn", c),
                step("click", null, "Btn", c));
        List<String> fields = PomCodeEmitter.build(steps, "P").locatorFields();
        long count = fields.stream().filter(f -> f.contains("By.id(\"btn\")")).count();
        Assert.assertEquals(count, 1L, "same expression should produce exactly one field declaration");
    }

    @Test
    public void identifier_digitLeadingName_prefixedWithEl() throws Exception {
        java.lang.reflect.Method m = PomCodeEmitter.class.getDeclaredMethod(
                "identifier", String.class, String.class, String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, "123field", null, null);
        Assert.assertTrue(result.startsWith("el"), "digit-leading field must be prefixed with 'el': " + result);
    }

    @Test
    public void identifier_emptyName_fallsBackToElement() throws Exception {
        java.lang.reflect.Method m = PomCodeEmitter.class.getDeclaredMethod(
                "identifier", String.class, String.class, String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, null, null, null);
        Assert.assertFalse(result.isEmpty(), "identifier must never be empty");
    }

    @Test
    public void esc_escapesSpecialCharacters() throws Exception {
        java.lang.reflect.Method m = PomCodeEmitter.class.getDeclaredMethod("esc", String.class);
        m.setAccessible(true);
        Assert.assertEquals(m.invoke(null, "a\\b"),  "a\\\\b");
        Assert.assertEquals(m.invoke(null, "a\"b"), "a\\\"b");
        Assert.assertEquals(m.invoke(null, "a\nb"), "a\\nb");
        Assert.assertEquals(m.invoke(null, "a\rb"), "ab");
        Assert.assertEquals(m.invoke(null, (Object) null), "");
    }

    @Test
    public void templateDynamic_quotedDigit_templated() throws Exception {
        java.lang.reflect.Method m = PomCodeEmitter.class.getDeclaredMethod("templateDynamic", String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, "[data-choice-index='0']");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("%s"), result);
    }

    @Test
    public void templateDynamic_trailingUnderscore_templated() throws Exception {
        java.lang.reflect.Method m = PomCodeEmitter.class.getDeclaredMethod("templateDynamic", String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, "month_0");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("%s"), result);
    }

    @Test
    public void templateDynamic_plainDigit_templated() throws Exception {
        java.lang.reflect.Method m = PomCodeEmitter.class.getDeclaredMethod("templateDynamic", String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, "item42");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("%s"), result);
    }

    @Test
    public void templateDynamic_noDigit_returnsNull() throws Exception {
        java.lang.reflect.Method m = PomCodeEmitter.class.getDeclaredMethod("templateDynamic", String.class);
        m.setAccessible(true);
        Assert.assertNull(m.invoke(null, "loginBtn"));
    }

    @Test
    public void hasTestDataGen_falseWhenNoGeneratorMethod() {
        LocatorCandidate c = new LocatorCandidate(By.id("x"), "By.id(\"x\")", 0.9, "id", true, false);
        PomCodeEmitter.EmitResult r = PomCodeEmitter.build(List.of(step("click", null, "X", c)), "P");
        Assert.assertFalse(r.hasTestDataGen());
    }
}
