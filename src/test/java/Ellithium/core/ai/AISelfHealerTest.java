package AI;

import Ellithium.core.ai.healing.AISelfHealer;
import Ellithium.Utilities.ai.LLMProvider;
import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.Utilities.ai.HealingStrategy;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AISelfHealerTest {

    @Test
    public void testAttemptHeal_WithDisabledStrategy_ReturnsNull() {
        AISelfHealer.initializeForThread(null, HealingStrategy.DISABLED);
        try {
            WebDriver driver = mock(WebDriver.class);
            WebElement element = AISelfHealer.attemptHeal(driver, By.id("broken"), new StackTraceElement[0]);

            Assert.assertNull(element);
            verify(driver, never()).getPageSource();
        } finally {
            AISelfHealer.initializeForThread(null, null);
        }
    }

    @Test
    public void testAttemptHeal_WithValidLLMResponse_ReturnsWebElement() {
        try (MockedStatic<AIConfigLoader> configMock = Mockito.mockStatic(AIConfigLoader.class)) {
            configMock.when(AIConfigLoader::getHealingStrategy).thenReturn(HealingStrategy.HEAL_AND_CONTINUE);
            configMock.when(AIConfigLoader::getConfidenceThreshold).thenReturn(0.85);

            WebDriver driver = mock(WebDriver.class);
            when(driver.getPageSource()).thenReturn("<html><body><div id='newLogin'>Login</div></body></html>");

            WebElement mockElement = mock(WebElement.class);
            when(driver.findElement(By.id("newLogin"))).thenReturn(mockElement);

            LLMProvider provider = mock(LLMProvider.class);
            String mockJsonResponse = "{\"locator\": \"By.id(\\\"newLogin\\\")\", \"confidence\": 0.95, \"reasoning\": \"Id changed\"}";
            when(provider.ask(anyString(), anyString())).thenReturn(mockJsonResponse);

            AISelfHealer.initializeForThread(provider, HealingStrategy.HEAL_AND_CONTINUE);

            WebElement result = AISelfHealer.attemptHeal(driver, By.id("oldLogin"), new StackTraceElement[0]);
            
            Assert.assertNotNull(result);
            Assert.assertEquals(result, mockElement);
            verify(provider, times(1)).ask(anyString(), anyString());

            AISelfHealer.initializeForThread(null, null);
        }
    }
    
    @Test
    public void testAttemptHeal_WithLowConfidence_ReturnsNull() {
        try (MockedStatic<AIConfigLoader> configMock = Mockito.mockStatic(AIConfigLoader.class)) {
            configMock.when(AIConfigLoader::getHealingStrategy).thenReturn(HealingStrategy.HEAL_AND_CONTINUE);
            configMock.when(AIConfigLoader::getConfidenceThreshold).thenReturn(0.85);

            WebDriver driver = mock(WebDriver.class);
            when(driver.getPageSource()).thenReturn("<html><body><div id='newLogin'>Login</div></body></html>");

            LLMProvider provider = mock(LLMProvider.class);
            String mockJsonResponse = "{\"locator\": \"By.id(\\\"newLogin\\\")\", \"confidence\": 0.50, \"reasoning\": \"Not sure\"}";
            when(provider.ask(anyString(), anyString())).thenReturn(mockJsonResponse);

            AISelfHealer.initializeForThread(provider, HealingStrategy.HEAL_AND_CONTINUE);

            WebElement result = AISelfHealer.attemptHeal(driver, By.id("oldLogin"), new StackTraceElement[0]);
            
            Assert.assertNull(result);
            AISelfHealer.initializeForThread(null, null);
        }
    }

    @Test
    public void testAttemptHeal_WithLlmException_ReturnsNullGracefully() {
        try (MockedStatic<AIConfigLoader> configMock = Mockito.mockStatic(AIConfigLoader.class)) {
            configMock.when(AIConfigLoader::getHealingStrategy).thenReturn(HealingStrategy.HEAL_AND_CONTINUE);

            WebDriver driver = mock(WebDriver.class);
            when(driver.getPageSource()).thenReturn("<html><body><div id='newLogin'>Login</div></body></html>");

            LLMProvider provider = mock(LLMProvider.class);
            when(provider.ask(anyString(), anyString())).thenThrow(new RuntimeException("API Timeout"));

            AISelfHealer.initializeForThread(provider, HealingStrategy.HEAL_AND_CONTINUE);

            WebElement result = AISelfHealer.attemptHeal(driver, By.id("oldLogin"), new StackTraceElement[0]);
            
            Assert.assertNull(result); // Should fail gracefully

            AISelfHealer.initializeForThread(null, null);
        }
    }

    @Test
    public void testAttemptHeal_WithMalformedJsonResponse_ReturnsNullGracefully() {
        try (MockedStatic<AIConfigLoader> configMock = Mockito.mockStatic(AIConfigLoader.class)) {
            configMock.when(AIConfigLoader::getHealingStrategy).thenReturn(HealingStrategy.HEAL_AND_CONTINUE);

            WebDriver driver = mock(WebDriver.class);
            when(driver.getPageSource()).thenReturn("<html><body><div id='newLogin'>Login</div></body></html>");

            LLMProvider provider = mock(LLMProvider.class);
            when(provider.ask(anyString(), anyString())).thenReturn("This is not JSON");

            AISelfHealer.initializeForThread(provider, HealingStrategy.HEAL_AND_CONTINUE);

            WebElement result = AISelfHealer.attemptHeal(driver, By.id("oldLogin"), new StackTraceElement[0]);
            
            Assert.assertNull(result);

            AISelfHealer.initializeForThread(null, null);
        }
    }
}
