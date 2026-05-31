package Ellithium.core.ai.codegen;

import Ellithium.core.ai.generators.PomClassGenerator;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PomCodeEmitter {

    private PomCodeEmitter() {}

    private static final Pattern BY_EXPR = Pattern.compile("^By\\.(\\w+)\\(\"(.*)\"\\)$");

    public record EmitResult(String className, List<String> locatorFields,
                             List<String> methods, List<String> statements) {}

    public static EmitResult build(List<RecordedStep> steps, String className) {
        return build(steps, className, true);
    }

    public static EmitResult build(List<RecordedStep> steps, String className, boolean parameterize) {
        Set<String> usedFieldNames = new LinkedHashSet<>();
        java.util.Map<String, String> exprToField = new java.util.HashMap<>();
        List<String> locatorFields = new ArrayList<>();
        List<String> statements = new ArrayList<>();
        List<String> params = new ArrayList<>();

        for (RecordedStep step : steps) {
            String action = step.getActionType();
            if ("navigate".equals(action)) {
                statements.add("driverActions.navigation().navigateToUrl(\"" + esc(step.getData()) + "\");");
                continue;
            }
            LocatorCandidate chosen = step.chosen();
            if (chosen == null) continue;

            String byRef;
            if (parameterize && chosen.parameterizable()) {
                String param = "value" + params.size();
                String formatted = parameterize(chosen.javaExpression(), param);
                if (formatted != null) {
                    byRef = formatted;
                    params.add(param);
                } else {
                    byRef = fieldFor(step, chosen, usedFieldNames, locatorFields, exprToField);
                }
            } else {
                byRef = fieldFor(step, chosen, usedFieldNames, locatorFields, exprToField);
            }

            String stmt = statementFor(action, byRef, step.getData());
            if (stmt == null) {
                Reporter.log("PomCodeEmitter: no API mapping for action '" + action + "' — step skipped",
                        LogLevel.WARN);
                continue;
            }
            List<Integer> frames = step.getFrameChain();
            for (Integer idx : frames) {
                statements.add("driverActions.frames().switchToFrameByIndex(" + idx + ");");
            }
            statements.add(stmt);
            if (!frames.isEmpty()) statements.add("driverActions.frames().switchToDefaultContent();");
        }

        String name = (className != null && !className.isBlank()) ? className : "RecordedPage";
        String signature = "public " + name + " perform(" + paramList(params) + ")";
        StringBuilder body = new StringBuilder(signature).append(" {\n");
        for (String s : statements) body.append("        ").append(s).append("\n");
        body.append("        return this;\n    }");

        return new EmitResult(name, locatorFields, List.of(body.toString()), statements);
    }

    public static String previewSource(List<RecordedStep> steps, String className, String pkg) {
        EmitResult r = build(steps, className);
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import Ellithium.Utilities.interactions.DriverActions;\n");
        sb.append("import org.openqa.selenium.By;\n");
        sb.append("import org.openqa.selenium.WebDriver;\n\n");
        sb.append("public class ").append(r.className()).append(" {\n\n");
        sb.append("    private final DriverActions<?> driverActions;\n\n");
        for (String f : r.locatorFields()) sb.append("    ").append(f).append("\n");
        sb.append("\n    public ").append(r.className()).append("(WebDriver driver) {\n");
        sb.append("        this.driverActions = new DriverActions<>(driver);\n    }\n\n");
        for (String m : r.methods()) sb.append("    ").append(m).append("\n\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static String previewTestSource(List<RecordedStep> steps, String className, String pkg,
                                           String startUrl, String browser) {
        EmitResult r = build(steps, className, false);
        String cls = r.className().endsWith("Test") ? r.className() : r.className() + "Test";
        String br = (browser == null || browser.isBlank()) ? "Chrome" : browser;
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import Ellithium.Utilities.interactions.DriverActions;\n");
        sb.append("import Ellithium.core.driver.DriverFactory;\n");
        sb.append("import Ellithium.core.driver.HeadlessMode;\n");
        sb.append("import Ellithium.core.driver.LocalDriverConfig;\n");
        sb.append("import Ellithium.core.driver.LocalDriverType;\n");
        sb.append("import Ellithium.core.driver.PageLoadStrategyMode;\n");
        sb.append("import Ellithium.core.driver.PrivateMode;\n");
        sb.append("import Ellithium.core.driver.SandboxMode;\n");
        sb.append("import Ellithium.core.driver.WebSecurityMode;\n");
        sb.append("import org.openqa.selenium.By;\n");
        sb.append("import org.openqa.selenium.WebDriver;\n");
        sb.append("import org.testng.annotations.AfterClass;\n");
        sb.append("import org.testng.annotations.BeforeClass;\n");
        sb.append("import org.testng.annotations.Test;\n\n");
        sb.append("public class ").append(cls).append(" {\n\n");
        sb.append("    private DriverActions<?> driverActions;\n");
        sb.append("    private WebDriver driver;\n\n");
        for (String f : r.locatorFields()) sb.append("    ").append(f).append("\n");
        sb.append("\n    @BeforeClass\n    public void setUp() {\n");
        sb.append("        LocalDriverConfig driverConfig = new LocalDriverConfig(LocalDriverType.").append(br).append(",\n");
        sb.append("                HeadlessMode.False, PrivateMode.True, PageLoadStrategyMode.Normal,\n");
        sb.append("                WebSecurityMode.SecureMode, SandboxMode.Sandbox);\n");
        sb.append("        driver = DriverFactory.getNewDriver(driverConfig);\n");
        sb.append("        this.driverActions = new DriverActions<>(driver);\n");
        if (startUrl != null && !startUrl.isBlank()) {
            sb.append("        driverActions.navigation().navigateToUrl(\"").append(esc(startUrl)).append("\");\n");
        }
        sb.append("    }\n\n");
        sb.append("    @Test\n    public void perform() {\n");
        for (String s : r.statements()) sb.append("        ").append(s).append("\n");
        sb.append("    }\n\n");
        sb.append("    @AfterClass\n    public void tearDown() {\n        DriverFactory.quitDriver();\n    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static String emitTest(List<RecordedStep> steps, String className, RecorderOptions opts, String startUrl) {
        RecorderOptions o = opts != null ? opts : RecorderOptions.defaults();
        String src = previewTestSource(steps, className, o.packageName(), startUrl, o.browser());
        String cls = (className == null || className.isBlank() ? "RecordedPage" : className);
        if (!cls.endsWith("Test")) cls = cls + "Test";
        String path = o.outputBasePath() + "/" + o.packageName().replace('.', '/') + "/" + cls + ".java";
        try {
            java.nio.file.Path p = java.nio.file.Paths.get(path);
            java.nio.file.Files.createDirectories(p.getParent());
            java.nio.file.Files.writeString(p, src);
            Reporter.log("PomCodeEmitter: wrote test " + path, LogLevel.INFO_GREEN);
            return path;
        } catch (Exception e) {
            Reporter.log("PomCodeEmitter: failed to write test: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    public static String emit(List<RecordedStep> steps, String className, RecorderOptions opts) {
        RecorderOptions o = opts != null ? opts : RecorderOptions.defaults();
        EmitResult result = build(steps, className);
        String path = o.outputBasePath() + "/" + o.packageName().replace('.', '/')
                + "/" + result.className() + ".java";
        boolean ok = PomClassGenerator.createPomClass(path, o.packageName(), result.className(),
                result.locatorFields(), result.methods());
        if (ok) Reporter.log("PomCodeEmitter: wrote " + path, LogLevel.INFO_GREEN);
        return ok ? path : null;
    }

    private static final String ASSERT = "Ellithium.Utilities.assertion.AssertionExecutor.hard";

    private static String statementFor(String action, String byRef, String data) {
        return switch (action) {
            case "click" -> "driverActions.elements().clickOnElement(" + byRef + ");";
            case "input", "sendData" -> "driverActions.elements().sendData(" + byRef + ", \"" + esc(data) + "\");";
            case "clear" -> "driverActions.elements().clearElement(" + byRef + ");";
            case "getText" -> "driverActions.elements().getText(" + byRef + ");";
            case "select", "selectByText" -> "driverActions.select().selectDropdownByText(" + byRef + ", \"" + esc(data) + "\");";
            case "hover" -> "driverActions.mouse().hoverOverElement(" + byRef + ");";
            case "doubleClick" -> "driverActions.mouse().doubleClick(" + byRef + ");";
            case "assertVisible" -> ASSERT + ".assertTrue(driverActions.elements().isElementDisplayed("
                    + byRef + "), \"element should be visible\");";
            case "assertText" -> ASSERT + ".assertContains(driverActions.elements().getText("
                    + byRef + "), \"" + esc(data) + "\");";
            case "assertValue" -> ASSERT + ".assertEquals(driverActions.elements().getAttributeValue("
                    + byRef + ", \"value\"), \"" + esc(data) + "\");";
            default -> null;
        };
    }

    private static String fieldFor(RecordedStep step, LocatorCandidate chosen, Set<String> used,
                                   List<String> locatorFields, java.util.Map<String, String> exprToField) {
        String existing = exprToField.get(chosen.javaExpression());
        if (existing != null) return existing;
        String base = identifier(step.getElementName(), step.getTagName(), step.getActionType());
        String fieldName = base;
        int n = 2;
        while (!used.add(fieldName)) fieldName = base + n++;
        locatorFields.add("private final By " + fieldName + " = " + chosen.javaExpression() + ";");
        exprToField.put(chosen.javaExpression(), fieldName);
        return fieldName;
    }

    private static String parameterize(String javaExpression, String param) {
        Matcher m = BY_EXPR.matcher(javaExpression);
        if (!m.matches()) return null;
        String method = m.group(1);
        String content = m.group(2);
        String templated = templateDynamic(content);
        if (templated == null) return null;
        return "By." + method + "(String.format(\"" + templated + "\", " + param + "))";
    }

    private static String templateDynamic(String content) {
        Matcher quoted = Pattern.compile("'(\\d+)'").matcher(content);
        if (quoted.find()) return quoted.replaceFirst("'%s'");
        Matcher trailing = Pattern.compile("_(\\d+)").matcher(content);
        if (trailing.find()) return trailing.replaceFirst("_%s");
        Matcher digits = Pattern.compile("(\\d+)").matcher(content);
        if (digits.find()) return digits.replaceFirst("%s");
        return null;
    }

    private static String paramList(List<String> params) {
        List<String> typed = new ArrayList<>(params.size());
        for (String p : params) typed.add("String " + p);
        return String.join(", ", typed);
    }

    private static String identifier(String elementName, String tag, String action) {
        String src = (elementName != null && !elementName.isBlank()) ? elementName
                : ((tag != null ? tag : "el") + " " + (action != null ? action : ""));
        String[] words = src.trim().toLowerCase().split("[^a-z0-9]+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() == 0) sb.append(w);
            else sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        String id = sb.toString();
        if (id.isEmpty()) id = "element";
        if (Character.isDigit(id.charAt(0))) id = "el" + id;
        return id;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
