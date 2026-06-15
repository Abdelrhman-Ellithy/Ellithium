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
public class OpenAICompatibleProviderTest {

    private OpenAICompatibleProvider provider;
    private HttpClient mockClient;

    @BeforeMethod
    public void setUp() throws Exception {
        provider = new OpenAICompatibleProvider("", "test-key", "");
        mockClient = Mockito.mock(HttpClient.class);
        Field f = OpenAICompatibleProvider.class.getDeclaredField("httpClient");
        f.setAccessible(true);
        f.set(provider, mockClient);
    }

    // ── Constructor defaults ──────────────────────────────────────────────

    @Test
    public void defaultBaseUrl_usesOpenAIEndpoint() throws Exception {
        Field f = OpenAICompatibleProvider.class.getDeclaredField("baseUrl");
        f.setAccessible(true);
        Assert.assertEquals(f.get(provider), "https://api.openai.com/v1");
    }

    @Test
    public void defaultModel_usesGpt4o() throws Exception {
        Field f = OpenAICompatibleProvider.class.getDeclaredField("model");
        f.setAccessible(true);
        Assert.assertEquals(f.get(provider), "gpt-4o");
    }

    @Test
    public void customBaseUrl_retained() throws Exception {
        OpenAICompatibleProvider p = new OpenAICompatibleProvider("https://my.groq/v1", "k", "llama3");
        Field f = OpenAICompatibleProvider.class.getDeclaredField("baseUrl");
        f.setAccessible(true);
        Assert.assertEquals(f.get(p), "https://my.groq/v1");
    }

    @Test
    public void customModel_retained() throws Exception {
        OpenAICompatibleProvider p = new OpenAICompatibleProvider("", "k", "deepseek-chat");
        Field f = OpenAICompatibleProvider.class.getDeclaredField("model");
        f.setAccessible(true);
        Assert.assertEquals(f.get(p), "deepseek-chat");
    }

    @Test
    public void supportsVision_returnsFalse() {
        Assert.assertFalse(provider.supportsVision());
    }

    @Test
    public void getModelName_returnsDefault() {
        Assert.assertEquals(provider.getModelName(), "gpt-4o");
    }

    // ── Response parsing ──────────────────────────────────────────────────

    @Test
    public void ask_successResponse_returnsContent() throws Exception {
        stubResponse(200, openAiBody("healed locator"));
        Assert.assertEquals(provider.ask("s", "u"), "healed locator");
    }

    @Test
    public void ask_emptyChoices_returnsNull() throws Exception {
        stubResponse(200, "{\"choices\": []}");
        Assert.assertNull(provider.ask("s", "u"));
    }

    @Test
    public void ask_missingMessageField_returnsNull() throws Exception {
        stubResponse(200, "{\"choices\": [{\"finish_reason\": \"stop\"}]}");
        Assert.assertNull(provider.ask("s", "u"));
    }

    @Test
    public void ask_emptyContent_returnsNull() throws Exception {
        stubResponse(200, "{\"choices\": [{\"message\": {\"content\": \"\"}}]}");
        Assert.assertNull(provider.ask("s", "u"));
    }

    @Test
    public void ask_http401_returnsNull() throws Exception {
        stubResponse(401, "{\"error\": {\"message\": \"invalid api key\"}}");
        Assert.assertNull(provider.ask("s", "u"));
    }

    @Test
    public void ask_http500_returnsNull() throws Exception {
        stubResponse(500, "{\"error\": \"server error\"}");
        Assert.assertNull(provider.ask("s", "u"));
    }

    @Test
    public void ask_markdownFencesStripped() throws Exception {
        stubResponse(200, openAiBody("```json\n{\"key\": 1}\n```"));
        String result = provider.ask("s", "u");
        Assert.assertNotNull(result);
        Assert.assertFalse(result.startsWith("```"));
    }

    @Test
    public void ask_networkException_returnsNull() throws Exception {
        Mockito.doThrow(new RuntimeException("connection refused")).when(mockClient)
                .send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class));
        Assert.assertNull(provider.ask("s", "u"));
    }

    // ── URL construction ──────────────────────────────────────────────────

    @Test
    public void ask_urlEndsWithChatCompletions() throws Exception {
        stubResponse(200, openAiBody("ok"));
        provider.ask("s", "u");
        Mockito.verify(mockClient).send(
                Mockito.argThat(req -> req.uri().toString().endsWith("/chat/completions")),
                Mockito.any());
    }

    @Test
    public void ask_authorizationHeaderSet() throws Exception {
        stubResponse(200, openAiBody("ok"));
        provider.ask("s", "u");
        Mockito.verify(mockClient).send(
                Mockito.argThat(req -> req.headers().firstValue("Authorization")
                        .map(v -> v.startsWith("Bearer ")).orElse(false)),
                Mockito.any());
    }

    @Test
    public void ask_customBaseUrl_noDoubleSlash() throws Exception {
        OpenAICompatibleProvider p = new OpenAICompatibleProvider("https://api.groq.com/openai/v1", "k", "m");
        Field f = OpenAICompatibleProvider.class.getDeclaredField("httpClient");
        f.setAccessible(true);
        HttpClient mc = Mockito.mock(HttpClient.class);
        f.set(p, mc);

        HttpResponse<String> resp = Mockito.mock(HttpResponse.class);
        Mockito.when(resp.statusCode()).thenReturn(200);
        Mockito.when(resp.body()).thenReturn(openAiBody("ok"));
        Mockito.doReturn(resp).when(mc).send(Mockito.any(), Mockito.any());

        p.ask("s", "u");

        Mockito.verify(mc).send(
                Mockito.argThat(req -> {
                    String uri = req.uri().toString();
                    return uri.endsWith("/chat/completions") && !uri.contains("//chat");
                }),
                Mockito.any());
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

    private static String openAiBody(String content) {
        String escaped = content
                .replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\""
                + escaped + "\"},\"finish_reason\":\"stop\"}]}";
    }
}
