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
public class AnthropicProviderTest {

    private AnthropicProvider provider;
    private HttpClient mockClient;

    @BeforeMethod
    public void setUp() throws Exception {
        provider = new AnthropicProvider("", "test-key", "");
        mockClient = Mockito.mock(HttpClient.class);
        Field f = AnthropicProvider.class.getDeclaredField("httpClient");
        f.setAccessible(true);
        f.set(provider, mockClient);
    }

    // ── Constructor defaults ──────────────────────────────────────────────

    @Test
    public void defaultBaseUrl_usesAnthropicEndpoint() throws Exception {
        Field f = AnthropicProvider.class.getDeclaredField("baseUrl");
        f.setAccessible(true);
        Assert.assertEquals(f.get(provider), "https://api.anthropic.com/v1");
    }

    @Test
    public void defaultModel_usesClaude35() throws Exception {
        Field f = AnthropicProvider.class.getDeclaredField("model");
        f.setAccessible(true);
        Assert.assertEquals(f.get(provider), "claude-3-5-sonnet-latest");
    }

    @Test
    public void customBaseUrl_isRetained() throws Exception {
        AnthropicProvider p = new AnthropicProvider("https://my.proxy/v1", "k", "claude-3");
        Field f = AnthropicProvider.class.getDeclaredField("baseUrl");
        f.setAccessible(true);
        Assert.assertEquals(f.get(p), "https://my.proxy/v1");
    }

    @Test
    public void customModel_isRetained() throws Exception {
        AnthropicProvider p = new AnthropicProvider("", "k", "claude-3-opus");
        Field f = AnthropicProvider.class.getDeclaredField("model");
        f.setAccessible(true);
        Assert.assertEquals(f.get(p), "claude-3-opus");
    }

    @Test
    public void supportsVision_returnsFalse() {
        Assert.assertFalse(provider.supportsVision());
    }

    @Test
    public void getModelName_returnsDefault() {
        Assert.assertEquals(provider.getModelName(), "claude-3-5-sonnet-latest");
    }

    // ── Response parsing ──────────────────────────────────────────────────

    @Test
    public void ask_successResponse_returnsTextContent() throws Exception {
        String body = """
            {
              "content": [
                {"type": "text", "text": "healed locator result"},
                {"type": "tool_use", "id": "ignored"}
              ]
            }
            """;
        stubResponse(200, body);
        String result = provider.ask("system", "user");
        Assert.assertEquals(result, "healed locator result");
    }

    @Test
    public void ask_multipleTextBlocks_concatenates() throws Exception {
        String body = """
            {
              "content": [
                {"type": "text", "text": "part one "},
                {"type": "text", "text": "part two"}
              ]
            }
            """;
        stubResponse(200, body);
        Assert.assertEquals(provider.ask("s", "u"), "part one part two");
    }

    @Test
    public void ask_noTextBlock_returnsNull() throws Exception {
        String body = """
            {
              "content": [
                {"type": "tool_use", "id": "abc"}
              ]
            }
            """;
        stubResponse(200, body);
        Assert.assertNull(provider.ask("s", "u"));
    }

    @Test
    public void ask_emptyContentArray_returnsNull() throws Exception {
        stubResponse(200, "{\"content\": []}");
        Assert.assertNull(provider.ask("s", "u"));
    }

    @Test
    public void ask_http4xx_returnsNull() throws Exception {
        stubResponse(401, "{\"error\": \"unauthorized\"}");
        Assert.assertNull(provider.ask("s", "u"));
    }

    @Test
    public void ask_http5xx_returnsNull() throws Exception {
        stubResponse(500, "{\"error\": \"server error\"}");
        Assert.assertNull(provider.ask("s", "u"));
    }

    @Test
    public void ask_markdownFencesStripped() throws Exception {
        String body = """
            {
              "content": [{"type": "text", "text": "```json\\n{\\"key\\": \\"value\\"}\\n```"}]
            }
            """;
        stubResponse(200, body);
        String result = provider.ask("s", "u");
        Assert.assertNotNull(result);
        Assert.assertFalse(result.startsWith("```"), "Markdown fences must be stripped");
    }

    @Test
    public void ask_networkException_returnsNull() throws Exception {
        Mockito.when(mockClient.send(Mockito.any(HttpRequest.class),
                Mockito.any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("connection refused"));
        Assert.assertNull(provider.ask("s", "u"));
    }

    @Test
    public void ask_urlAppended_messages() throws Exception {
        AnthropicProvider p = new AnthropicProvider("https://api.anthropic.com/v1", "k", "m");
        Field f = AnthropicProvider.class.getDeclaredField("httpClient");
        f.setAccessible(true);
        HttpClient mc = Mockito.mock(HttpClient.class);
        f.set(p, mc);

        HttpResponse<String> resp = Mockito.mock(HttpResponse.class);
        Mockito.when(resp.statusCode()).thenReturn(200);
        Mockito.when(resp.body()).thenReturn("{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}");
        Mockito.doReturn(resp).when(mc).send(Mockito.any(), Mockito.any());

        p.ask("s", "u");

        Mockito.verify(mc).send(
                Mockito.argThat(req -> req.uri().toString().endsWith("/messages")),
                Mockito.any());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    @SuppressWarnings("rawtypes")
    private void stubResponse(int status, String body) throws Exception {
        HttpResponse<String> resp = Mockito.mock(HttpResponse.class);
        Mockito.when(resp.statusCode()).thenReturn(status);
        Mockito.when(resp.body()).thenReturn(body);
        Mockito.doReturn(resp).when(mockClient).send(
                Mockito.any(HttpRequest.class),
                Mockito.any(HttpResponse.BodyHandler.class));
    }
}
