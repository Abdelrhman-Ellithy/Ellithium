package Ellithium.core.ai;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Decrypts the Tier 3 ONNX model from an AES-256-GCM encrypted resource in the JAR.
 *
 * <h3>Encryption scheme</h3>
 * <ul>
 *   <li>Algorithm: AES-256-GCM (authenticated encryption — detects tampering)</li>
 *   <li>Key derivation: SHA-256(licenseKey + SALT) → 32-byte AES key</li>
 *   <li>Encrypted file layout: [12-byte IV][16-byte GCM auth tag (appended by JDK)][ciphertext]</li>
 *   <li>Resource path: {@code /ai-models/tier3.onnx.enc} inside the JAR</li>
 * </ul>
 *
 * <h3>Security properties</h3>
 * <ul>
 *   <li>Decrypted bytes are returned as an in-memory {@code byte[]} and NEVER written to disk.</li>
 *   <li>The salt is embedded here; the key is the user's license key — both are required to decrypt.</li>
 *   <li>GCM authentication fails immediately if the ciphertext has been tampered with.</li>
 * </ul>
 *
 * <p>When the Kaggle fine-tuned model is ready: encrypt it with {@code ModelEncryptionUtil}
 * (a separate offline CLI), place the {@code .enc} file under
 * {@code src/main/resources/ai-models/tier3.onnx.enc}, and it auto-bundles into the JAR.</p>
 */
public class ModelDecryptor {

    static final String MODEL_RESOURCE_PATH = "/ai-models/tier3.onnx.enc";
    private static final String TOKENIZER_RESOURCE_PATH = "/ai-models/tokenizer.json";
    private static final int    GCM_IV_LENGTH_BYTES  = 12;
    private static final int    GCM_TAG_LENGTH_BITS   = 128;

    // Embedded salt — pair with the user's license key for key derivation.
    // Must match the salt used by ModelEncryptionUtil when encrypting the model.
    private static final String SALT = "ellithium-tier3-model-v1";

    // ──────────────────────── Public API ────────────────────────

    /**
     * Decrypts the ONNX model resource and returns the raw bytes suitable for
     * {@code OrtEnvironment.createSession(byte[])}.
     *
     * @param licenseKey The user's Tier 3 license key (used as decryption key source)
     * @return Decrypted ONNX model bytes, or {@code null} if the resource is absent or
     *         decryption fails (wrong key, tampered file, or resource not yet embedded)
     */
    public static byte[] decryptModel(String licenseKey) {
        return decryptResource(MODEL_RESOURCE_PATH, licenseKey, "ONNX model");
    }

    /**
     * Decrypts the tokenizer JSON resource.
     *
     * @param licenseKey The user's Tier 3 license key
     * @return Decrypted tokenizer JSON bytes, or {@code null} if absent or decryption fails
     */
    public static byte[] decryptTokenizer(String licenseKey) {
        return decryptResource(TOKENIZER_RESOURCE_PATH, licenseKey, "tokenizer");
    }

    /**
     * Returns {@code true} when the encrypted model resource exists in the JAR.
     * Does NOT attempt decryption — safe to call during startup probing.
     */
    public static boolean isModelResourcePresent() {
        return ModelDecryptor.class.getResourceAsStream(MODEL_RESOURCE_PATH) != null;
    }

    // ──────────────────────── Core Decryption ────────────────────────

    private static byte[] decryptResource(String resourcePath, String licenseKey, String resourceName) {
        if (licenseKey == null || licenseKey.isBlank()) {
            Reporter.log("[TIER 2] ModelDecryptor: no license key — cannot decrypt " + resourceName, LogLevel.DEBUG);
            return null;
        }

        byte[] encrypted = loadResource(resourcePath);
        if (encrypted == null) {
            Reporter.log("[TIER 2] ModelDecryptor: resource not found in JAR: " + resourcePath, LogLevel.DEBUG);
            return null;
        }

        if (encrypted.length <= GCM_IV_LENGTH_BYTES) {
            Reporter.log("[TIER 2] ModelDecryptor: resource too short to be valid: " + resourcePath, LogLevel.WARN);
            return null;
        }

        try {
            byte[] key        = deriveKey(licenseKey);
            byte[] iv         = new byte[GCM_IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[encrypted.length - GCM_IV_LENGTH_BYTES];

            System.arraycopy(encrypted, 0, iv, 0, GCM_IV_LENGTH_BYTES);
            System.arraycopy(encrypted, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            Reporter.log("[TIER 2] ModelDecryptor: " + resourceName + " decrypted ("
                    + plaintext.length + " bytes)", LogLevel.INFO_GREEN);
            return plaintext;

        } catch (javax.crypto.AEADBadTagException e) {
            Reporter.log("[TIER 2] ModelDecryptor: GCM authentication failed for " + resourceName
                    + " — wrong license key or tampered file", LogLevel.ERROR);
            return null;
        } catch (Exception e) {
            Reporter.log("[TIER 2] ModelDecryptor: decryption error for " + resourceName
                    + ": " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    // ──────────────────────── Key Derivation ────────────────────────

    /**
     * Derives a 32-byte AES-256 key from the license key using SHA-256.
     * Key = SHA-256(licenseKey + "|" + SALT)
     */
    static byte[] deriveKey(String licenseKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update((licenseKey + "|" + SALT).getBytes(StandardCharsets.UTF_8));
        return digest.digest();
    }

    // ──────────────────────── Resource Loading ────────────────────────

    private static byte[] loadResource(String path) {
        try (InputStream is = ModelDecryptor.class.getResourceAsStream(path)) {
            if (is == null) return null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = is.read(chunk)) != -1) buffer.write(chunk, 0, read);
            return buffer.toByteArray();
        } catch (IOException e) {
            Reporter.log("[TIER 2] ModelDecryptor: failed to load resource " + path
                    + ": " + e.getMessage(), LogLevel.WARN);
            return null;
        }
    }
}
