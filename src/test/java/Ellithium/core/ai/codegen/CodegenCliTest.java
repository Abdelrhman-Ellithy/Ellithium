package Ellithium.core.ai.codegen;

import Ellithium.core.driver.LocalDriverType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CodegenCliTest {

    @Test
    public void sanitizeClassName_null_returnsDefault() {
        Assert.assertEquals(CodegenCli.sanitizeClassName(null), "RecordedPage");
    }

    @Test
    public void sanitizeClassName_blank_returnsDefault() {
        Assert.assertEquals(CodegenCli.sanitizeClassName("   "), "RecordedPage");
    }

    @Test
    public void sanitizeClassName_allSpecialChars_returnsDefault() {
        Assert.assertEquals(CodegenCli.sanitizeClassName("!!!---???"), "RecordedPage");
    }

    @Test
    public void sanitizeClassName_digitLeading_prefixedWithPage() {
        String result = CodegenCli.sanitizeClassName("123Dashboard");
        Assert.assertTrue(result.startsWith("Page"), result);
    }

    @Test
    public void sanitizeClassName_normal_capitalizesFirst() {
        Assert.assertEquals(CodegenCli.sanitizeClassName("loginPage"), "LoginPage");
    }

    @Test
    public void sanitizeClassName_stripsNonAlphanumeric() {
        Assert.assertEquals(CodegenCli.sanitizeClassName("login-page"), "Loginpage");
    }

    @Test
    public void deriveClassName_pathSegment_usedAsName() {
        String result = CodegenCli.deriveClassName("https://app.example.com/dashboard");
        Assert.assertEquals(result, "DashboardPage");
    }

    @Test
    public void deriveClassName_rootPath_usesHost() {
        String result = CodegenCli.deriveClassName("https://app.example.com/");
        Assert.assertTrue(result.endsWith("Page"), result);
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void deriveClassName_null_returnsRecordedPage() {
        Assert.assertEquals(CodegenCli.deriveClassName(null), "RecordedPage");
    }

    @Test
    public void deriveClassName_malformedUrl_returnsRecordedPage() {
        Assert.assertEquals(CodegenCli.deriveClassName("not a url!!!"), "RecordedPage");
    }

    @Test
    public void deriveClassName_digitLeadingSegment_prefixed() {
        String result = CodegenCli.deriveClassName("https://app.com/404");
        Assert.assertTrue(result.startsWith("Page"), result);
    }

    @Test
    public void browserOf_edge_mapsCorrectly() throws Exception {
        Assert.assertEquals(invokeBrowserOf("edge"), LocalDriverType.Edge);
    }

    @Test
    public void browserOf_firefox_mapsCorrectly() throws Exception {
        Assert.assertEquals(invokeBrowserOf("firefox"), LocalDriverType.FireFox);
    }

    @Test
    public void browserOf_safari_mapsCorrectly() throws Exception {
        Assert.assertEquals(invokeBrowserOf("safari"), LocalDriverType.Safari);
    }

    @Test
    public void browserOf_chrome_isDefault() throws Exception {
        Assert.assertEquals(invokeBrowserOf("chrome"), LocalDriverType.Chrome);
        Assert.assertEquals(invokeBrowserOf("unknown"), LocalDriverType.Chrome);
    }

    @Test
    public void browserOf_caseInsensitive() throws Exception {
        Assert.assertEquals(invokeBrowserOf("FIREFOX"), LocalDriverType.FireFox);
        Assert.assertEquals(invokeBrowserOf("Edge"), LocalDriverType.Edge);
    }

    @Test
    public void parse_extractsUrl() throws Exception {
        Map<String, String> flags = new HashMap<>();
        String url = invokeParse(new String[]{"https://example.com"}, flags);
        Assert.assertEquals(url, "https://example.com");
        Assert.assertTrue(flags.isEmpty());
    }

    @Test
    public void parse_extractsValueFlags() throws Exception {
        Map<String, String> flags = new HashMap<>();
        invokeParse(new String[]{"--browser", "chrome", "--class", "LoginPage"}, flags);
        Assert.assertEquals(flags.get("browser"), "chrome");
        Assert.assertEquals(flags.get("class"), "LoginPage");
    }

    @Test
    public void parse_extractsBooleanFlags() throws Exception {
        Map<String, String> flags = new HashMap<>();
        invokeParse(new String[]{"--headless", "--llm-polish"}, flags);
        Assert.assertEquals(flags.get("headless"), "true");
        Assert.assertEquals(flags.get("llm-polish"), "true");
    }

    @Test
    public void parse_skipsCodegenKeyword() throws Exception {
        Map<String, String> flags = new HashMap<>();
        String url = invokeParse(new String[]{"codegen", "https://example.com"}, flags);
        Assert.assertEquals(url, "https://example.com");
    }

    @Test
    public void parse_urlAndFlagsTogether() throws Exception {
        Map<String, String> flags = new HashMap<>();
        String url = invokeParse(new String[]{"https://app.com", "--package", "pages", "--headless"}, flags);
        Assert.assertEquals(url, "https://app.com");
        Assert.assertEquals(flags.get("package"), "pages");
        Assert.assertEquals(flags.get("headless"), "true");
    }

    private static LocalDriverType invokeBrowserOf(String name) throws Exception {
        Method m = CodegenCli.class.getDeclaredMethod("browserOf", String.class);
        m.setAccessible(true);
        return (LocalDriverType) m.invoke(null, name);
    }

    @SuppressWarnings("unchecked")
    private static String invokeParse(String[] args, Map<String, String> flags) throws Exception {
        Method m = CodegenCli.class.getDeclaredMethod("parse", String[].class, Map.class);
        m.setAccessible(true);
        return (String) m.invoke(null, args, flags);
    }
}
