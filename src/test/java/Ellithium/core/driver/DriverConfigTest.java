package Ellithium.core.driver;

import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DriverConfigTest {

    // ────────────────────── LocalDriverConfig ──────────────────────

    @Test
    public void localConfig_defaultConstructor_setsExpectedDefaults() {
        LocalDriverConfig config = new LocalDriverConfig();
        Assert.assertEquals(config.getLocalDriverType(), LocalDriverType.Chrome);
        Assert.assertEquals(config.getHeadlessMode(), HeadlessMode.False);
        Assert.assertEquals(config.getPageLoadStrategy(), PageLoadStrategyMode.Normal);
        Assert.assertEquals(config.getPrivateMode(), PrivateMode.False);
        Assert.assertEquals(config.getSandboxMode(), SandboxMode.Sandbox);
        Assert.assertEquals(config.getWebSecurityMode(), WebSecurityMode.SecureMode);
    }

    @Test
    public void localConfig_validate_passesWhenDriverTypeSet() {
        new LocalDriverConfig(LocalDriverType.Chrome).validate();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void localConfig_validate_throwsWhenDriverTypeNull() {
        LocalDriverConfig config = new LocalDriverConfig();
        config.setLocalDriverType(null);
        config.validate();
    }

    @Test
    public void localConfig_getDriverType_returnsChromeFallbackWhenFieldIsNull() {
        LocalDriverConfig config = new LocalDriverConfig();
        config.setLocalDriverType(null);
        Assert.assertEquals(config.getDriverType(), LocalDriverType.Chrome);
    }

    @Test
    public void localConfig_getLocalDriverType_returnsChromeFallbackWhenFieldIsNull() {
        LocalDriverConfig config = new LocalDriverConfig();
        config.setLocalDriverType(null);
        Assert.assertEquals(config.getLocalDriverType(), LocalDriverType.Chrome);
    }

    @Test
    public void localConfig_fluentSetters_applyAndChain() {
        LocalDriverConfig config = new LocalDriverConfig()
                .setLocalDriverType(LocalDriverType.FireFox)
                .setHeadlessMode(HeadlessMode.True)
                .setPrivateMode(PrivateMode.True)
                .setSandboxMode(SandboxMode.NoSandboxMode)
                .setWebSecurityMode(WebSecurityMode.AllowUnsecure)
                .setPageLoadStrategy(PageLoadStrategyMode.Eager);

        Assert.assertEquals(config.getLocalDriverType(), LocalDriverType.FireFox);
        Assert.assertEquals(config.getHeadlessMode(), HeadlessMode.True);
        Assert.assertEquals(config.getPrivateMode(), PrivateMode.True);
        Assert.assertEquals(config.getSandboxMode(), SandboxMode.NoSandboxMode);
        Assert.assertEquals(config.getWebSecurityMode(), WebSecurityMode.AllowUnsecure);
        Assert.assertEquals(config.getPageLoadStrategy(), PageLoadStrategyMode.Eager);
    }

    @Test
    public void localConfig_setDriverType_updatesViaInterface() {
        LocalDriverConfig config = new LocalDriverConfig();
        config.setDriverType(LocalDriverType.Edge);
        Assert.assertEquals(config.getLocalDriverType(), LocalDriverType.Edge);
    }

    // ────────────────────── RemoteDriverConfig ──────────────────────

    @Test
    public void remoteConfig_defaultConstructor_setsExpectedDefaults() {
        RemoteDriverConfig config = new RemoteDriverConfig();
        Assert.assertEquals(config.getRemoteDriverType(), RemoteDriverType.REMOTE_Chrome);
        Assert.assertNotNull(config.getRemoteAddress());
        Assert.assertTrue(config.getRemoteAddress().toString().contains("localhost"));
        Assert.assertEquals(config.getHeadlessMode(), HeadlessMode.False);
        Assert.assertEquals(config.getPrivateMode(), PrivateMode.False);
        Assert.assertEquals(config.getSandboxMode(), SandboxMode.Sandbox);
        Assert.assertEquals(config.getWebSecurityMode(), WebSecurityMode.SecureMode);
    }

    @Test
    public void remoteConfig_validate_passesWithDefaults() {
        new RemoteDriverConfig().validate();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void remoteConfig_validate_throwsWhenDriverTypeNull() {
        RemoteDriverConfig config = new RemoteDriverConfig();
        config.setRemoteDriverType(null);
        config.validate();
    }

    @Test
    public void remoteConfig_getDriverType_returnsChromeFallbackWhenFieldIsNull() {
        RemoteDriverConfig config = new RemoteDriverConfig();
        config.setRemoteDriverType(null);
        Assert.assertEquals(config.getDriverType(), RemoteDriverType.REMOTE_Chrome);
    }

    @Test
    public void remoteConfig_getRemoteAddress_returnsDefaultWhenNull() {
        RemoteDriverConfig config = new RemoteDriverConfig();
        config.setRemoteAddress((java.net.URL) null);
        Assert.assertNotNull(config.getRemoteAddress());
        Assert.assertTrue(config.getRemoteAddress().toString().contains("localhost"));
    }

    @Test
    public void remoteConfig_fluentSetters_applyAndChain() throws Exception {
        java.net.URL addr = new java.net.URL("http://grid.example.com:4444/wd/hub");
        RemoteDriverConfig config = new RemoteDriverConfig()
                .setRemoteDriverType(RemoteDriverType.REMOTE_FireFox)
                .setRemoteAddress(addr)
                .setHeadlessMode(HeadlessMode.True)
                .setPrivateMode(PrivateMode.True)
                .setSandboxMode(SandboxMode.NoSandboxMode)
                .setWebSecurityMode(WebSecurityMode.AllowUnsecure)
                .setPageLoadStrategy(PageLoadStrategyMode.Eager);

        Assert.assertEquals(config.getRemoteDriverType(), RemoteDriverType.REMOTE_FireFox);
        Assert.assertEquals(config.getRemoteAddress(), addr);
        Assert.assertEquals(config.getHeadlessMode(), HeadlessMode.True);
        Assert.assertEquals(config.getPrivateMode(), PrivateMode.True);
        Assert.assertEquals(config.getSandboxMode(), SandboxMode.NoSandboxMode);
        Assert.assertEquals(config.getWebSecurityMode(), WebSecurityMode.AllowUnsecure);
        Assert.assertEquals(config.getPageLoadStrategy(), PageLoadStrategyMode.Eager);
    }

    // ────────────────────── MobileDriverConfig ──────────────────────

    @Test(expectedExceptions = IllegalStateException.class)
    public void mobileConfig_validate_throwsWhenDriverTypeNull() {
        new MobileDriverConfig().validate();
    }

    @Test
    public void mobileConfig_validate_passesForAndroid() {
        new MobileDriverConfig(MobileDriverType.Android).validate();
    }

    @Test
    public void mobileConfig_validate_passesForIOS() {
        new MobileDriverConfig(MobileDriverType.IOS).validate();
    }

    @Test
    public void mobileConfig_getCapabilities_returnsUiAutomator2ForAndroid() {
        MobileDriverConfig config = new MobileDriverConfig(MobileDriverType.Android);
        Assert.assertTrue(config.getCapabilities() instanceof UiAutomator2Options,
                "Android config must return UiAutomator2Options");
    }

    @Test
    public void mobileConfig_getCapabilities_returnsXCUITestForIOS() {
        MobileDriverConfig config = new MobileDriverConfig(MobileDriverType.IOS);
        Assert.assertTrue(config.getCapabilities() instanceof XCUITestOptions,
                "iOS config must return XCUITestOptions");
    }

    @Test
    public void mobileConfig_setDriverType_setsPlatformNameCapability() {
        MobileDriverConfig config = new MobileDriverConfig(MobileDriverType.Android);
        Object platformName = config.getInternalCapabilities().get("platformName");
        Assert.assertNotNull(platformName, "platformName capability must be set");
        Assert.assertTrue("android".equalsIgnoreCase(platformName.toString()),
                "platformName must represent Android platform, got: " + platformName);
    }

    @Test
    public void mobileConfig_setDeviceName_reflectsInCapabilities() {
        MobileDriverConfig config = new MobileDriverConfig(MobileDriverType.Android)
                .setDeviceName("Pixel 7");
        Assert.assertEquals(config.getInternalCapabilities().get("appium:deviceName"), "Pixel 7");
    }

    @Test
    public void mobileConfig_getRemoteAddress_returnsDefaultWhenNoneSet() {
        MobileDriverConfig config = new MobileDriverConfig();
        Assert.assertNotNull(config.getRemoteAddress());
        Assert.assertTrue(config.getRemoteAddress().toString().contains("127.0.0.1"));
    }

    // ────────────────────── DriverFactory thread state ──────────────────────

    @Test
    public void factory_getCurrentDriver_returnsNullOnFreshThread() {
        DriverFactory.removeDriver();
        Assert.assertNull(DriverFactory.getCurrentDriver());
    }

    @Test
    public void factory_getCurrentDriverConfiguration_returnsNullOnFreshThread() {
        DriverFactory.removeDriver();
        Assert.assertNull(DriverFactory.getCurrentDriverConfiguration());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void factory_getNewDriver_throwsForNullConfig() {
        DriverFactory.getNewDriver((DriverConfigBuilder) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void factory_getNewDriver_throwsForUnknownConfigType() {
        DriverFactory.getNewDriver(new DriverConfigBuilder() {
            public DriverConfigBuilder setCapabilities(org.openqa.selenium.Capabilities c) { return this; }
            public DriverConfigBuilder setDriverType(DriverType t) { return this; }
            public org.openqa.selenium.Capabilities getCapabilities() { return null; }
            public DriverType getDriverType() { return null; }
        });
    }
}
