package Ellithium.core.ai.healing;

import Ellithium.core.ai.models.HealOutcome;
import Ellithium.core.ai.models.HealingRequest;
import Ellithium.core.ai.reporting.AIHealingReporter;
import Ellithium.core.ai.spi.HealingTier;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class HealingOrchestratorGatingTest {

    @Test
    public void testBridgeLabel_ForClickOnRadioOrCheckbox_ReturnsTargetInput() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        WebElement label = mock(WebElement.class);
        WebElement input = mock(WebElement.class);

        when(label.getTagName()).thenReturn("label");
        when(label.getAttribute("for")).thenReturn("radio-firefox");
        when(driver.findElement(By.id("radio-firefox"))).thenReturn(input);
        when(input.getTagName()).thenReturn("input");
        when(input.getAttribute("type")).thenReturn("radio");
        when(input.isEnabled()).thenReturn(true);
        when(input.isDisplayed()).thenReturn(true);

        HealingTier mockTier = mock(HealingTier.class);
        when(mockTier.order()).thenReturn(2);
        when(mockTier.isAvailable()).thenReturn(true);
        when(mockTier.persistsOwnHeal()).thenReturn(false);
        when(mockTier.heal(any())).thenReturn(HealOutcome.of(label, By.id("label-firefox"), 0.90, 2));

        HealingRequest request = new HealingRequest(
                driver, By.id("broken-radio"), new StackTraceElement[0],
                "clickOnElement", "selectBrowser", "firefoxRadio", "radio", null);

        try (MockedStatic<AIHealingReporter> reporter = Mockito.mockStatic(AIHealingReporter.class);
             MockedStatic<AISelfHealer>      aiHealer = Mockito.mockStatic(AISelfHealer.class);
             MockedStatic<BaselineStore>     bs       = Mockito.mockStatic(BaselineStore.class);
             MockedStatic<HealedLocatorBuilder> hlb   = Mockito.mockStatic(HealedLocatorBuilder.class)) {

            hlb.when(() -> HealedLocatorBuilder.build(any(), any(), any()))
                    .thenReturn(By.id("radio-firefox"));

            HealingOrchestrator orchestrator = new HealingOrchestrator(List.of(mockTier));
            HealOutcome outcome = orchestrator.heal(request);

            Assert.assertNotNull(outcome, "Heal outcome should not be null");
            Assert.assertEquals(outcome.element(), input, "Label must be bridged to associated radio input");
        }
    }

    @Test
    public void testResolveInteractive_ForSendKeys_RejectsNonEditableContainer() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        WebElement div = mock(WebElement.class);
        when(div.getTagName()).thenReturn("div");
        when(div.getAttribute("role")).thenReturn(null);
        when(div.getAttribute("type")).thenReturn(null);
        when(div.getAttribute("contenteditable")).thenReturn("false");
        when(div.findElements(any(By.class))).thenReturn(List.of());
        when(div.isEnabled()).thenReturn(true);

        HealingTier mockTier = mock(HealingTier.class);
        when(mockTier.order()).thenReturn(2);
        when(mockTier.isAvailable()).thenReturn(true);
        when(mockTier.persistsOwnHeal()).thenReturn(false);
        when(mockTier.heal(any())).thenReturn(HealOutcome.of(div, By.id("div-box"), 0.90, 2));

        HealingRequest request = new HealingRequest(
                driver, By.id("broken-input"), new StackTraceElement[0],
                "sendData", "enterUsername", "usernameField", "username", null);

        HealingOrchestrator orchestrator = new HealingOrchestrator(List.of(mockTier));
        HealOutcome outcome = orchestrator.heal(request);

        Assert.assertNull(outcome, "SEND_KEYS on non-editable div with no editable children must be rejected");
    }

    @Test
    public void testResolveInteractive_ForClick_AcceptsCustomElementWithAriaRole() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        WebElement customBtn = mock(WebElement.class);
        when(customBtn.getTagName()).thenReturn("div");
        when(customBtn.getAttribute("role")).thenReturn("button");
        when(customBtn.isEnabled()).thenReturn(true);
        when(customBtn.isDisplayed()).thenReturn(true);

        when(((JavascriptExecutor) driver).executeScript(anyString(), any()))
                .thenReturn(new Object[]{"div", "button"});

        HealingTier mockTier = mock(HealingTier.class);
        when(mockTier.order()).thenReturn(2);
        when(mockTier.isAvailable()).thenReturn(true);
        when(mockTier.persistsOwnHeal()).thenReturn(false);
        when(mockTier.heal(any())).thenReturn(HealOutcome.of(customBtn, By.cssSelector("div.btn"), 0.90, 2));

        HealingRequest request = new HealingRequest(
                driver, By.id("broken-btn"), new StackTraceElement[0],
                "clickOnElement", "clickSubmit", "submitBtn", "submit", null);

        try (MockedStatic<AIHealingReporter> reporter = Mockito.mockStatic(AIHealingReporter.class);
             MockedStatic<AISelfHealer>      aiHealer = Mockito.mockStatic(AISelfHealer.class);
             MockedStatic<BaselineStore>     bs       = Mockito.mockStatic(BaselineStore.class);
             MockedStatic<HealedLocatorBuilder> hlb   = Mockito.mockStatic(HealedLocatorBuilder.class)) {

            hlb.when(() -> HealedLocatorBuilder.build(any(), any(), any()))
                    .thenReturn(By.cssSelector("div[role='button']"));

            HealingOrchestrator orchestrator = new HealingOrchestrator(List.of(mockTier));
            HealOutcome outcome = orchestrator.heal(request);

            Assert.assertNotNull(outcome, "Custom div with role='button' must be accepted for click");
            Assert.assertEquals(outcome.element(), customBtn);
        }
    }

    @Test
    public void testBridgeLabel_ForHiddenRadioOrCheckbox_FallsBackToLabelWhenInputHidden() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        WebElement label = mock(WebElement.class);
        WebElement input = mock(WebElement.class);

        when(label.getTagName()).thenReturn("label");
        when(label.getAttribute("for")).thenReturn("radio-firefox-hidden");
        when(label.isDisplayed()).thenReturn(true);
        when(driver.findElement(By.id("radio-firefox-hidden"))).thenReturn(input);
        when(input.getTagName()).thenReturn("input");
        when(input.getAttribute("type")).thenReturn("radio");
        when(input.isEnabled()).thenReturn(true);
        when(input.isDisplayed()).thenReturn(false); // Hidden input!

        HealingTier mockTier = mock(HealingTier.class);
        when(mockTier.order()).thenReturn(2);
        when(mockTier.isAvailable()).thenReturn(true);
        when(mockTier.persistsOwnHeal()).thenReturn(false);
        when(mockTier.heal(any())).thenReturn(HealOutcome.of(label, By.id("label-firefox"), 0.90, 2));

        HealingRequest request = new HealingRequest(
                driver, By.id("broken-radio"), new StackTraceElement[0],
                "clickOnElement", "selectBrowser", "firefoxRadio", "radio", null);

        try (MockedStatic<AIHealingReporter> reporter = Mockito.mockStatic(AIHealingReporter.class);
             MockedStatic<AISelfHealer>      aiHealer = Mockito.mockStatic(AISelfHealer.class);
             MockedStatic<BaselineStore>     bs       = Mockito.mockStatic(BaselineStore.class);
             MockedStatic<HealedLocatorBuilder> hlb   = Mockito.mockStatic(HealedLocatorBuilder.class)) {

            hlb.when(() -> HealedLocatorBuilder.build(any(), any(), any()))
                    .thenReturn(By.id("label-firefox"));

            HealingOrchestrator orchestrator = new HealingOrchestrator(List.of(mockTier));
            HealOutcome outcome = orchestrator.heal(request);

            Assert.assertNotNull(outcome);
            Assert.assertEquals(outcome.element(), label, "When input is hidden, click must target visible label");
        }
    }

    @Test
    public void testResolveInteractive_ForSendKeys_AcceptsEmptyContentEditable() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        WebElement div = mock(WebElement.class);
        when(div.getTagName()).thenReturn("div");
        when(div.getAttribute("role")).thenReturn(null);
        when(div.getAttribute("type")).thenReturn(null);
        when(div.getAttribute("contenteditable")).thenReturn(""); // HTML5 boolean attribute
        when(div.isEnabled()).thenReturn(true);
        when(div.isDisplayed()).thenReturn(true);

        when(((JavascriptExecutor) driver).executeScript(anyString(), any()))
                .thenReturn(new Object[]{"div", null, null, ""});

        HealingTier mockTier = mock(HealingTier.class);
        when(mockTier.order()).thenReturn(2);
        when(mockTier.isAvailable()).thenReturn(true);
        when(mockTier.persistsOwnHeal()).thenReturn(false);
        when(mockTier.heal(any())).thenReturn(HealOutcome.of(div, By.id("editable-div"), 0.90, 2));

        HealingRequest request = new HealingRequest(
                driver, By.id("broken-editor"), new StackTraceElement[0],
                "sendData", "enterNotes", "notesField", "notes", null);

        try (MockedStatic<AIHealingReporter> reporter = Mockito.mockStatic(AIHealingReporter.class);
             MockedStatic<AISelfHealer>      aiHealer = Mockito.mockStatic(AISelfHealer.class);
             MockedStatic<BaselineStore>     bs       = Mockito.mockStatic(BaselineStore.class);
             MockedStatic<HealedLocatorBuilder> hlb   = Mockito.mockStatic(HealedLocatorBuilder.class)) {

            hlb.when(() -> HealedLocatorBuilder.build(any(), any(), any()))
                    .thenReturn(By.id("editable-div"));

            HealingOrchestrator orchestrator = new HealingOrchestrator(List.of(mockTier));
            HealOutcome outcome = orchestrator.heal(request);

            Assert.assertNotNull(outcome, "Empty string contenteditable div must be accepted for sendData");
            Assert.assertEquals(outcome.element(), div);
        }
    }
}
