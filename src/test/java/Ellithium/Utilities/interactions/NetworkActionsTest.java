package Ellithium.Utilities.interactions;

import org.openqa.selenium.HasAuthentication;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertNotNull;

public class NetworkActionsTest {

    interface AuthWebDriver extends WebDriver, HasAuthentication {}

    @Test
    public void authenticate_registersCredentialsOnHasAuthentication() {
        AuthWebDriver driver = mock(AuthWebDriver.class);
        NetworkActions<AuthWebDriver> actions = new NetworkActions<>(driver);

        NetworkActions<AuthWebDriver> result = actions.authenticate("admin", "secret123");

        assertNotNull(result);
        verify(driver).register(any(), any());
    }

    @Test
    public void close_clearsActiveInterceptorsWithoutThrowing() {
        WebDriver driver = mock(WebDriver.class);
        NetworkActions<WebDriver> actions = new NetworkActions<>(driver);

        actions.close();
        actions.reset();
    }
}
