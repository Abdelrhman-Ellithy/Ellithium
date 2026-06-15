package Ellithium.config.management;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ConfigContextTest {

    private int originalRetry;
    private boolean originalOnExecution;
    private boolean originalLoggingOn;

    @BeforeMethod
    public void snapshot() {
        originalRetry = ConfigContext.getRetryCount();
        originalOnExecution = ConfigContext.isOnExecution();
        originalLoggingOn = ConfigContext.isLoggingOn();
    }

    @AfterMethod
    public void restore() {
        ConfigContext.setRetryCount(originalRetry);
        ConfigContext.setOnExecution(originalOnExecution);
        ConfigContext.setIsLoggingOn(originalLoggingOn);
    }

    // ── Version constants ──────────────────────────────────────────────────

    @Test
    public void getAllureVersion_returnsNonBlank() {
        Assert.assertNotNull(ConfigContext.getAllureVersion());
        Assert.assertFalse(ConfigContext.getAllureVersion().isBlank());
    }

    @Test
    public void getEllithiumVersion_returnsNonBlank() {
        Assert.assertNotNull(ConfigContext.getEllithuiumVersion());
        Assert.assertFalse(ConfigContext.getEllithuiumVersion().isBlank());
    }

    // ── retryCount ─────────────────────────────────────────────────────────

    @Test
    public void retryCount_setAndGet_roundTrips() {
        ConfigContext.setRetryCount(5);
        Assert.assertEquals(ConfigContext.getRetryCount(), 5);
    }

    @Test
    public void retryCount_zero_isValid() {
        ConfigContext.setRetryCount(0);
        Assert.assertEquals(ConfigContext.getRetryCount(), 0);
    }

    @Test
    public void state_retryCount_matchesTopLevel() {
        ConfigContext.setRetryCount(3);
        Assert.assertEquals(ConfigContext.State.retryCount(), 3);
    }

    // ── onExecution ────────────────────────────────────────────────────────

    @Test
    public void onExecution_setTrue_returnsTrue() {
        ConfigContext.setOnExecution(true);
        Assert.assertTrue(ConfigContext.isOnExecution());
    }

    @Test
    public void onExecution_setFalse_returnsFalse() {
        ConfigContext.setOnExecution(false);
        Assert.assertFalse(ConfigContext.isOnExecution());
    }

    @Test
    public void state_onExecution_reflectsTopLevel() {
        ConfigContext.setOnExecution(true);
        Assert.assertTrue(ConfigContext.State.onExecution());
    }

    // ── loggingOn ─────────────────────────────────────────────────────────

    @Test
    public void loggingOn_setTrue_returnsTrue() {
        ConfigContext.setIsLoggingOn(true);
        Assert.assertTrue(ConfigContext.isLoggingOn());
    }

    @Test
    public void state_setLoggingOn_reflectsTopLevel() {
        ConfigContext.State.setLoggingOn(true);
        Assert.assertTrue(ConfigContext.isLoggingOn());
    }

    // ── Paths ──────────────────────────────────────────────────────────────

    @Test
    public void paths_configFilePath_endsWithProperties() {
        Assert.assertTrue(ConfigContext.getConfigFilePath().endsWith(".properties"));
    }

    @Test
    public void paths_logFilePath_endsWithProperties() {
        Assert.assertTrue(ConfigContext.getLogFilePath().endsWith(".properties"));
    }

    @Test
    public void paths_allureFilePath_endsWithProperties() {
        Assert.assertTrue(ConfigContext.getAllureFilePath().endsWith(".properties"));
    }

    @Test
    public void paths_aiFilePath_endsWithProperties() {
        Assert.assertTrue(ConfigContext.getAiFilePath().endsWith(".properties"));
    }

    @Test
    public void paths_static_matchTopLevel() {
        Assert.assertEquals(ConfigContext.Paths.config(), ConfigContext.getConfigFilePath());
        Assert.assertEquals(ConfigContext.Paths.log(), ConfigContext.getLogFilePath());
    }

    @Test
    public void getEllithiumRepoPath_containsM2() {
        Assert.assertTrue(ConfigContext.getEllithiumRepoPath().contains(".m2"));
    }
}
