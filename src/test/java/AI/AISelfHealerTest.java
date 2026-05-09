package AI;

import Ellithium.Utilities.ai.AISelfHealer;
import Ellithium.Utilities.ai.config.AIConfigLoader;
import Ellithium.Utilities.ai.config.HealingStrategy;
import Ellithium.Utilities.ai.provider.LLMProvider;
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
        try (MockedStatic<AIConfigLoader> configMock = Mockito.mockStatic(AIConfigLoader.class)) {
            configMock.when(AIConfigLoader::getHealingStrategy).thenReturn(HealingStrategy.DISABLED);

            WebDriver driver = mock(WebDriver.class);
            WebElement element = AISelfHealer.attemptHeal(driver, By.id("broken"), new StackTraceElement[0]);
            
            Assert.assertNull(element);
            verify(driver, never()).getPageSource();
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
            verify(driver, never()).findElement(any(By.class));

            AISelfHealer.initializeForThread(null, null);
        }
    }
}
