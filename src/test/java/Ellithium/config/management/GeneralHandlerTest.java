package Ellithium.config.management;

import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.LocalDriverConfig;
import Ellithium.core.driver.LocalDriverType;
import io.qameta.allure.model.Parameter;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

public class GeneralHandlerTest {

    @Test
    public void testTestFailed_whenNoDriver_returnsNull() {
        // When no driver is bound to the thread, it safely returns null without throwing NPE
        File result = GeneralHandler.testFailed("chrome", "sampleTest");
        Assert.assertNull(result);

        // Also verify null arguments do not cause NPE
        File nullResult = GeneralHandler.testFailed(null, null);
        Assert.assertNull(nullResult);
    }

    @Test
    public void testAttachBrowserConsoleLogs_whenNoDriver_doesNotThrow() {
        // Safe invocation when driver is null
        GeneralHandler.attachBrowserConsoleLogs();
    }

    @Test
    public void testGetParameters_whenNoDriverConfig_returnsEmptyList() {
        List<Parameter> params = GeneralHandler.getParameters();
        Assert.assertNotNull(params);
        // Without active driver, configuration is null, returns empty list
        Assert.assertTrue(params.isEmpty());
    }

    @Test
    public void testAddAttachments_attachesAndClearsLogsGracefully() {
        Ellithium.core.logging.Logger.info("Sample log entry for GeneralHandler test");
        GeneralHandler.addAttachments();
    }
}
