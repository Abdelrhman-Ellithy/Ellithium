package Ellithium.core.ai.crypto;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

public class ModelDecryptorTest {

    @Test
    public void testIsModelResourcePresent() {
        boolean present = ModelDecryptor.isModelResourcePresent();
        // Encrypted model is bundled in src/main/resources
        Assert.assertTrue(present);
    }

    @Test
    public void testLoadTokenizer() {
        byte[] tokenizerBytes = ModelDecryptor.loadTokenizer();
        Assert.assertNotNull(tokenizerBytes);
        Assert.assertTrue(tokenizerBytes.length > 0);

        String json = new String(tokenizerBytes, StandardCharsets.UTF_8);
        Assert.assertTrue(json.contains("model") || json.contains("tokenizer") || json.contains("vocab"));
    }

    @Test
    public void testDecryptModel_gracefulHandling() {
        // Depending on OS architecture and native library presence, decryptModel returns byte[] or null gracefully
        byte[] model = ModelDecryptor.decryptModel();
        if (model != null) {
            Assert.assertTrue(model.length > 0);
        }
    }
}
