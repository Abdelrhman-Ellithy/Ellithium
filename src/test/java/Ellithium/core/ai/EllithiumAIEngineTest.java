package Ellithium.core.ai;

import Ellithium.Utilities.ai.EllithiumAIEngine;
import Ellithium.Utilities.ai.LLMProvider;
import Ellithium.core.ai.models.TraceabilityRecord;
import Ellithium.core.ai.models.TestCaseSource;
import Ellithium.core.ai.readers.TextTestCaseReader;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

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
        File outputRoot = Files.createTempDirectory("ai-gen-out").toFile();
        outputRoot.deleteOnExit();

        EllithiumAIEngine engine = new EllithiumAIEngine(
                provider,
                outputRoot.getAbsolutePath(),
                outputRoot.getAbsolutePath(),
                false
        );
        try (MockedStatic<TraceabilityManager> traceMock = Mockito.mockStatic(TraceabilityManager.class)) {
            traceMock.when(() -> TraceabilityManager.isAlreadyGenerated(anyString(), anyString())).thenReturn(false);
            engine.generateFrom(reqFile.getAbsolutePath());
            traceMock.verify(() -> TraceabilityManager.saveRecord(any(TraceabilityRecord.class)), times(1));
        }

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
        try (MockedStatic<TraceabilityManager> traceMock = Mockito.mockStatic(TraceabilityManager.class)) {
            traceMock.when(() -> TraceabilityManager.isAlreadyGenerated(anyString(), anyString())).thenReturn(true);

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

    @Test
    public void testGenerateFrom_WhenLlmResponseMissingRequiredFields_SkipsGeneration() throws Exception {
        LLMProvider provider = mock(LLMProvider.class);
        when(provider.ask(anyString(), anyString())).thenReturn("{\"pomClass\":\"LoginPage\"}");

        File reqFile = File.createTempFile("req", ".txt");
        Files.write(reqFile.toPath(), "ID: TC-INVALID\nDescription: validate missing response fields".getBytes());
        reqFile.deleteOnExit();

        EllithiumAIEngine engine = new EllithiumAIEngine(provider);
        engine.generateFrom(reqFile.getAbsolutePath());

        verify(provider, times(1)).ask(anyString(), anyString());
    }

    @Test
    public void testTextReader_ParsesSingleMultilineCaseWithoutSplittingOnBlankLines() throws Exception {
        String content = "ID: TC_checkSelected_AI\r\n"
                + "Title: Verify Dropdown Option Selection\r\n"
                + "Description: Select option 1 and validate selection.\r\n\r\n"
                + "Preconditions:\r\n"
                + "1. User is on Dropdown page.\r\n\r\n"
                + "Steps:\r\n"
                + "1. Locate dropdown.\r\n"
                + "2. Select Option 1.\r\n\r\n"
                + "Expected Results:\r\n"
                + "1. Selected option is Option 1.\r\n";

        File reqFile = File.createTempFile("req-reader", ".txt");
        Files.write(reqFile.toPath(), content.getBytes());
        reqFile.deleteOnExit();

        TextTestCaseReader reader = new TextTestCaseReader();
        List<TestCaseSource> cases = reader.read(reqFile.getAbsolutePath());

        Assert.assertEquals(cases.size(), 1, "Reader should keep one testcase for one ID block");
        Assert.assertEquals(cases.get(0).getTestId(), "TC_checkSelected_AI");
        Assert.assertTrue(cases.get(0).getDescription().contains("Expected Results"));
    }
}
