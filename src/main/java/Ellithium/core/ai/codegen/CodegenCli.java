package Ellithium.core.ai.codegen;

import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.HeadlessMode;
import Ellithium.core.driver.LocalDriverType;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CodegenCli {

    private CodegenCli() {}

    private static final long SESSION_CAP_MS = 60L * 60L * 1000L;
    private static final long POLL_MS = 500L;

    public static void main(String[] args) {
        Map<String, String> flags = new HashMap<>();
        String url = parse(args, flags);
        if (url == null && flags.isEmpty()) {
            System.out.println("Usage: codegen [url] [--browser chrome|edge|firefox|safari] "
                    + "[--target test|pom] [--output <dir>] [--package <pkg>] [--headless] [--llm-polish]");
            System.out.println("(url is optional — omit it and type a URL in the browser after recording starts)");
            return;
        }

        AIConfigLoader.initialize();

        LocalDriverType browser = browserOf(flags.getOrDefault("browser", "chrome"));
        HeadlessMode headless = flags.containsKey("headless") ? HeadlessMode.True : HeadlessMode.False;
        RecorderOptions opts = new RecorderOptions(
                flags.getOrDefault("output", "src/test/java"),
                flags.getOrDefault("package", "Pages"),
                browser.name(),
                flags.getOrDefault("target", "test"),
                flags.containsKey("llm-polish"),
                false);

        System.out.println("=== Ellithium Codegen — " + (url != null ? "recording " + url : "open a URL in the browser")
                + " in " + browser.getName() + " ===");
        System.out.println("Interact with the page; click 'Stop' in the overlay to generate code.");

        WebDriver driver = DriverFactory.getNewLocalDriver(browser, headless);
        try {
            driver.get(url != null ? url : "about:blank");
            InteractionRecorder.start(driver, opts, url);

            long deadline = System.currentTimeMillis() + SESSION_CAP_MS;
            while (InteractionRecorder.isRecording() && System.currentTimeMillis() < deadline) {
                try { Thread.sleep(POLL_MS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }

            List<RecordedStep> steps = InteractionRecorder.stop();
            String startUrl = InteractionRecorder.getStartUrl();
            if (steps.isEmpty()) {
                System.out.println("No interactions recorded — nothing to generate.");
                return;
            }
            String className = deriveClassName(url != null ? url : startUrl);
            String path = opts.isTest()
                    ? PomCodeEmitter.emitTest(steps, className, opts, startUrl)
                    : PomCodeEmitter.emit(steps, className, opts);
            System.out.println(path != null
                    ? "Generated " + (opts.isTest() ? "test" : "POM") + ": " + path + " (" + steps.size() + " steps)"
                    : "Code generation failed — see logs.");
        } finally {
            try { DriverFactory.quitDriver(); } catch (Exception ignored) {}
        }
    }

    private static String parse(String[] args, Map<String, String> flags) {
        String url = null;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("codegen")) continue;
            if (a.startsWith("--")) {
                String key = a.substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    flags.put(key, args[++i]);
                } else {
                    flags.put(key, "true");
                }
            } else if (url == null) {
                url = a;
            }
        }
        return url;
    }

    private static LocalDriverType browserOf(String name) {
        return switch (name.trim().toLowerCase()) {
            case "edge" -> LocalDriverType.Edge;
            case "firefox" -> LocalDriverType.FireFox;
            case "safari" -> LocalDriverType.Safari;
            default -> LocalDriverType.Chrome;
        };
    }

    static String deriveClassName(String url) {
        try {
            String path = new java.net.URI(url).getPath();
            String seg = (path == null || path.isBlank() || path.equals("/"))
                    ? new java.net.URI(url).getHost() : path;
            String[] parts = seg.replaceAll("[^a-zA-Z0-9]+", " ").trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (p.isEmpty()) continue;
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase());
            }
            String name = sb.toString();
            if (name.isEmpty()) name = "Recorded";
            if (Character.isDigit(name.charAt(0))) name = "Page" + name;
            return name + "Page";
        } catch (Exception e) {
            return "RecordedPage";
        }
    }
}
