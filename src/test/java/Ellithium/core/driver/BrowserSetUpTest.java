package Ellithium.core.driver;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests that BrowserSetUp builds the correct Options objects and that
 * user-supplied capabilities properly survive / override framework defaults.
 *
 * All tests use reflection on the private configure* methods — no real
 * browser binary is required. The Options object is inspected in-process via asMap().
 *
 * Args are read from the browser-specific capability sub-map:
 *   Chrome  → asMap().get("goog:chromeOptions") → get("args")
 *   Edge    → asMap().get("ms:edgeOptions")      → get("args")
 *   Firefox → asMap().get("moz:firefoxOptions")  → get("args")
 */
public class BrowserSetUpTest {

    // ── reflection helpers ────────────────────────────────────────────────

    private static ChromeOptions chromeOpts(HeadlessMode hm, PageLoadStrategyMode pls,
            PrivateMode pm, SandboxMode sm, WebSecurityMode wsm) throws Exception {
        Method m = BrowserSetUp.class.getDeclaredMethod("configureChromeOptions",
                HeadlessMode.class, PageLoadStrategyMode.class,
                PrivateMode.class, SandboxMode.class, WebSecurityMode.class);
        m.setAccessible(true);
        return (ChromeOptions) m.invoke(null, hm, pls, pm, sm, wsm);
    }

    private static ChromeOptions defaultChromeOpts() throws Exception {
        return chromeOpts(HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.False, SandboxMode.Sandbox, WebSecurityMode.SecureMode);
    }

    private static FirefoxOptions firefoxOpts(HeadlessMode hm, PageLoadStrategyMode pls,
            PrivateMode pm, SandboxMode sm, WebSecurityMode wsm) throws Exception {
        Method m = BrowserSetUp.class.getDeclaredMethod("configureFirefoxOptions",
                HeadlessMode.class, PageLoadStrategyMode.class,
                PrivateMode.class, SandboxMode.class, WebSecurityMode.class);
        m.setAccessible(true);
        return (FirefoxOptions) m.invoke(null, hm, pls, pm, sm, wsm);
    }

    private static FirefoxOptions defaultFirefoxOpts() throws Exception {
        return firefoxOpts(HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.False, SandboxMode.Sandbox, WebSecurityMode.SecureMode);
    }

    private static EdgeOptions edgeOpts(HeadlessMode hm, PageLoadStrategyMode pls,
            PrivateMode pm, SandboxMode sm, WebSecurityMode wsm) throws Exception {
        Method m = BrowserSetUp.class.getDeclaredMethod("configureEdgeOptions",
                HeadlessMode.class, PageLoadStrategyMode.class,
                PrivateMode.class, SandboxMode.class, WebSecurityMode.class);
        m.setAccessible(true);
        return (EdgeOptions) m.invoke(null, hm, pls, pm, sm, wsm);
    }

    private static SafariOptions safariOpts(PageLoadStrategyMode pls, PrivateMode pm) throws Exception {
        Method m = BrowserSetUp.class.getDeclaredMethod("configureSafariOptions",
                PageLoadStrategyMode.class, PrivateMode.class);
        m.setAccessible(true);
        return (SafariOptions) m.invoke(null, pls, pm);
    }

