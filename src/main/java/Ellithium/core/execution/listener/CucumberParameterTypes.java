package Ellithium.core.execution.listener;

import Ellithium.Utilities.helpers.Locators;
import io.cucumber.java.ParameterType;
import org.openqa.selenium.By;

/**
 * Common Cucumber 8 Parameter Types automatically registered for all BDD scenarios.
 * Enables step definitions to accept {locator} or {by} directly as {@link org.openqa.selenium.By} arguments.
 *
 * <p>Examples in Gherkin:</p>
 * <pre>
 *   When user clicks the "id=submit-btn"
 *   And user enters "secret" into "css=#password"
 * </pre>
 *
 * <p>Step Definition:</p>
 * <pre>
 *   &#64;When("user clicks the {locator}")
 *   public void clickElement(By locator) {
 *       driverActions.elements().clickOnElement(locator);
 *   }
 * </pre>
 */
public class CucumberParameterTypes {

    /**
     * Resolves a Gherkin string into a Selenium {@link By} locator.
     *
     * @param locatorStr The locator string from the step definition
     * @return Resolved {@link By} locator
     */
    @ParameterType(".*")
    public By locator(String locatorStr) {
        return Locators.fromString(unquote(locatorStr));
    }

    /**
     * Alias for {@link #locator(String)}.
     *
     * @param locatorStr The locator string from the step definition
     * @return Resolved {@link By} locator
     */
    @ParameterType(".*")
    public By by(String locatorStr) {
        return Locators.fromString(unquote(locatorStr));
    }

    private static String unquote(String str) {
        if (str == null) return null;
        String s = str.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
