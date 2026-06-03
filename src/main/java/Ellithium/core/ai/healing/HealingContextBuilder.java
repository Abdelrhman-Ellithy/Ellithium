package Ellithium.core.ai.healing;

import Ellithium.Utilities.ai.LLMProvider;
import Ellithium.Utilities.ai.HealingStrategy;
import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.ai.sanitizers.DOMMinimizer;
import Ellithium.core.ai.sanitizers.DataScrubber;
import Ellithium.core.ai.scoring.SemanticQueryBuilder;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HealingContextBuilder {

    static final ExecutorService TIER4_PREP_POOL =
            Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "ellithium-tier4-prep");
                t.setDaemon(true);
                return t;
            });

    static class HealingContext {
        String brokenLocatorStr;
        String byMethod;
        String byValue;
        String pageClassName;
        String methodName;
        String actionType;
        String callSiteSource;
        String minimizedDom;
        String filePath;
        String fieldName;
        int lineNumber;
        boolean isMobile;
        byte[] screenshot;
        ElementFingerprint baseline;
        String semanticQuery;
    }

    static class SourceLocation {
        final String filePath;
        final String fieldName;
        final String className;
        final String methodName;
        final int lineNumber;

        SourceLocation(String filePath, String fieldName, String className, String methodName, int lineNumber) {
            this.filePath = filePath;
            this.fieldName = fieldName;
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
        }
    }

    static HealingContext build(WebDriver driver, By brokenLocator, StackTraceElement[] stackTrace,
                                LLMProvider provider, HealingStrategy strategy) {
        HealingContext ctx = new HealingContext();
        ctx.brokenLocatorStr = brokenLocator.toString();
        ctx.isMobile = driver instanceof AppiumDriver;

        parseByLocator(brokenLocator.toString(), ctx);

        SourceLocation srcLoc = resolveSourceLocation(stackTrace);
        if (srcLoc != null) {
            ctx.pageClassName = srcLoc.className;
            ctx.methodName = srcLoc.methodName;
            ctx.filePath = srcLoc.filePath;
            ctx.fieldName = srcLoc.fieldName;
            ctx.lineNumber = srcLoc.lineNumber;
        }

        ctx.actionType = extractActionType(stackTrace);

        if (ctx.filePath != null && ctx.lineNumber > 0) {
            ctx.callSiteSource = readCallSiteSource(ctx.filePath, ctx.lineNumber);
        }

        ctx.baseline = BaselineStore.getBaseline(brokenLocator.toString());

        ctx.semanticQuery = SemanticQueryBuilder.buildFromContext(
                ctx.actionType, ctx.brokenLocatorStr, ctx.methodName, ctx.baseline);

        boolean visionAllowedHere = ctx.isMobile
                ? AIConfigLoader.isVisionAllowedOnMobile()
                : AIConfigLoader.isVisionAllowedOnWeb();
        boolean wantScreenshot = provider != null
                && provider.supportsVision()
                && strategy != HealingStrategy.SUGGEST_ONLY
                && visionAllowedHere
                && driver instanceof org.openqa.selenium.TakesScreenshot;
        if (provider != null && provider.supportsVision() && !visionAllowedHere) {
            String flag = ctx.isMobile ? "ai.vision.allowMobile" : "ai.vision.allowWeb";
            Reporter.log("AI Self-Healing: screenshot withheld from LLM (" + flag + "=false) "
                    + "— set it true to enable visual healing (PII consideration)", LogLevel.DEBUG);
        }

        java.util.concurrent.CompletableFuture<String> domF =
                java.util.concurrent.CompletableFuture.supplyAsync(
                        () -> DOMMinimizer.getOptimalDOMRepresentation(driver), TIER4_PREP_POOL);
        java.util.concurrent.CompletableFuture<byte[]> shotF = wantScreenshot
                ? java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try {
                            return ((org.openqa.selenium.TakesScreenshot) driver)
                                    .getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
                        } catch (Exception e) {
                            Reporter.log("Failed to capture screenshot for visual healing: "
                                    + e.getMessage(), LogLevel.WARN);
                            return null;
                        }
                    }, TIER4_PREP_POOL)
                : java.util.concurrent.CompletableFuture.completedFuture(null);

        try {
            ctx.minimizedDom = DataScrubber.scrub(domF.get());
        } catch (Exception e) {
            ctx.minimizedDom = "";
        }
        try {
            ctx.screenshot = shotF.get();
            if (ctx.screenshot != null) {
                Reporter.log("Screenshot captured for visual healing ("
                        + ctx.screenshot.length + " bytes)", LogLevel.INFO_BLUE);
            }
        } catch (Exception ignored) {}

        return ctx;
    }

    static void parseByLocator(String locatorStr, HealingContext ctx) {
        Matcher m = Pattern.compile("By\\.([a-zA-Z]+):\\s*(.*)").matcher(locatorStr);
        if (m.find()) {
            ctx.byMethod = m.group(1);
            ctx.byValue = m.group(2).trim();
        }
    }

    static String extractActionType(StackTraceElement[] stackTrace) {
        for (StackTraceElement frame : stackTrace) {
            String cls = frame.getClassName();
            if (cls.startsWith("Ellithium.Utilities.interactions.")) {
                String method = frame.getMethodName();
                if (!method.equals("findWebElement") && !method.equals("waitForVisibilityAndFindElement")
                        && !method.equals("getFluentWait") && !method.equals("findWebElements")
                        && !method.equals("waitForVisibilityAndFindElements")) {
                    return method;
                }
            }
        }
        return "unknown";
    }

    static String readCallSiteSource(String filePath, int lineNumber) {
        try {
            List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(filePath));
            int start = Math.max(0, lineNumber - 3);
            int end = Math.min(lines.size(), lineNumber + 2);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                sb.append(lines.get(i)).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    static SourceLocation resolveSourceLocation(StackTraceElement[] stackTrace) {
        for (StackTraceElement frame : stackTrace) {
            String className = frame.getClassName();
            if (className.startsWith("Ellithium.")
                    || className.startsWith("org.openqa.selenium")
                    || className.startsWith("java.")
                    || className.startsWith("sun.")
                    || className.startsWith("io.cucumber")
                    || className.startsWith("io.qameta")
                    || className.startsWith("org.testng")
                    || className.startsWith("net.bytebuddy")) {
                continue;
            }

            String classFilePart = className.replace('.', '/') + ".java";
            String resolvedPath = null;
            for (String root : new String[]{"src/test/java/", "src/main/java/"}) {
                String candidate = root + classFilePart;
                if (new java.io.File(candidate).exists()) {
                    resolvedPath = candidate;
                    break;
                }
            }
            int callSiteLine = frame.getLineNumber();

            if (resolvedPath == null) {
                return new SourceLocation(null, null, className, frame.getMethodName(), callSiteLine);
            }

            String fieldName = resolveFieldNameFromSource(resolvedPath, callSiteLine);

            Reporter.log("AI Healer: Located source at " + resolvedPath
                    + ":" + callSiteLine
                    + (fieldName != null ? " → field '" + fieldName + "'" : " (inline locator)"), LogLevel.INFO_BLUE);
            return new SourceLocation(resolvedPath, fieldName, className, frame.getMethodName(), callSiteLine);
        }
        return null;
    }

    static String resolveFieldNameFromSource(String filePath, int callSiteLine) {
        try {
            List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(filePath));
            if (callSiteLine < 1 || callSiteLine > lines.size()) return null;

            String callLine = lines.get(callSiteLine - 1).trim();

            if (callLine.matches(".*By\\.[a-zA-Z]+\\(.*") || callLine.matches(".*AppiumBy\\.[a-zA-Z]+\\(.*")) {
                java.util.regex.Matcher m = Pattern.compile("(?:this\\.)?([a-zA-Z_][a-zA-Z0-9_]*)")
                        .matcher(callLine);
                java.util.List<String> candidates = new java.util.ArrayList<>();
                while (m.find()) candidates.add(m.group(1));

                for (int i = callSiteLine - 2; i >= 0; i--) {
                    String srcLine = lines.get(i).trim();
                    java.util.regex.Matcher fieldMatcher = Pattern
                            .compile("(?:private|protected|public)?\\s*(?:final\\s+)?By\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:=|;)")
                            .matcher(srcLine);
                    if (fieldMatcher.find()) {
                        String foundField = fieldMatcher.group(1);
                        if (candidates.contains(foundField)) {
                            return foundField;
                        }
                    }
                }
                return null;
            }

            java.util.regex.Matcher m = Pattern.compile("(?:this\\.)?([a-zA-Z_][a-zA-Z0-9_]*)")
                    .matcher(callLine);
            java.util.List<String> candidates = new java.util.ArrayList<>();
            while (m.find()) candidates.add(m.group(1));

            for (int i = callSiteLine - 2; i >= 0; i--) {
                String srcLine = lines.get(i).trim();
                java.util.regex.Matcher fieldMatcher = Pattern
                        .compile("(?:private|protected|public)?\\s*(?:final\\s+)?By\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:=|;)")
                        .matcher(srcLine);
                if (fieldMatcher.find()) {
                    String foundField = fieldMatcher.group(1);
                    if (candidates.contains(foundField)) return foundField;
                }
            }
        } catch (Exception e) {
            Reporter.log("AI Healer: Could not read source for field resolution: " + e.getMessage(), LogLevel.WARN);
        }
        return null;
    }
}
