package ai;

import Ellithium.Utilities.ai.LLMProvider;
import Ellithium.core.ai.generators.AITestDataGenerator;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AITestDataGeneratorTest {

    @Test
    public void testGenerateFuzzingData_ParsesJsonArrayCorrectly() throws Exception {
        LLMProvider provider = mock(LLMProvider.class);
        
        // Mocking the LLM returning a valid JSON array of two elements
        String mockJsonResponse = "[\n" +
                "  {\"username\": \"admin' OR 1=1--\", \"age\": -1},\n" +
                "  {\"username\": \"<script>alert(1)</script>\", \"age\": 99999}\n" +
                "]";
        when(provider.ask(anyString(), anyString())).thenReturn(mockJsonResponse);

        List<String> payloads = AITestDataGenerator.generateFuzzingData("User: username(str), age(int)", 2, provider);

        Assert.assertEquals(payloads.size(), 2);
        Assert.assertTrue(payloads.get(0).contains("admin' OR 1=1--"));
        Assert.assertTrue(payloads.get(1).contains("<script>alert(1)</script>"));
        
        verify(provider, times(1)).ask(anyString(), anyString());
    }

    @Test
    public void testGenerateFuzzingData_HandlesInvalidJsonGracefully() throws Exception {
        LLMProvider provider = mock(LLMProvider.class);
        
        // Mocking an invalid LLM response
        when(provider.ask(anyString(), anyString())).thenReturn("I am an AI. Here is your data: {not an array}");

        List<String> payloads = AITestDataGenerator.generateFuzzingData("User", 2, provider);

        // It should catch the exception and return an empty list
        Assert.assertEquals(payloads.size(), 0);
    }

    @Test
    public void testGenerateFuzzingData_HandlesNullSchema() throws Exception {
        LLMProvider provider = mock(LLMProvider.class);

        // If schema is null, it should fail gracefully and return empty list
        List<String> payloads = AITestDataGenerator.generateFuzzingData(null, 5, provider);

        Assert.assertEquals(payloads.size(), 0);
        // Ensure we don't even waste LLM tokens if schema is null
        verify(provider, never()).ask(anyString(), anyString());
    }

    @Test
    public void testGenerateFuzzingData_HandlesZeroRecords() throws Exception {
        LLMProvider provider = mock(LLMProvider.class);

        List<String> payloads = AITestDataGenerator.generateFuzzingData("Schema", 0, provider);

        Assert.assertEquals(payloads.size(), 0);
        verify(provider, never()).ask(anyString(), anyString());
    }

    @Test
    public void testGenerateFuzzingData_HandlesLlmException() throws Exception {
        LLMProvider provider = mock(LLMProvider.class);
        when(provider.ask(anyString(), anyString())).thenThrow(new RuntimeException("Network Error"));

        List<String> payloads = AITestDataGenerator.generateFuzzingData("Schema", 2, provider);

        Assert.assertEquals(payloads.size(), 0);
    }
}
