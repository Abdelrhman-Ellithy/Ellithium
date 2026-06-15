package Ellithium.core.ai;

import Ellithium.Utilities.ai.EllithiumAIEngine;
import Ellithium.Utilities.ai.LLMProvider;
import Ellithium.Utilities.ai.LLMProviderFactory;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class AIGenerationRunnerTest {

    private static final String TEMP_OUT = "target/ai-gen-test-output";
    private static final String TEMP_TEST_OUT = "target/ai-gen-test-output-tests";

    @Test
    public void testLiveAIGeneration() {
        System.out.println("Starting AI Generation...");
        LLMProvider provider = LLMProviderFactory.createProvider();
        if (provider == null) {
            System.err.println("API Key is missing. Check ai-config.properties or environment variables.");
            return;
        }

        EllithiumAIEngine engine = new EllithiumAIEngine(provider, TEMP_OUT, TEMP_TEST_OUT, true);
        String testCasePath = new File("src/test/resources/testcases/manual-login-test.json").getAbsolutePath();
        engine.generateFrom(testCasePath);
        System.out.println("AI Generation Completed.");
    }

    @AfterClass
    public void cleanup() {
        deleteDir(Path.of(TEMP_OUT));
        deleteDir(Path.of(TEMP_TEST_OUT));
    }

    private static void deleteDir(Path dir) {
        if (!Files.exists(dir)) return;
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ignored) {}
    }
}
