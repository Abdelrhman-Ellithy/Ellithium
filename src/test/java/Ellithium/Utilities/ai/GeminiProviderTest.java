package Ellithium.Utilities.ai;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@SuppressWarnings("unchecked")
public class GeminiProviderTest {

    private GeminiProvider provider;
    private HttpClient mockClient;

    @BeforeMethod
    public void setUp() throws Exception {
        provider = new GeminiProvider("", "test-key", "");
        mockClient = Mockito.mock(HttpClient.class);
        Field f = GeminiProvider.class.getDeclaredField("httpClient");
        f.setAccessible(true);
        f.set(provider, mockClient);
    }

    // ── Constructor defaults ──────────────────────────────────────────────

    @Test
    public void defaultBaseUrl_usesGoogleEndpoint() throws Exception {
        Field f = GeminiProvider.class.getDeclaredField("baseUrl");
        f.setAccessible(true);
        Assert.assertEquals(f.get(provider), "https://generativelanguage.googleapis.com/v1beta/");
    }

    @Test
    public void defaultModel_usesGeminiFlash() throws Exception {
        Field f = GeminiProvider.class.getDeclaredField("model");
        f.setAccessible(true);
        Assert.assertEquals(f.get(provider), "gemini-1.5-flash");
    }

    @Test
    public void customModel_isRetained() throws Exception {
        GeminiProvider p = new GeminiProvider("", "k", "gemini-2.0-pro");
        Field f = GeminiProvider.class.getDeclaredField("model");
        f.setAccessible(true);
        Assert.assertEquals(f.get(p), "gemini-2.0-pro");
    }

    @Test
    public void supportsVision_returnsTrue() {
        Assert.assertTrue(provider.supportsVision());
    }

    @Test
    public void getModelName_returnsDefault() {
        Assert.assertEquals(provider.getModelName(), "gemini-1.5-flash");
    }

    // ── Response parsing — ask() ──────────────────────────────────────────

    @Test
    public void ask_successResponse_returnsText() throws Exception {
        stubResponse(200, geminiBody("Hello from Gemini"));
        Assert.assertEquals(provider.ask("s", "u"), "Hello from Gemini");
    }

    @Test
    public void ask_multipleTextParts_concatenates() throws Exception {
        String body = """
            {
              "candidates": [{
                "content": {
                  "parts": [
                    {"text": "part A "},
                    {"text": "part B"}
                  ]
                }
              }]
            }
            """;
        stubResponse(200, body);
        Assert.assertEquals(provider.ask("s", "u"), "part A part B");
    }

    @Test
    public void ask_finishReasonNotStop_returnsNull() throws Exception {
        String body = """
            {
              "candidates": [{
                "finishReason": "SAFETY",
                "content": {"parts": []}
              }]
            }
            """;
        stubResponse(200, body);
        Assert.assertNull(provider.ask("s", "u"));
    }

    @Test
    public void ask_emptyCandidates_returnsNull() throws Exception {
        stubResponse(200, "{\"candidates\": []}");
        Assert.assertNull(provider.ask("s", "u"));
    }

    @Test
    public void ask_http4xx_returnsNull() throws Exception {
        stubResponse(403, "{\"error\": \"forbidden\"}");
        Assert.assertNull(provider.ask("s", "u"));
    }

    @Test
    public void ask_markdownFencesStripped() throws Exception {
        stubResponse(200, geminiBody("```json\n{\"key\": 1}\n```"));
        String result = provider.ask("s", "u");
        Assert.assertNotNull(result);
        Assert.assertFalse(result.startsWith("```"));
    }

    @Test
    public void ask_networkException_returnsNull() throws Exception {
        Mockito.doThrow(new RuntimeException("timeout")).when(mockClient)
                .send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class));
        Assert.assertNull(provider.ask("s", "u"));
    }

    // ── URL construction ──────────────────────────────────────────────────

    @Test
    public void ask_urlContainsGenerateContent() throws Exception {
        stubResponse(200, geminiBody("ok"));
        provider.ask("s", "u");
        Mockito.verify(mockClient).send(
                Mockito.argThat(req -> req.uri().toString().contains(":generateContent")),
                Mockito.any());
    }

    @Test
    public void ask_modelPathPrefixedWithModels() throws Exception {
        GeminiProvider p = new GeminiProvider("", "k", "gemini-flash");
        Field f = GeminiProvider.class.getDeclaredField("httpClient");
        f.setAccessible(true);
        HttpClient mc = Mockito.mock(HttpClient.class);
        f.set(p, mc);
        HttpResponse<String> resp = Mockito.mock(HttpResponse.class);
        Mockito.when(resp.statusCode()).thenReturn(200);
        Mockito.when(resp.body()).thenReturn(geminiBody("ok"));
        Mockito.doReturn(resp).when(mc).send(Mockito.any(), Mockito.any());

        p.ask("s", "u");

        Mockito.verify(mc).send(
                Mockito.argThat(req -> req.uri().toString().contains("models/gemini-flash")),
                Mockito.any());
    }

    // ── askWithVision ─────────────────────────────────────────────────────

    @Test
    public void askWithVision_successResponse_returnsText() throws Exception {
        stubResponse(200, geminiBody("vision result"));
        String result = provider.askWithVision("describe this", new byte[]{1, 2, 3});
        Assert.assertEquals(result, "vision result");
    }

    @Test
    public void askWithVision_http5xx_returnsNull() throws Exception {
        stubResponse(500, "{\"error\": \"server error\"}");
        Assert.assertNull(provider.askWithVision("prompt", new byte[]{1}));
    }

    @Test
    public void askWithVision_finishReasonSafety_returnsNull() throws Exception {
        String body = """
            {
              "candidates": [{
                "finishReason": "SAFETY",
                "content": {"parts": []}
              }]
            }
            """;
        stubResponse(200, body);
        Assert.assertNull(provider.askWithVision("prompt", new byte[]{1}));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void stubResponse(int status, String body) throws Exception {
        HttpResponse<String> resp = Mockito.mock(HttpResponse.class);
        Mockito.when(resp.statusCode()).thenReturn(status);
        Mockito.when(resp.body()).thenReturn(body);
        Mockito.doReturn(resp).when(mockClient).send(
                Mockito.any(HttpRequest.class),
                Mockito.any(HttpResponse.BodyHandler.class));
    }

    private static String geminiBody(String text) {
        String escaped = text
                .replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + escaped
                + "\"}]},\"finishReason\":\"STOP\"}]}";
    }
}
