package Ellithium.core.execution.listener;

import org.testng.Assert;
import org.testng.annotations.Test;

public class APIListenerTest {

    private final APIListener listener = new APIListener();

    // ── obfuscateData — plain text patterns ───────────────────────────────

    @Test
    public void obfuscateData_null_returnsNull() {
        Assert.assertNull(listener.obfuscateData(null));
    }

    @Test
    public void obfuscateData_email_masked() {
        String result = listener.obfuscateData("contact: user@example.com");
        Assert.assertFalse(result.contains("user@example.com"));
    }

    @Test
    public void obfuscateData_creditCard_masked() {
        String result = listener.obfuscateData("card: 4111 1111 1111 1111");
        Assert.assertFalse(result.contains("4111"));
    }

    @Test
    public void obfuscateData_ssn_masked() {
        String result = listener.obfuscateData("ssn: 123-45-6789");
        Assert.assertFalse(result.contains("123-45-6789"));
    }

    @Test
    public void obfuscateData_bearerToken_masked() {
        String result = listener.obfuscateData("Authorization: Bearer abc123token");
        Assert.assertFalse(result.contains("abc123token"));
        Assert.assertTrue(result.contains("Bearer"));
    }

    @Test
    public void obfuscateData_plainText_unchanged() {
        String result = listener.obfuscateData("hello world");
        Assert.assertEquals(result, "hello world");
    }

    // ── obfuscateData — JSON sensitive field masking ──────────────────────

    @Test
    public void obfuscateData_jsonWithPassword_masked() {
        String json = "{\"name\":\"alice\",\"password\":\"s3cr3t\"}";
        String result = listener.obfuscateData(json);
        Assert.assertFalse(result.contains("s3cr3t"));
        Assert.assertTrue(result.contains("alice"));
    }

    @Test
    public void obfuscateData_jsonWithToken_masked() {
        String json = "{\"token\":\"eyJhbGciOiJIUzI1NiJ9\"}";
        String result = listener.obfuscateData(json);
        Assert.assertFalse(result.contains("eyJhbGciOiJIUzI1NiJ9"));
    }

    @Test
    public void obfuscateData_jsonWithNonSensitiveField_preserved() {
        String json = "{\"name\":\"Alice\",\"age\":30}";
        String result = listener.obfuscateData(json);
        Assert.assertTrue(result.contains("Alice"));
    }

    @Test
    public void obfuscateData_jsonArray_processesElements() {
        String json = "[{\"password\":\"secret\"},{\"name\":\"Bob\"}]";
        String result = listener.obfuscateData(json);
        Assert.assertFalse(result.contains("secret"));
        Assert.assertTrue(result.contains("Bob"));
    }

    // ── obfuscateUri — query-string parameter masking ─────────────────────

    @Test
    public void obfuscateUri_apiKeyQueryParam_masked() {
        String result = listener.obfuscateUri("https://api.example.com/data?api_key=abcd1234");
        Assert.assertFalse(result.contains("abcd1234"));
        Assert.assertTrue(result.contains("api_key="));
    }

    @Test
    public void obfuscateUri_tokenQueryParam_masked() {
        String result = listener.obfuscateUri("https://api.example.com/data?token=secretTokenValue");
        Assert.assertFalse(result.contains("secretTokenValue"));
    }

    @Test
    public void obfuscateUri_multipleParams_onlySensitiveMasked() {
        String result = listener.obfuscateUri("https://api.example.com/data?page=2&api_key=abcd1234");
        Assert.assertTrue(result.contains("page=2"));
        Assert.assertFalse(result.contains("abcd1234"));
    }

    @Test
    public void obfuscateUri_noQueryString_unchanged() {
        String result = listener.obfuscateUri("https://api.example.com/data");
        Assert.assertEquals(result, "https://api.example.com/data");
    }

    @Test
    public void obfuscateUri_null_returnsNull() {
        Assert.assertNull(listener.obfuscateUri(null));
    }

    @Test
    public void obfuscateData_ipAddress_masked() {
        String result = listener.obfuscateData("server IP is 192.168.1.100 and dns 10.0.0.1");
        Assert.assertFalse(result.contains("192.168.1.100"));
        Assert.assertTrue(result.contains("***.***.***.***"));
    }

    @Test
    public void obfuscateData_phoneNumber_masked() {
        String result = listener.obfuscateData("call me at +1 5551234567890");
        Assert.assertFalse(result.contains("5551234567890"));
    }

    // ── handleCookies ─────────────────────────────────────────────────────

    @Test
    public void handleCookies_null_returnsNoCookies() {
        Assert.assertEquals(listener.handleCookies(null), "No Cookies");
    }

    @Test
    public void handleCookies_emptyMap_returnsNoCookies() {
        Assert.assertEquals(listener.handleCookies(java.util.Collections.emptyMap()), "No Cookies");
    }

    @Test
    public void handleCookies_mapWithSensitive_masked() {
        java.util.Map<String, String> cookies = new java.util.HashMap<>();
        cookies.put("session", "session_secret_123");
        cookies.put("theme", "dark");
        String result = listener.handleCookies(cookies);
        Assert.assertFalse(result.contains("session_secret_123"));
        Assert.assertTrue(result.contains("dark"));
    }

    @Test
    public void handleCookies_cookiesObject_masked() {
        io.restassured.http.Cookie c1 = new io.restassured.http.Cookie.Builder("token", "secretToken").build();
        io.restassured.http.Cookie c2 = new io.restassured.http.Cookie.Builder("lang", "en").build();
        io.restassured.http.Cookies cookies = new io.restassured.http.Cookies(c1, c2);
        String result = listener.handleCookies(cookies);
        Assert.assertFalse(result.contains("secretToken"));
        Assert.assertTrue(result.contains("en"));
    }

    @Test
    public void handleCookies_emptyCookiesObject_returnsNoCookies() {
        io.restassured.http.Cookies cookies = new io.restassured.http.Cookies();
        Assert.assertEquals(listener.handleCookies(cookies), "No Cookies");
    }

    // ── mapToJson & headersToJson ──────────────────────────────────────────

    @Test
    public void testMapToJson_nullOrEmpty() {
        Assert.assertEquals(listener.mapToJson(null).length(), 0);
        Assert.assertEquals(listener.mapToJson(java.util.Collections.emptyMap()).length(), 0);
    }

    @Test
    public void testHeadersToJson_masksSensitiveHeaders() {
        io.restassured.http.Header h1 = new io.restassured.http.Header("Authorization", "Bearer token123");
        io.restassured.http.Header h2 = new io.restassured.http.Header("Content-Type", "application/json");
        io.restassured.http.Headers headers = new io.restassured.http.Headers(h1, h2);
        org.json.JSONObject json = listener.headersToJson(headers);
        Assert.assertEquals(json.getString("Authorization"), "********");
        Assert.assertEquals(json.getString("Content-Type"), "application/json");
    }

    @Test
    public void testGetSensitiveFields_unmodifiable() {
        java.util.Set<String> fields = APIListener.getSensitiveFields();
        Assert.assertNotNull(fields);
        Assert.assertTrue(fields.contains("password"));
        Assert.assertTrue(fields.contains("token"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> fields.add("newField"));
    }
}
