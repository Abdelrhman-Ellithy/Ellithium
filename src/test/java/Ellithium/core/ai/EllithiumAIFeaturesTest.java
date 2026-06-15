package Ellithium.core.ai;

import Ellithium.Utilities.ai.EllithiumAIEngine;
import Ellithium.Utilities.ai.GeminiProvider;
import Ellithium.Utilities.ai.LLMProvider;
import Ellithium.Utilities.interactions.DriverActions;
import Ellithium.Utilities.interactions.ScreenRecorderActions;
import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.HeadlessMode;
import Ellithium.core.driver.LocalDriverType;
import Pages.LoginPage;
import Pages.SecureAreaPage;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EllithiumAIFeaturesTest {

    private WebDriver driver;
    private LLMProvider llmProvider;

    @BeforeMethod
    public void setUp() {
        // Initialize Driver
        driver = DriverFactory.getNewLocalDriver(LocalDriverType.Chrome, HeadlessMode.False);
        
        // Initialize AI Configuration
        AIConfigLoader.initialize();
        String apiKey = AIConfigLoader.getLlmApiKey();
        String model = AIConfigLoader.getLlmModel();
        String baseUrl = AIConfigLoader.getLlmBaseUrl();
        
        // Setup Provider
        llmProvider = new GeminiProvider(baseUrl, apiKey, model);
    }

    @Test
    public void testSelfHealingAndInContextGeneration() {
        driver.get("https://the-internet.herokuapp.com/login");

        LoginPage loginPage = new LoginPage(driver);

        // 1. Demonstrate Self-Healing
        // The locators in LoginPage are INTENTIONALLY BROKEN (e.g. By.id("name") instead of "username")
        // The AISelfHealer will catch the failure, analyze the AX tree, and heal the locator at runtime
        loginPage.setUserName("tomsmith");
        loginPage.setPassword("SuperSecretPassword!");

        // 3. Demonstrate Live In-Context Generation
        String naturalLanguageSteps = "navigate to https://the-internet.herokuapp.com/ and then click on the link with text Dropdown to go to the dropdown page";
        EllithiumAIEngine.continueFrom(driver, llmProvider, naturalLanguageSteps);
        DriverActions driverActions = new DriverActions(driver);
        driverActions.waits().waitForUrlContains("dropdown");
        new ScreenRecorderActions<>(driver).captureScreenshot("naturalLanguageSteps");
        Assert.assertTrue(driver.getCurrentUrl().contains("/dropdown"), "Expected to be on the dropdown page but was: " + driver.getCurrentUrl());
    }

    @AfterMethod
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}
