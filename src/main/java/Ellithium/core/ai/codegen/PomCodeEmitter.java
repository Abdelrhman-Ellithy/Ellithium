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
                             List<String> methods, List<String> statements, boolean hasAssertions) {}

    static final String ASSERT_HARD = "Ellithium.Utilities.assertion.AssertionExecutor.hard";
    static final String SOFT_TYPE = "Ellithium.Utilities.assertion.AssertionExecutor.soft";
    static final String SOFT_VAR = "softAssert";

    public static EmitResult build(List<RecordedStep> steps, String className) {
        return build(steps, className, true, true);
    }

    public static EmitResult build(List<RecordedStep> steps, String className, boolean parameterize) {
        return build(steps, className, parameterize, true);
    }

    public static EmitResult build(List<RecordedStep> steps, String className,
                                   boolean parameterize, boolean soft) {
        Set<String> usedFieldNames = new LinkedHashSet<>();
        java.util.Map<String, String> exprToField = new java.util.HashMap<>();
        List<String> locatorFields = new ArrayList<>();
        List<String> statements = new ArrayList<>();
        List<String> params = new ArrayList<>();
        String assertRef = soft ? SOFT_VAR : ASSERT_HARD;
        boolean hasAssertions = false;

        for (RecordedStep step : steps) {
            String action = step.getActionType();
            if ("navigate".equals(action)) {
                statements.add("driverActions.navigation().navigateToUrl(\"" + esc(step.getData()) + "\");");
                continue;
            }
            if ("navigateBack".equals(action)) {
                statements.add("driverActions.navigation().navigateBack();");
                continue;
            }
            if ("navigateForward".equals(action)) {
                statements.add("driverActions.navigation().navigateForward();");
                continue;
            }
            if ("assertText".equals(action) && (step.getData() == null || step.getData().isBlank())) {
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

            String stmt = statementFor(action, byRef, step.getData(), assertRef);
            if (stmt == null) {
                Reporter.log("PomCodeEmitter: no API mapping for action '" + action + "' — step skipped",
                        LogLevel.WARN);
                continue;
            }
            if (action.startsWith("assert")) hasAssertions = true;
            List<Integer> frames = step.getFrameChain();
            for (Integer idx : frames) {
                statements.add("driverActions.frames().switchToFrameByIndex(" + idx + ");");
            }
            statements.add(stmt);
            if (!frames.isEmpty()) statements.add("driverActions.frames().switchToDefaultContent();");
        }

        boolean softAll = soft && hasAssertions;
        String name = (className != null && !className.isBlank()) ? className : "RecordedPage";
        String signature = "public " + name + " perform(" + paramList(params) + ")";
        StringBuilder body = new StringBuilder(signature).append(" {\n");
        if (softAll) body.append("        ").append(SOFT_TYPE).append(" ").append(SOFT_VAR)
                .append(" = new ").append(SOFT_TYPE).append("();\n");
        for (String s : statements) body.append("        ").append(s).append("\n");
        if (softAll) body.append("        ").append(SOFT_VAR).append(".assertAll();\n");
        body.append("        return this;\n    }");

        return new EmitResult(name, locatorFields, List.of(body.toString()), statements, hasAssertions);
    }

    public static String previewSource(List<RecordedStep> steps, String className, String pkg) {
        return previewSource(steps, className, pkg, true);
    }

    public static String previewSource(List<RecordedStep> steps, String className, String pkg, boolean soft) {
        EmitResult r = build(steps, className, true, soft);
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
        return previewTestSource(steps, className, pkg, startUrl, browser, true);
    }

    public static String previewTestSource(List<RecordedStep> steps, String className, String pkg,
                                           String startUrl, String browser, boolean soft) {
        EmitResult r = build(steps, className, false, soft);
        boolean softAll = soft && r.hasAssertions();
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
        if (softAll) sb.append("        ").append(SOFT_TYPE).append(" ").append(SOFT_VAR)
                .append(" = new ").append(SOFT_TYPE).append("();\n");
        for (String s : r.statements()) sb.append("        ").append(s).append("\n");
        if (softAll) sb.append("        ").append(SOFT_VAR).append(".assertAll();\n");
        sb.append("    }\n\n");
        sb.append("    @AfterClass\n    public void tearDown() {\n        DriverFactory.quitDriver();\n    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static String emitTest(List<RecordedStep> steps, String className, RecorderOptions opts, String startUrl) {
        RecorderOptions o = opts != null ? opts : RecorderOptions.defaults();
        String src = previewTestSource(steps, className, o.packageName(), startUrl, o.browser(), o.isSoftAssert());
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
        EmitResult result = build(steps, className, true, o.isSoftAssert());
        String path = o.outputBasePath() + "/" + o.packageName().replace('.', '/')
                + "/" + result.className() + ".java";
        boolean ok = PomClassGenerator.createPomClass(path, o.packageName(), result.className(),
                result.locatorFields(), result.methods());
        if (ok) Reporter.log("PomCodeEmitter: wrote " + path, LogLevel.INFO_GREEN);
        return ok ? path : null;
    }

    private static final String SECRET = "__ELL_SECRET__";

    private static String statementFor(String action, String byRef, String data, String assertRef) {
        return switch (action) {
            case "click" -> "driverActions.elements().clickOnElement(" + byRef + ");";
            case "input", "sendData" -> SECRET.equals(data)
                    ? "driverActions.elements().sendData(" + byRef + ", System.getenv(\"ELLITHIUM_SECRET\"));"
                    : "driverActions.elements().sendData(" + byRef + ", \"" + esc(data) + "\");";
            case "pressEnter" -> "driverActions.elements().sendData(" + byRef + ", org.openqa.selenium.Keys.ENTER);";
            case "clear" -> "driverActions.elements().clearElement(" + byRef + ");";
            case "getText" -> "driverActions.elements().getText(" + byRef + ");";
            case "select", "selectByText" -> "driverActions.select().selectDropdownByText(" + byRef + ", \"" + esc(data) + "\");";
            case "hover" -> "driverActions.mouse().hoverOverElement(" + byRef + ");";
            case "doubleClick" -> "driverActions.mouse().doubleClick(" + byRef + ");";
            case "rightClick" -> "driverActions.mouse().rightClick(" + byRef + ");";
            case "uploadFile" -> "driverActions.elements().uploadFile(" + byRef + ", \"" + esc(data) + "\");";
            case "assertVisible" -> assertRef + ".assertTrue(driverActions.elements().isElementDisplayed("
                    + byRef + "), \"element should be visible\");";
            case "assertText" -> assertRef + ".assertContains(driverActions.elements().getText("
                    + byRef + "), \"" + esc(data) + "\");";
            case "assertValue" -> assertRef + ".assertEquals(driverActions.elements().getAttributeValue("
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
        if (RESERVED_NAMES.contains(id)) id = id + "Field";
        return id;
    }

    private static final Set<String> RESERVED_NAMES = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false",
            "null", "var", "yield", "record",
            "driver", "driverActions", "driverConfig");

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
