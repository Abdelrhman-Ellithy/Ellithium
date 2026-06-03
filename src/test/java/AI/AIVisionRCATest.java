package ai;

import Ellithium.core.ai.vision.AIVisionRCA;
import Ellithium.Utilities.ai.LLMProvider;
import Ellithium.core.ai.config.AIConfigLoader;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AIVisionRCATest {

    @Test
    public void testAnalyze_WithVisionEnabled_CallsProvider() throws Exception {
        try (MockedStatic<AIConfigLoader> configMock = Mockito.mockStatic(AIConfigLoader.class)) {
            configMock.when(AIConfigLoader::isVisionRcaEnabled).thenReturn(true);

            LLMProvider provider = mock(LLMProvider.class);
            when(provider.supportsVision()).thenReturn(true);
            when(provider.askWithVision(anyString(), any(byte[].class))).thenReturn("Cookie banner is blocking the element.");

            // Create a dummy temp file to act as the screenshot
            File dummyScreenshot = File.createTempFile("test-screenshot", ".png");
            Files.write(dummyScreenshot.toPath(), new byte[]{1, 2, 3});
            dummyScreenshot.deleteOnExit();

            AIVisionRCA.analyze(dummyScreenshot, "ElementNotInteractableException", provider);

            verify(provider, times(1)).askWithVision(anyString(), any(byte[].class));
        }
    }

    @Test
    public void testAnalyze_WithVisionDisabled_DoesNotCallProvider() throws Exception {
        try (MockedStatic<AIConfigLoader> configMock = Mockito.mockStatic(AIConfigLoader.class)) {
            configMock.when(AIConfigLoader::isVisionRcaEnabled).thenReturn(false);

            LLMProvider provider = mock(LLMProvider.class);
            File dummyScreenshot = File.createTempFile("test-screenshot", ".png");
            dummyScreenshot.deleteOnExit();

            verify(provider, never()).askWithVision(anyString(), any(byte[].class));
        }
    }

    @Test
    public void testAnalyze_WithNullOrMissingScreenshot_DoesNotCallProvider() throws Exception {
        try (MockedStatic<AIConfigLoader> configMock = Mockito.mockStatic(AIConfigLoader.class)) {
            configMock.when(AIConfigLoader::isVisionRcaEnabled).thenReturn(true);

            LLMProvider provider = mock(LLMProvider.class);
            when(provider.supportsVision()).thenReturn(true);

            // Null screenshot
            AIVisionRCA.analyze(null, "Error", provider);
            
            // Missing screenshot
            AIVisionRCA.analyze(new File("does_not_exist.png"), "Error", provider);

            verify(provider, never()).askWithVision(anyString(), any(byte[].class));
        }
    }

    @Test
    public void testAnalyze_WithUnsupportedProvider_DoesNotCallProvider() throws Exception {
        try (MockedStatic<AIConfigLoader> configMock = Mockito.mockStatic(AIConfigLoader.class)) {
            configMock.when(AIConfigLoader::isVisionRcaEnabled).thenReturn(true);

            LLMProvider provider = mock(LLMProvider.class);
            when(provider.supportsVision()).thenReturn(false);

            File dummyScreenshot = File.createTempFile("test-screenshot", ".png");
            Files.write(dummyScreenshot.toPath(), new byte[]{1, 2, 3});
            dummyScreenshot.deleteOnExit();

            AIVisionRCA.analyze(dummyScreenshot, "Error", provider);

            verify(provider, never()).askWithVision(anyString(), any(byte[].class));
        }
    }

    @Test
    public void testAnalyze_WithProviderException_HandlesGracefully() throws Exception {
        try (MockedStatic<AIConfigLoader> configMock = Mockito.mockStatic(AIConfigLoader.class)) {
            configMock.when(AIConfigLoader::isVisionRcaEnabled).thenReturn(true);

            LLMProvider provider = mock(LLMProvider.class);
            when(provider.supportsVision()).thenReturn(true);
            when(provider.askWithVision(anyString(), any(byte[].class))).thenThrow(new RuntimeException("API Down"));

            File dummyScreenshot = File.createTempFile("test-screenshot", ".png");
            Files.write(dummyScreenshot.toPath(), new byte[]{1, 2, 3});
            dummyScreenshot.deleteOnExit();

            // Should not throw an unhandled exception
            AIVisionRCA.analyze(dummyScreenshot, "Error", provider);

            verify(provider, times(1)).askWithVision(anyString(), any(byte[].class));
        }
    }
}
