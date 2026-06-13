package Ellithium.core.ai.crypto;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ModelDecryptor {

    static final String  MODEL_RESOURCE_PATH     = "/Ellithium-ai-model/model_quantized.onnx.enc";
    private static final String TOKENIZER_RESOURCE_PATH = "/Ellithium-ai-model/tokenizer.json";
    private static final String NATIVE_BASE_NAME        = "ellcrypto";

    private static volatile boolean nativeLoaded    = false;
    private static volatile boolean nativeAttempted = false;

    private static native byte[] nativeDecrypt(byte[] enc);

    public static byte[] decryptModel() {
        if (!ensureNativeLoaded()) {
            Reporter.log("[TIER 2] ModelDecryptor: native crypto library unavailable", LogLevel.WARN);
            return null;
        }
        byte[] encrypted = loadResource(MODEL_RESOURCE_PATH);
        if (encrypted == null) {
            Reporter.log("[TIER 2] ModelDecryptor: encrypted model not found", LogLevel.DEBUG);
            return null;
        }
        try {
            byte[] plaintext = nativeDecrypt(encrypted);
            if (plaintext == null || plaintext.length == 0) {
                Reporter.log("[TIER 2] ModelDecryptor: native decrypt returned no data", LogLevel.ERROR);
                return null;
            }
            return plaintext;
        } catch (Throwable t) {
            Reporter.log("[TIER 2] ModelDecryptor: native decrypt failed: " + t.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    public static byte[] loadTokenizer() {
        return loadResource(TOKENIZER_RESOURCE_PATH);
    }

    public static boolean isModelResourcePresent() {
        try (java.io.InputStream is = ModelDecryptor.class.getResourceAsStream(MODEL_RESOURCE_PATH)) {
            return is != null;
        } catch (java.io.IOException ignored) {
            return false;
        }
    }

    private static synchronized boolean ensureNativeLoaded() {
        if (nativeLoaded)    return true;
        if (nativeAttempted) return false;
        nativeAttempted = true;

        String resourcePath = nativeResourcePath();
        if (resourcePath == null) {
            Reporter.log("[TIER 2] ModelDecryptor: unsupported platform — "
                    + System.getProperty("os.name") + " / " + System.getProperty("os.arch"), LogLevel.WARN);
            return false;
        }

        byte[] libBytes = loadResource(resourcePath);
        if (libBytes == null) {
            Reporter.log("[TIER 2] ModelDecryptor: native library not found: " + resourcePath, LogLevel.WARN);
            return false;
        }
        try {
            String suffix = resourcePath.substring(resourcePath.lastIndexOf('.'));
            java.nio.file.Path tmp = Files.createTempFile(NATIVE_BASE_NAME, suffix);
            Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name("ell-native-cleanup").unstarted(() -> {
                try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            }));
            try {
                java.nio.file.attribute.PosixFileAttributeView posix =
                        Files.getFileAttributeView(tmp, java.nio.file.attribute.PosixFileAttributeView.class);
                if (posix != null) {
                    posix.setPermissions(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
                }
            } catch (Exception ignored) {}
            try {
                java.nio.file.attribute.AclFileAttributeView acl =
                        Files.getFileAttributeView(tmp, java.nio.file.attribute.AclFileAttributeView.class);
                if (acl != null) {
                    java.nio.file.attribute.UserPrincipal owner = acl.getOwner();
                    java.nio.file.attribute.AclEntry ownerOnly = java.nio.file.attribute.AclEntry.newBuilder()
                            .setType(java.nio.file.attribute.AclEntryType.ALLOW)
                            .setPrincipal(owner)
                            .setPermissions(java.util.EnumSet.allOf(java.nio.file.attribute.AclEntryPermission.class))
                            .build();
                    acl.setAcl(java.util.List.of(ownerOnly));
                }
            } catch (Exception ignored) {}
            Files.write(tmp, libBytes);
            return loadLibraryFile(tmp.toFile());
        } catch (IOException e) {
            Reporter.log("[TIER 2] ModelDecryptor: failed to write temp native library: " + e.getMessage(), LogLevel.WARN);
            return false;
        }
    }

    private static boolean loadLibraryFile(File libFile) {
        try {
            System.load(libFile.getAbsolutePath());
            nativeLoaded = true;
            Reporter.log("[TIER 2] ModelDecryptor: native library loaded", LogLevel.DEBUG);
            return true;
        } catch (Throwable t) {
            Reporter.log("[TIER 2] ModelDecryptor: System.load failed: " + t.getMessage(), LogLevel.WARN);
            return false;
        }
    }

    private static String nativeResourcePath() {
        String os   = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        String osTag, ext;
        if      (os.contains("win"))                           { osTag = "windows"; ext = "dll";   }
        else if (os.contains("mac") || os.contains("darwin")) { osTag = "macos";   ext = "dylib"; }
        else if (os.contains("nux") || os.contains("nix"))    { osTag = "linux";   ext = "so";    }
        else return null;

        String archTag;
        if      (arch.contains("aarch64") || arch.contains("arm64"))                              archTag = "arm64";
        else if (arch.contains("amd64")   || arch.contains("x86_64") || arch.contains("x64"))    archTag = "x64";
        else return null;

        return "/native/" + NATIVE_BASE_NAME + "-" + osTag + "-" + archTag + "." + ext;
    }

    private static byte[] loadResource(String path) {
        try (InputStream is = ModelDecryptor.class.getResourceAsStream(path)) {
            if (is == null) return null;
            return readAllBytes(is);
        } catch (IOException e) {
            Reporter.log("[TIER 2] ModelDecryptor: classpath read failed for " + path + ": " + e.getMessage(), LogLevel.WARN);
            return null;
        }
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = is.read(chunk)) != -1) buf.write(chunk, 0, n);
        return buf.toByteArray();
    }
}
