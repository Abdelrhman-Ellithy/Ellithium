package Ellithium.core.ai.codegen;

/**
 * Configuration for the codegen recorder session.
 *
 * @param outputBasePath  Base path for generated source files (default: {@code "src/test/java"})
 * @param packageName     Java package for the generated class (default: {@code "Pages"})
 * @param browser         Browser name passed to DriverFactory (default: {@code "Chrome"})
 * @param target          Output target: {@code "test"} (TestNG test class) or {@code "pom"} (POM only)
 * @param assertMode      Assertion style for generated asserts: {@code "soft"} (default) or {@code "hard"}
 * @param llmPolish       Reserved for a future LLM polish pass — currently unused (emission is deterministic)
 * @param pickModeDefault Whether the overlay starts in "pick element" mode by default
 */
public record RecorderOptions(String outputBasePath, String packageName, String browser,
                              String target, String assertMode, boolean llmPolish, boolean pickModeDefault) {

    public static RecorderOptions defaults() {
        return new RecorderOptions("src/test/java", "Pages", "Chrome", "test", "soft", false, false);
    }

    public boolean isTest() {
        return !"pom".equalsIgnoreCase(target);
    }

    public boolean isSoftAssert() {
        return !"hard".equalsIgnoreCase(assertMode);
    }

    public RecorderOptions withAssertMode(String mode) {
        return new RecorderOptions(outputBasePath, packageName, browser, target, mode, llmPolish, pickModeDefault);
    }
}
