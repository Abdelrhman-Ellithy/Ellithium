package Ellithium.core.ai.codegen;

public record RecorderOptions(String outputBasePath, String packageName, String browser,
                              String target, boolean llmPolish, boolean pickModeDefault) {

    public static RecorderOptions defaults() {
        return new RecorderOptions("src/test/java", "Pages", "Chrome", "test", false, false);
    }

    public boolean isTest() {
        return !"pom".equalsIgnoreCase(target);
    }
}
