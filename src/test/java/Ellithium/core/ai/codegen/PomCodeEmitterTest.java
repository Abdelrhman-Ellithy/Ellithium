package Ellithium.core.ai.codegen;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

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

    @Test
    public void emitsAssertionsViaFullyQualifiedAssertionExecutor() {
        LocatorCandidate c = new LocatorCandidate(By.id("flash"), "By.id(\"flash\")", 0.9, "id", true, false);
        List<RecordedStep> steps = List.of(
                step("assertVisible", null, "Flash", c),
                step("assertText", "Logged in", "Flash", c),
                step("assertValue", "john@x.com", "Email", c));
        String stmts = String.join("\n", PomCodeEmitter.build(steps, "P").statements());

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
        Assert.assertTrue(src.contains("driverActions.elements().sendData(username, \"tomsmith\");"), src);
        Assert.assertTrue(src.contains("DriverFactory.quitDriver();"), src);
        Assert.assertFalse(src.contains("return this;"), "a test method must not return this");
    }

    @Test
    public void unmappedAction_isSkippedNotEmittedInvalid() {
        LocatorCandidate c = new LocatorCandidate(By.id("x"), "By.id(\"x\")", 0.9, "id", true, false);
        PomCodeEmitter.EmitResult r = PomCodeEmitter.build(
                List.of(step("teleport", null, "X", c)), "P");
        Assert.assertTrue(r.statements().isEmpty(), "no API mapping → no statement emitted");
    }
}
