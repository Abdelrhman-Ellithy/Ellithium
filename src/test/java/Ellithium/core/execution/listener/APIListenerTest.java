package Ellithium.core.execution.listener;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

public class APIListenerTest {

    private final APIListener listener = new APIListener();

    // ── obfuscateData — plain text patterns ───────────────────────────────

    @Test
    public void obfuscateData_null_returnsNull() throws Exception {
        Assert.assertNull(invoke("obfuscateData", (Object) null));
    }

    @Test
    public void obfuscateData_email_masked() throws Exception {
        String result = (String) invoke("obfuscateData", "contact: user@example.com");
        Assert.assertFalse(result.contains("user@example.com"));
    }

    @Test
    public void obfuscateData_creditCard_masked() throws Exception {
        String result = (String) invoke("obfuscateData", "card: 4111 1111 1111 1111");
        Assert.assertFalse(result.contains("4111"));
    }

    @Test
    public void obfuscateData_ssn_masked() throws Exception {
        String result = (String) invoke("obfuscateData", "ssn: 123-45-6789");
        Assert.assertFalse(result.contains("123-45-6789"));
    }

    @Test
    public void obfuscateData_bearerToken_masked() throws Exception {
        String result = (String) invoke("obfuscateData", "Authorization: Bearer abc123token");
        Assert.assertFalse(result.contains("abc123token"));
        Assert.assertTrue(result.contains("Bearer"));
    }

    @Test
    public void obfuscateData_plainText_unchanged() throws Exception {
        String result = (String) invoke("obfuscateData", "hello world");
        Assert.assertEquals(result, "hello world");
    }

    // ── obfuscateData — JSON sensitive field masking ──────────────────────

    @Test
    public void obfuscateData_jsonWithPassword_masked() throws Exception {
        String json = "{\"name\":\"alice\",\"password\":\"s3cr3t\"}";
        String result = (String) invoke("obfuscateData", json);
        Assert.assertFalse(result.contains("s3cr3t"));
        Assert.assertTrue(result.contains("alice"));
    }

    @Test
    public void obfuscateData_jsonWithToken_masked() throws Exception {
        String json = "{\"token\":\"eyJhbGciOiJIUzI1NiJ9\"}";
        String result = (String) invoke("obfuscateData", json);
        Assert.assertFalse(result.contains("eyJhbGciOiJIUzI1NiJ9"));
    }

    @Test
    public void obfuscateData_jsonWithNonSensitiveField_preserved() throws Exception {
        String json = "{\"name\":\"Alice\",\"age\":30}";
        String result = (String) invoke("obfuscateData", json);
        Assert.assertTrue(result.contains("Alice"));
    }

    @Test
    public void obfuscateData_jsonArray_processesElements() throws Exception {
        String json = "[{\"password\":\"secret\"},{\"name\":\"Bob\"}]";
        String result = (String) invoke("obfuscateData", json);
        Assert.assertFalse(result.contains("secret"));
        Assert.assertTrue(result.contains("Bob"));
    }

    // ── handleCookies ─────────────────────────────────────────────────────

    @Test
    public void handleCookies_null_returnsNoCookies() throws Exception {
        String result = invokeHandleCookies(null);
        Assert.assertEquals(result, "No Cookies");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Object invoke(String methodName, Object arg) throws Exception {
        Method m = APIListener.class.getDeclaredMethod(methodName, String.class);
        m.setAccessible(true);
        return m.invoke(listener, arg);
    }

    private String invokeHandleCookies(Object arg) throws Exception {
        Method m = APIListener.class.getDeclaredMethod("handleCookies", Object.class);
        m.setAccessible(true);
        return (String) m.invoke(listener, arg);
    }
}