    // Args live inside the browser-specific sub-map, not at the top-level capability map.
    @SuppressWarnings("unchecked")
    private static List<String> chromeArgs(ChromeOptions opts) {
        Map<String, Object> sub = (Map<String, Object>) opts.asMap().get("goog:chromeOptions");
        if (sub == null) return Collections.emptyList();
        List<String> args = (List<String>) sub.get("args");
        return args != null ? args : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private static List<String> edgeArgs(EdgeOptions opts) {
        Map<String, Object> sub = (Map<String, Object>) opts.asMap().get("ms:edgeOptions");
        if (sub == null) return Collections.emptyList();
        List<String> args = (List<String>) sub.get("args");
        return args != null ? args : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private static List<String> firefoxArgs(FirefoxOptions opts) {
        Map<String, Object> sub = (Map<String, Object>) opts.asMap().get("moz:firefoxOptions");
        if (sub == null) return Collections.emptyList();
        List<String> args = (List<String>) sub.get("args");
        return args != null ? args : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firefoxPrefs(FirefoxOptions opts) {
        Map<String, Object> sub = (Map<String, Object>) opts.asMap().get("moz:firefoxOptions");
        if (sub == null) return Collections.emptyMap();
        Map<String, Object> prefs = (Map<String, Object>) sub.get("prefs");
        return prefs != null ? prefs : Collections.emptyMap();
    }

    // ── Chrome: conditional args ──────────────────────────────────────────

    @Test
    public void chrome_headlessTrue_addsHeadlessArg() throws Exception {
        ChromeOptions opts = chromeOpts(HeadlessMode.True, PageLoadStrategyMode.Normal,
                PrivateMode.False, SandboxMode.Sandbox, WebSecurityMode.SecureMode);
        Assert.assertTrue(chromeArgs(opts).contains("--headless"));
    }

    @Test
    public void chrome_headlessFalse_noHeadlessArg() throws Exception {
        Assert.assertFalse(chromeArgs(defaultChromeOpts()).contains("--headless"));
    }

    @Test
    public void chrome_privateTrue_addsIncognito() throws Exception {
        ChromeOptions opts = chromeOpts(HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.True, SandboxMode.Sandbox, WebSecurityMode.SecureMode);
        Assert.assertTrue(chromeArgs(opts).contains("--incognito"));
    }

    @Test
    public void chrome_privateFalse_noIncognito() throws Exception {
        Assert.assertFalse(chromeArgs(defaultChromeOpts()).contains("--incognito"));
    }

    @Test
    public void chrome_noSandbox_addsNoSandboxArg() throws Exception {
        ChromeOptions opts = chromeOpts(HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.False, SandboxMode.NoSandboxMode, WebSecurityMode.SecureMode);
        Assert.assertTrue(chromeArgs(opts).contains("--no-sandbox"));
    }

    @Test
    public void chrome_sandbox_noNoSandboxArg() throws Exception {
        Assert.assertFalse(chromeArgs(defaultChromeOpts()).contains("--no-sandbox"));
    }

    @Test
    public void chrome_allowUnsecure_addsWebSecurityArgs() throws Exception {
        ChromeOptions opts = chromeOpts(HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.False, SandboxMode.Sandbox, WebSecurityMode.AllowUnsecure);
        List<String> args = chromeArgs(opts);
        Assert.assertTrue(args.contains("--disable-web-security"));
        Assert.assertTrue(args.contains("--allow-running-insecure-content"));
    }

    @Test
    public void chrome_secureMode_noWebSecurityArgs() throws Exception {
        List<String> args = chromeArgs(defaultChromeOpts());
        Assert.assertFalse(args.contains("--disable-web-security"));
        Assert.assertFalse(args.contains("--allow-running-insecure-content"));
    }

    // ── Chrome: base args and framework caps always present ───────────────

    @Test
    public void chrome_baseArgs_alwaysPresent() throws Exception {
        List<String> args = chromeArgs(defaultChromeOpts());
        Assert.assertTrue(args.contains("--window-size=1920,1080"));
        Assert.assertTrue(args.contains("--disable-extensions"));
        Assert.assertTrue(args.contains("--disable-dev-shm-usage"));
        Assert.assertTrue(args.contains("--ignore-certificate-errors"));
        Assert.assertTrue(args.contains("--disable-background-networking"));
    }

    @Test
    public void chrome_frameworkCapabilities_webSocketUrlSetTrue() throws Exception {
        Assert.assertEquals(defaultChromeOpts().getCapability("webSocketUrl"), true,
                "Framework must set webSocketUrl=true for CDP support");
    }

    // ── Chrome: user capabilities merge and override ──────────────────────

    @Test
    public void chrome_userCap_survivesAfterMerge() throws Exception {
        MutableCapabilities userCaps = new MutableCapabilities();
        userCaps.setCapability("project:customCap", "expectedValue");
        ChromeOptions merged = defaultChromeOpts().merge(userCaps);
        Assert.assertEquals(merged.getCapability("project:customCap"), "expectedValue",
                "User capability must be present in merged options");
    }

    @Test
    public void chrome_userCap_overridesSameKeyFrameworkCap() throws Exception {
        ChromeOptions opts = defaultChromeOpts();
        Assert.assertEquals(opts.getCapability("webSocketUrl"), true);

        MutableCapabilities userCaps = new MutableCapabilities();
        userCaps.setCapability("webSocketUrl", false);
        ChromeOptions merged = opts.merge(userCaps);

        Assert.assertEquals(merged.getCapability("webSocketUrl"), false,
                "User capability must override the framework capability for the same key");
    }

    @Test
    public void chrome_userCap_multipleKeysSurviveMerge() throws Exception {
        MutableCapabilities userCaps = new MutableCapabilities();
        userCaps.setCapability("project:env", "staging");
        userCaps.setCapability("project:build", "42");
        ChromeOptions merged = defaultChromeOpts().merge(userCaps);
        Assert.assertEquals(merged.getCapability("project:env"), "staging");
        Assert.assertEquals(merged.getCapability("project:build"), "42");
    }

    @Test
    public void chrome_noUserCaps_frameworkArgsComplete() throws Exception {
        ChromeOptions opts = defaultChromeOpts();
        Assert.assertTrue(chromeArgs(opts).contains("--window-size=1920,1080"),
                "Framework args must be intact when no user caps are supplied");
        Assert.assertEquals(opts.getCapability("webSocketUrl"), true,
                "Framework caps must be intact when no user caps are supplied");
    }

    // ── Firefox: private mode uses preference, not a CLI arg ──────────────

    @Test
    public void firefox_headlessTrue_addsHeadlessArg() throws Exception {
        FirefoxOptions opts = firefoxOpts(HeadlessMode.True, PageLoadStrategyMode.Normal,
                PrivateMode.False, SandboxMode.Sandbox, WebSecurityMode.SecureMode);
        Assert.assertTrue(firefoxArgs(opts).contains("--headless"));
    }

    @Test
    public void firefox_privateTrue_setsPreference_notCliArg() throws Exception {
        FirefoxOptions opts = firefoxOpts(HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.True, SandboxMode.Sandbox, WebSecurityMode.SecureMode);
        Assert.assertEquals(firefoxPrefs(opts).get("browser.privatebrowsing.autostart"), true,
                "Firefox private mode must use the preference, not a CLI arg");
        Assert.assertFalse(firefoxArgs(opts).contains("--incognito"),
                "Firefox must never use --incognito");
    }

    @Test
    public void firefox_privateFalse_noAutoStartPreference() throws Exception {
        Assert.assertNotEquals(
                firefoxPrefs(defaultFirefoxOpts()).get("browser.privatebrowsing.autostart"), true,
                "Private preference must not be set when PrivateMode.False");
    }

    @Test
    public void firefox_allowUnsecure_setsMixedContentPrefsAndAcceptInsecureCerts() throws Exception {
        FirefoxOptions opts = firefoxOpts(HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.False, SandboxMode.Sandbox, WebSecurityMode.AllowUnsecure);
        Map<String, Object> prefs = firefoxPrefs(opts);
        Assert.assertEquals(prefs.get("security.mixed_content.block_active_content"), false);
        Assert.assertEquals(prefs.get("security.mixed_content.block_display_content"), false);
        Assert.assertEquals(opts.asMap().get("acceptInsecureCerts"), true,
                "acceptInsecureCerts must be true for AllowUnsecure");
    }

    @Test
    public void firefox_userCap_survivesAfterMerge() throws Exception {
        MutableCapabilities userCaps = new MutableCapabilities();
        userCaps.setCapability("project:env", "prod");
        FirefoxOptions merged = defaultFirefoxOpts().merge(userCaps);
        Assert.assertEquals(merged.getCapability("project:env"), "prod");
    }

    // ── Edge: uses --inPrivate, not --incognito ───────────────────────────

    @Test
    public void edge_privateTrue_addsInPrivate_notIncognito() throws Exception {
        EdgeOptions opts = edgeOpts(HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.True, SandboxMode.Sandbox, WebSecurityMode.SecureMode);
        List<String> args = edgeArgs(opts);
        Assert.assertTrue(args.contains("--inPrivate"),
                "Edge must use --inPrivate for private mode");
        Assert.assertFalse(args.contains("--incognito"),
                "Edge must not use Chrome's --incognito flag");
    }

    @Test
    public void edge_privateFalse_noPrivateFlag() throws Exception {
        List<String> args = edgeArgs(edgeOpts(HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.False, SandboxMode.Sandbox, WebSecurityMode.SecureMode));
        Assert.assertFalse(args.contains("--inPrivate"));
        Assert.assertFalse(args.contains("--incognito"));
    }

    @Test
    public void edge_noSandbox_addsNoSandboxArg() throws Exception {
        EdgeOptions opts = edgeOpts(HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.False, SandboxMode.NoSandboxMode, WebSecurityMode.SecureMode);
        Assert.assertTrue(edgeArgs(opts).contains("--no-sandbox"));
    }

    @Test
    public void edge_baseArgs_alwaysPresent() throws Exception {
        List<String> args = edgeArgs(edgeOpts(HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.False, SandboxMode.Sandbox, WebSecurityMode.SecureMode));
        Assert.assertTrue(args.contains("--window-size=1920,1080"));
        Assert.assertTrue(args.contains("--disable-extensions"));
    }

    @Test
    public void edge_userCap_overridesSameKeyFrameworkCap() throws Exception {
        EdgeOptions opts = edgeOpts(HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.False, SandboxMode.Sandbox, WebSecurityMode.SecureMode);
        Assert.assertEquals(opts.getCapability("webSocketUrl"), true);

        MutableCapabilities userCaps = new MutableCapabilities();
        userCaps.setCapability("webSocketUrl", false);
        EdgeOptions merged = opts.merge(userCaps);

        Assert.assertEquals(merged.getCapability("webSocketUrl"), false,
                "User capability must override Edge framework capability");
    }

    // ── Safari: page load strategy and merge ─────────────────────────────

    @Test
    public void safari_eager_setsEagerPageLoadStrategy() throws Exception {
        SafariOptions opts = safariOpts(PageLoadStrategyMode.Eager, PrivateMode.False);
        Object pageLoad = opts.getCapability("pageLoadStrategy");
        Assert.assertTrue(PageLoadStrategy.EAGER.equals(pageLoad)
                        || "eager".equalsIgnoreCase(String.valueOf(pageLoad)),
                "Safari must apply EAGER page load strategy, got: " + pageLoad);
    }

    @Test
    public void safari_normal_eagerStrategyNotApplied() throws Exception {
        Object pageLoad = safariOpts(PageLoadStrategyMode.Normal, PrivateMode.False)
                .getCapability("pageLoadStrategy");
        Assert.assertFalse(PageLoadStrategy.EAGER.equals(pageLoad)
                        || "eager".equalsIgnoreCase(String.valueOf(pageLoad)),
                "Normal mode must not set EAGER strategy");
    }

    @Test
    public void safari_privateTrue_doesNotThrow_noArgAdded() throws Exception {
        SafariOptions opts = safariOpts(PageLoadStrategyMode.Normal, PrivateMode.True);
        Assert.assertNotNull(opts,
                "SafariOptions must be returned even with PrivateMode.True (private mode is unsupported — logged, not thrown)");
    }

    @Test
    public void safari_userCap_survivesAfterMerge() throws Exception {
        MutableCapabilities userCaps = new MutableCapabilities();
        userCaps.setCapability("project:customCap", "customValue");
        SafariOptions merged = safariOpts(PageLoadStrategyMode.Normal, PrivateMode.False).merge(userCaps);
        Assert.assertEquals(merged.getCapability("project:customCap"), "customValue",
                "User capability must survive merge into SafariOptions");
    }

    // ── setupLocalDriver / setupRemoteDriver: unsupported type ────────────

    @Test(expectedExceptions = IllegalStateException.class)
    public void setupLocalDriver_mobileType_throwsBeforeBrowserLaunch() {
        // MobileDriverType.Android hits the default case → IllegalStateException
        // ChromeDriver/FirefoxDriver is never instantiated.
        BrowserSetUp.setupLocalDriver(
                MobileDriverType.Android, null,
                HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.False, SandboxMode.Sandbox, WebSecurityMode.SecureMode);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void setupRemoteDriver_mobileType_throwsBeforeBrowserLaunch() throws Exception {
        BrowserSetUp.setupRemoteDriver(
                MobileDriverType.Android,
                URI.create("http://localhost:4444").toURL(), null,
                HeadlessMode.False, PageLoadStrategyMode.Normal,
                PrivateMode.False, SandboxMode.Sandbox, WebSecurityMode.SecureMode);
    }
}
