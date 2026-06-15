package Ellithium.core.ai.healing;

import Ellithium.core.ai.models.HealOutcome;
import Ellithium.core.ai.models.HealingRequest;
import Ellithium.core.ai.reporting.AIHealingReporter;
import Ellithium.core.ai.spi.HealingTier;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Regression guard for the double-reporting bug: SemanticLocatorResolver previously called
 * AIHealingReporter.queueChange internally, AND the orchestrator called it again, causing every
 * heal to appear twice in the healing report. The fix removed the internal call from
 * SemanticLocatorResolver. This test verifies the orchestrator fires queueChange exactly once.
 */
public class OrchestratorReportingTest {

    @Test
    public void heal_queuesChangeExactlyOnce_forNonSelfPersistingTier() {
        By brokenLocator = By.id("broken-rpt-" + System.nanoTime());
        WebDriver driver = mock(WebDriver.class);
        WebElement healedElement = mock(WebElement.class);
        when(healedElement.isEnabled()).thenReturn(true);

        By reconstructed = By.id("healed-btn");
        HealingTier mockTier = mock(HealingTier.class);
        when(mockTier.order()).thenReturn(2);
        when(mockTier.isAvailable()).thenReturn(true);
        when(mockTier.persistsOwnHeal()).thenReturn(false);
        when(mockTier.heal(any())).thenReturn(HealOutcome.of(healedElement, reconstructed, 0.9, 2));

        HealingRequest request = new HealingRequest(
                driver, brokenLocator, new StackTraceElement[0],
                "findElement", "testMethod", "locatorField", "broken", null);

        try (MockedStatic<AIHealingReporter> reporter = Mockito.mockStatic(AIHealingReporter.class);
             MockedStatic<AISelfHealer>      aiHealer = Mockito.mockStatic(AISelfHealer.class);
             MockedStatic<BaselineStore>     bs       = Mockito.mockStatic(BaselineStore.class)) {

            // void statics do nothing by default in MockedStatic — no stubbing needed for
            // cacheHealedLocator, queueSourcePatch, and capture.
            aiHealer.when(() -> AISelfHealer.byToJavaExpression(any()))
                    .thenReturn("By.id(\"healed-btn\")");

            new HealingOrchestrator(List.of(mockTier)).heal(request);

            reporter.verify(() -> AIHealingReporter.queueChange(
                    any(), any(), any(), any(), any(), any(), anyInt()), times(1));
        }
    }
}
