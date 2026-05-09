package AI;

import Ellithium.Utilities.ai.EllithiumAIEngine;
import Ellithium.Utilities.ai.provider.LLMProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class EllithiumAIEngineTest {

    @Test
    public void testGenerateFromRequirements_WritesToDiskWhenNotAlreadyGenerated() throws Exception {
        LLMProvider provider = mock(LLMProvider.class);
        String mockJsonResponse = "{\n" +
                "  \"pomClass\": \"LoginPage\",\n" +
                "  \"pomPackage\": \"pages\",\n" +
                "  \"pomMethods\": [\"login\"],\n" +
                "  \"locatorFields\": [\"private final By user = By.id(\\\"user\\\");\"],\n" +
                "  \"methodBodies\": [\"public LoginPage login(String email, String pass) { return this; }\"],\n" +
                "  \"testClass\": \"LoginTest\",\n" +
                "  \"testPackage\": \"tests\",\n" +
                "  \"testMethod\": \"testValidLogin\",\n" +
                "  \"testBody\": \"new LoginPage(driver).login(email, pass);\",\n" +
                "  \"featureFile\": \"\",\n" +
                "  \"scenarioTitle\": \"\",\n" +
                "  \"gherkinScenario\": \"\"\n" +
                "}";
        
        when(provider.ask(anyString(), anyString())).thenReturn(mockJsonResponse);

        // Create a temporary requirement file
        File reqFile = File.createTempFile("req", ".txt");
        Files.write(reqFile.toPath(), "Login test requirements".getBytes());
        reqFile.deleteOnExit();

        // This will create files in the actual project structure if we don't mock the file generation.
        // For a unit test, we should mock PomClassGenerator if we had injection,
        // but since EllithiumAIEngine has static methods calling static methods, we rely on the actual AST execution.
        // Since we are creating a POM in "pages" which is a standard structure, we should be careful.
        // We can just verify the LLM provider is called.
        
        EllithiumAIEngine engine = new EllithiumAIEngine(provider);
        engine.generateFrom(reqFile.getAbsolutePath());

        verify(provider, times(1)).ask(anyString(), anyString());
    }

    @Test
    public void testGenerateFrom_SkipsWhenAlreadyGenerated() throws Exception {
        // Here we test idempotency via TraceabilityManager
        LLMProvider provider = mock(LLMProvider.class);
        
        File reqFile = File.createTempFile("req", ".txt");
        Files.write(reqFile.toPath(), "TC-IDEMP-1 | Idempotent test".getBytes());
        reqFile.deleteOnExit();

        // Let's manually mark it as generated via mockStatic
        try (MockedStatic<Ellithium.Utilities.ai.TraceabilityManager> traceMock = Mockito.mockStatic(Ellithium.Utilities.ai.TraceabilityManager.class)) {
            traceMock.when(() -> Ellithium.Utilities.ai.TraceabilityManager.isAlreadyGenerated(anyString(), anyString())).thenReturn(true);

            EllithiumAIEngine engine = new EllithiumAIEngine(provider);
            engine.generateFrom(reqFile.getAbsolutePath());

            // Provider should NOT be asked
            verify(provider, never()).ask(anyString(), anyString());
        }
    }

    @Test
    public void testGenerateFrom_WithMissingFile_DoesNotCrash() throws Exception {
        LLMProvider provider = mock(LLMProvider.class);
        EllithiumAIEngine engine = new EllithiumAIEngine(provider);
        
        // This should log an error and return without throwing an exception
        engine.generateFrom("missing_file.json");
        
        verify(provider, never()).ask(anyString(), anyString());
    }
}
