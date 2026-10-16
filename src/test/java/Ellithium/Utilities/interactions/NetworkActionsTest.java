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

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*urlPattern must not be null or blank.*")
    public void mockResponse_nullUrlPattern_throwsIllegalArgumentException() {
        WebDriver driver = mock(WebDriver.class);
        NetworkActions<WebDriver> actions = new NetworkActions<>(driver);
        actions.mockResponse(null, 200, "text/plain", "ok");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*urlPattern must not be null or blank.*")
    public void mockResponse_blankUrlPattern_throwsIllegalArgumentException() {
        WebDriver driver = mock(WebDriver.class);
        NetworkActions<WebDriver> actions = new NetworkActions<>(driver);
        actions.mockResponse("   ", 200, "text/plain", "ok");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*filter must not be null.*")
    public void intercept_nullFilter_throwsIllegalArgumentException() {
        WebDriver driver = mock(WebDriver.class);
        NetworkActions<WebDriver> actions = new NetworkActions<>(driver);
        actions.intercept(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*username must not be null.*")
    public void authenticate_nullUsername_throwsIllegalArgumentException() {
        WebDriver driver = mock(WebDriver.class);
        NetworkActions<WebDriver> actions = new NetworkActions<>(driver);
        actions.authenticate(null, "pass");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*password must not be null.*")
    public void authenticate_nullPassword_throwsIllegalArgumentException() {
        WebDriver driver = mock(WebDriver.class);
        NetworkActions<WebDriver> actions = new NetworkActions<>(driver);
        actions.authenticate("user", null);
    }

    @Test
    public void authenticate_plainDriver_gracefullyHandlesUnsupported() {
        WebDriver driver = mock(WebDriver.class);
        NetworkActions<WebDriver> actions = new NetworkActions<>(driver);
        NetworkActions<WebDriver> result = actions.authenticate("user", "pass");
        assertNotNull(result);
    }
}
