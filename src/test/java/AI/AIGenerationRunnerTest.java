package ai;

import Ellithium.Utilities.ai.EllithiumAIEngine;
import Ellithium.core.ai.provider.LLMProvider;
import Ellithium.core.ai.provider.LLMProviderFactory;
import org.testng.annotations.Test;

import java.io.File;

public class AIGenerationRunnerTest {

    @Test
    public void testLiveAIGeneration() {
        System.out.println("Starting AI Generation...");
        LLMProvider provider = LLMProviderFactory.createProvider();
        if (provider == null) {
            System.err.println("API Key is missing. Check ai-config.properties or environment variables.");
            return;
        }

        EllithiumAIEngine engine = new EllithiumAIEngine(provider);
        String testCasePath = new File("src/test/resources/testcases/manual-login-test.json").getAbsolutePath();
        engine.generateFrom(testCasePath);
        System.out.println("AI Generation Completed.");
    }
}
