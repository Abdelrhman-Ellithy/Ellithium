package Ellithium.core.ai.crypto;

import Ellithium.Utilities.helpers.JarExtractor;
import Ellithium.core.execution.Internal.Loader.StartUpLoader;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;

import java.io.*;
import java.nio.file.Files;
import java.util.jar.JarFile;

public class ModelDecryptor {

    static final String  MODEL_RESOURCE_PATH     = "/Ellithium-ai-model/model_quantized.onnx.enc";
    private static final String TOKENIZER_RESOURCE_PATH = "/Ellithium-ai-model/tokenizer.json";
    private static final String NATIVE_BASE_NAME        = "ellcrypto";
    private static final String NATIVE_SUBDIR           = "native";

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
            Reporter.log("[TIER 2] ModelDecryptor: model decrypted (" + plaintext.length + " bytes)", LogLevel.INFO_GREEN);
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
        File jarFile = StartUpLoader.findJarFile();
        if (jarFile != null && jarFile.exists()) {
            File extracted = new File(jarFile.getParentFile(),
                    MODEL_RESOURCE_PATH.substring(1).replace('/', File.separatorChar));
            if (extracted.exists()) return true;
            try (JarFile jar = new JarFile(jarFile)) {
                if (jar.getJarEntry(MODEL_RESOURCE_PATH.substring(1)) != null) return true;
            } catch (IOException ignored) {}
        }
        return ModelDecryptor.class.getResourceAsStream(MODEL_RESOURCE_PATH) != null;
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

        String libFileName  = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        String jarEntryName = resourcePath.substring(1);

        File jarFile = StartUpLoader.findJarFile();
        if (jarFile != null && jarFile.exists()) {
            File libFile = new File(new File(jarFile.getParentFile(), NATIVE_SUBDIR), libFileName);
            if (!libFile.exists()) {
                JarExtractor.extractFileFromJar(jarFile, jarEntryName, libFile);
            }
            if (libFile.exists()) {
                return loadLibraryFile(libFile);
            }
        }

        byte[] libBytes = loadResource(resourcePath);
        if (libBytes == null) {
            Reporter.log("[TIER 2] ModelDecryptor: native library not found: " + resourcePath, LogLevel.WARN);
            return false;
        }
        try {
            String suffix = resourcePath.substring(resourcePath.lastIndexOf('.'));
            java.nio.file.Path tmp = Files.createTempFile(NATIVE_BASE_NAME, suffix);
            tmp.toFile().deleteOnExit();
            // Restrict to owner-read-only before writing, so other processes on a shared
            // system (e.g., shared Linux CI runner) cannot read the native library.
            try {
                java.nio.file.attribute.PosixFileAttributeView view =
                        Files.getFileAttributeView(tmp, java.nio.file.attribute.PosixFileAttributeView.class);
                if (view != null) {
                    view.setPermissions(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
                }
            } catch (Exception ignored) {} // Windows: no POSIX attributes; silently skip
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
            Reporter.log("[TIER 2] ModelDecryptor: native library loaded from " + libFile.getAbsolutePath(), LogLevel.INFO_GREEN);
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
        String entryName = path.startsWith("/") ? path.substring(1) : path;

        File jarFile = StartUpLoader.findJarFile();
        if (jarFile != null && jarFile.exists()) {
            File extracted = new File(jarFile.getParentFile(), entryName.replace('/', File.separatorChar));
            if (!extracted.exists()) {
                JarExtractor.extractFileFromJar(jarFile, entryName, extracted);
            }
            if (extracted.exists()) {
                try {
                    return Files.readAllBytes(extracted.toPath());
                } catch (IOException e) {
                    Reporter.log("[TIER 2] ModelDecryptor: failed to read " + extracted.getAbsolutePath()
                            + ": " + e.getMessage(), LogLevel.WARN);
                }
            }
        }

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
