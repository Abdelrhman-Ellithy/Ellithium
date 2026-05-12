import Ellithium.Utilities.ai.EllithiumAIEngine;
import Ellithium.Utilities.ai.config.AIConfigLoader;
import Ellithium.Utilities.ai.provider.LLMProvider;
import Ellithium.Utilities.ai.provider.LLMProviderFactory;

public class RunAIGen {
    public static void main(String[] args) {
        System.out.println("Starting AI Generation...");
        AIConfigLoader.initialize();
        LLMProvider provider = LLMProviderFactory.createProvider();
        if (provider == null) {
            System.out.println("AI provider is not configured. Check ai-config.properties/API key.");
            return;
        }
        EllithiumAIEngine engine = new EllithiumAIEngine(provider, "src/test/java", true);
        engine.generateFrom("src/test/resources/testcases/manual_dropdown_test.txt");
        System.out.println("AI Generation Complete!");
    }
}
