package AI;

import Ellithium.Utilities.ai.EllithiumAIEngine;
import Ellithium.Utilities.ai.config.AIConfigLoader;
import Ellithium.Utilities.ai.provider.GeminiProvider;
import Ellithium.Utilities.ai.provider.LLMProvider;
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
        SecureAreaPage secureAreaPage = loginPage.clickLoginBtn();

        // 2. Demonstrate Self-Healing on the next page
        // getLoginMessage() is intentionally broken (By.cssSelector("flash") instead of id("flash"))
        String message = secureAreaPage.getLoginMessage();
        //Assert.assertTrue(message.contains("You logged into a secure area!"), "Message was: " + message);

        // 3. Demonstrate Live In-Context Generation
        // Using the live, authenticated driver to click the Logout button
        String naturalLanguageSteps = "return to main page which is the same base url without login and then navigate to Dropdown page by clicking on dropdown link text";
        EllithiumAIEngine.continueFrom(driver, llmProvider, naturalLanguageSteps);

        // Verify we are back on the login page by checking the URL or title
        Assert.assertTrue(driver.getCurrentUrl().contains("/login"), "Expected to be back on the login page.");
    }

    @AfterMethod
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}
