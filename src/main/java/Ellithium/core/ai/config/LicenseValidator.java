package Ellithium.core.ai.config;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Hybrid license validator for Ellithium Tier 3 (paid feature).
 *
 * <h3>Validation flow</h3>
 * <ol>
 *   <li>Try online validation via POST to the Ellithium licensing REST API.</li>
 *   <li>On HTTP 4xx (invalid/expired key): reject immediately — no offline fallback.</li>
 *   <li>On network error or timeout: fall back to the offline signed JWT in the license key.</li>
 *   <li>Offline fallback enforces a 7-day grace period from the last successful online
 *       validation (stored in {@code ~/.ellithium/license-cache.json}).</li>
 * </ol>
 *
 * <h3>JWT structure (offline token)</h3>
 * <pre>
 * Header: {"alg":"RS256","typ":"JWT"}
 * Payload: {"tier3":true,"exp":<unix_epoch>,"org":"Acme Corp","sub":"<licenseKey>"}
 * Signature: RSA-SHA256 with embedded public key
 * </pre>
 *
 * <p>The RSA public key is embedded in this class. The matching private key is held
 * exclusively by the Ellithium licensing server — no user ever sees it.</p>
 */
public class LicenseValidator {

    private static final String LICENSING_API_URL = "https://license.ellithium.io/v1/validate";

    // Replace with actual RSA-2048 public key PEM when Tier 3 is ready to ship.
    // The matching private key is held exclusively by the Ellithium licensing server.
    private static final String EMBEDDED_PUBLIC_KEY_PEM = null;
    private static final int CONNECT_TIMEOUT_MS   = 4_000;
    private static final int READ_TIMEOUT_MS       = 6_000;
    private static final long GRACE_PERIOD_DAYS    = 7;
    private static final String CACHE_FILE_PATH    =
            System.getProperty("user.home") + File.separator + ".ellithium"
            + File.separator + "license-cache.json";

    private static final Gson GSON = new Gson();

    // ── In-memory validation cache (per JVM lifetime) ──────────────────────────────────
    private static volatile ValidationResult cachedResult = null;

    // ──────────────────────── Public API ────────────────────────

    /**
     * Returns true when Tier 3 is licensed and active.
     * Result is cached for the JVM lifetime after the first call.
     */
    public static boolean isTier3Enabled() {
        if (cachedResult != null) return cachedResult.tier3Enabled;
        cachedResult = validate(AIConfigLoader.getLicenseKey());
        return cachedResult != null && cachedResult.tier3Enabled;
    }

    /** Returns the license expiry instant, or {@link Instant#EPOCH} if unknown. */
    public static Instant getExpiry() {
        if (cachedResult == null) isTier3Enabled();
        return cachedResult != null && cachedResult.expiry != null
                ? cachedResult.expiry : Instant.EPOCH;
    }

    /** Returns the licensed organization name, or empty string if unknown. */
    public static String getOrganization() {
        if (cachedResult == null) isTier3Enabled();
        return cachedResult != null && cachedResult.org != null ? cachedResult.org : "";
    }

    /** Clears the in-memory cache (for testing / hot-reload scenarios). */
    public static void reset() { cachedResult = null; }

    // ──────────────────────── Validation Logic ────────────────────────

    private static ValidationResult validate(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            Reporter.log("[LICENSE] No license key configured — Tier 3 disabled", LogLevel.DEBUG);
            return ValidationResult.disabled();
        }

        // 1. Try online validation first
        try {
            ValidationResult online = validateOnline(licenseKey);
            if (online != null) {
                if (online.tier3Enabled) {
                    persistGraceCache(licenseKey);
                    Reporter.log("[LICENSE] Online validation OK — Tier 3 active | org=" + online.org
                            + " expires=" + online.expiry, LogLevel.INFO_GREEN);
                } else {
                    Reporter.log("[LICENSE] License rejected by server — Tier 3 disabled", LogLevel.WARN);
                }
                return online;
            }
        } catch (LicenseRejectedException e) {
            Reporter.log("[LICENSE] License key rejected (HTTP 4xx): " + e.getMessage()
                    + " — Tier 3 disabled (no offline fallback)", LogLevel.WARN);
            return ValidationResult.disabled();
        } catch (Exception networkErr) {
            Reporter.log("[LICENSE] Online validation unreachable: " + networkErr.getMessage()
                    + " — falling back to offline JWT", LogLevel.WARN);
        }

        // 2. Offline JWT fallback
        try {
            ValidationResult offline = validateOfflineJwt(licenseKey);
            if (offline != null && offline.tier3Enabled) {
                if (isWithinGracePeriod(licenseKey)) {
                    Reporter.log("[LICENSE] Offline JWT valid + within grace period — Tier 3 active | org="
                            + offline.org, LogLevel.INFO_YELLOW);
                    return offline;
                } else {
                    Reporter.log("[LICENSE] Offline JWT valid but grace period expired (>"
                            + GRACE_PERIOD_DAYS + " days since last online validation) — Tier 3 disabled",
                            LogLevel.WARN);
                    return ValidationResult.disabled();
                }
            }
        } catch (Exception jwtErr) {
            Reporter.log("[LICENSE] Offline JWT validation failed: " + jwtErr.getMessage()
                    + " — Tier 3 disabled", LogLevel.WARN);
        }

        return ValidationResult.disabled();
    }

    // ──────────────────────── Online Validation ────────────────────────

    static ValidationResult validateOnline(String licenseKey) throws Exception {
        String machineId = getMachineId();
        String payload = "{\"key\":\"" + licenseKey + "\",\"machineId\":\"" + machineId
                + "\",\"version\":\"" + getFrameworkVersion() + "\"}";

        URL url = new java.net.URI(LICENSING_API_URL).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();

        if (responseCode == 408 || responseCode == 429) {
            throw new IOException("Licensing server transient HTTP " + responseCode
                    + " — treating as unreachable (offline grace fallback)");
        }

        if (responseCode >= 400 && responseCode < 500) {
            String body = readStream(conn.getErrorStream());
            throw new LicenseRejectedException("HTTP " + responseCode + ": " + body);
        }

        if (responseCode != 200) {
            throw new IOException("Licensing server returned HTTP " + responseCode);
        }

        String body = readStream(conn.getInputStream());
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        boolean valid  = json.has("valid")  && json.get("valid").getAsBoolean();
        boolean tier3  = json.has("tier3")  && json.get("tier3").getAsBoolean();
        String  org    = json.has("org")    ? json.get("org").getAsString() : "";
        Instant expiry = Instant.EPOCH;
        if (json.has("expiry")) {
            try { expiry = Instant.parse(json.get("expiry").getAsString()); }
            catch (Exception ignored) {}
        }

        if (!valid) return ValidationResult.disabled();
        return new ValidationResult(tier3, expiry, org);
    }

    // ──────────────────────── Offline JWT Validation ────────────────────────

    /**
     * Validates an RSA-SHA256 signed JWT without any external dependencies
     * (pure JDK — avoids JJWT classpath dependency at validation time).
     *
     * The JWT is expected to carry payload claims: {@code tier3}, {@code exp}, {@code org}.
     * Signature verification uses the embedded RSA public key.
     *
     * <p>If JJWT is on the classpath (optional dependency), delegates to it for robustness.
     * Falls back to a manual Base64+JDK RSA verify otherwise.</p>
     */
    static ValidationResult validateOfflineJwt(String licenseKey) throws Exception {
        if (licenseKey == null || !licenseKey.contains(".")) return null;

        // Try JJWT first (optional dependency — present when ONNX feature is enabled)
        try {
            return validateWithJjwt(licenseKey);
        } catch (ClassNotFoundException ignored) {
            // JJWT not on classpath — fall through to manual verification
        }

        // Manual JWT verification (3-part Base64url split)
        String[] parts = licenseKey.split("\\.");
        if (parts.length != 3) return null;

        String payloadJson = new String(
                Base64.getUrlDecoder().decode(addPadding(parts[1])),
                StandardCharsets.UTF_8);

        JsonObject payload = JsonParser.parseString(payloadJson).getAsJsonObject();

        // Check expiry
        if (payload.has("exp")) {
            long expEpoch = payload.get("exp").getAsLong();
            if (Instant.ofEpochSecond(expEpoch).isBefore(Instant.now())) {
                Reporter.log("[LICENSE] JWT expired at " + Instant.ofEpochSecond(expEpoch), LogLevel.DEBUG);
                return ValidationResult.disabled();
            }
        }

        // Verify RSA-SHA256 signature using embedded public key
        boolean sigValid = verifyRsaSignature(parts[0] + "." + parts[1], parts[2]);
        if (!sigValid) {
            Reporter.log("[LICENSE] JWT signature verification failed — tampered token", LogLevel.WARN);
            return null;
        }

        boolean tier3  = payload.has("tier3") && payload.get("tier3").getAsBoolean();
        String  org    = payload.has("org")   ? payload.get("org").getAsString()    : "";
        Instant expiry = Instant.EPOCH;
        if (payload.has("exp")) {
            expiry = Instant.ofEpochSecond(payload.get("exp").getAsLong());
        }

        return new ValidationResult(tier3, expiry, org);
    }

    private static ValidationResult validateWithJjwt(String jwt) throws Exception {
        // Reflective JJWT call — avoids compile-time dependency
        Class<?> parsersClass = Class.forName("io.jsonwebtoken.Jwts");

        java.security.PublicKey pubKey = getEmbeddedPublicKey();
        if (pubKey == null) throw new ClassNotFoundException("Public key not available");

        Object parser  = parsersClass.getMethod("parser").invoke(null);
        Object builder = parser.getClass().getMethod("verifyWith", java.security.PublicKey.class)
                .invoke(parser, pubKey);
        Object built   = builder.getClass().getMethod("build").invoke(builder);
        Object jws     = built.getClass().getMethod("parseSignedClaims", String.class)
                .invoke(built, jwt);
        Object claims  = jws.getClass().getMethod("getPayload").invoke(jws);

        boolean tier3  = (Boolean) claims.getClass().getMethod("get", String.class, Class.class)
                .invoke(claims, "tier3", Boolean.class);
        String  org    = (String) claims.getClass().getMethod("get", String.class, Class.class)
                .invoke(claims, "org", String.class);
        Date    exp    = (Date) claims.getClass().getMethod("getExpiration").invoke(claims);
        Instant expiry = exp != null ? exp.toInstant() : Instant.EPOCH;

        return new ValidationResult(tier3, expiry, org != null ? org : "");
    }

    // ──────────────────────── Grace Period Cache ────────────────────────

    static boolean isWithinGracePeriod(String licenseKey) {
        try {
            Path cachePath = Paths.get(CACHE_FILE_PATH);
            if (!Files.exists(cachePath)) return false;

            String json = Files.readString(cachePath);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

            if (!obj.has("lastOnlineValidation") || !obj.has("licenseKeyHash")) return false;

            // Compare hash of license key (avoid storing key in plain text)
            String storedHash = obj.get("licenseKeyHash").getAsString();
            if (!storedHash.equals(sha256Hex(licenseKey))) return false;

            String timestampStr = obj.get("lastOnlineValidation").getAsString();
            Instant lastValidated = Instant.parse(timestampStr);
            return ChronoUnit.DAYS.between(lastValidated, Instant.now()) <= GRACE_PERIOD_DAYS;

        } catch (Exception e) {
            Reporter.log("[LICENSE] Could not read grace period cache: " + e.getMessage(), LogLevel.DEBUG);
            return false;
        }
    }

    private static void persistGraceCache(String licenseKey) {
        try {
            Path cachePath = Paths.get(CACHE_FILE_PATH);
            Files.createDirectories(cachePath.getParent());

            JsonObject obj = new JsonObject();
            obj.addProperty("lastOnlineValidation", Instant.now().toString());
            obj.addProperty("licenseKeyHash", sha256Hex(licenseKey));

            Files.writeString(cachePath, GSON.toJson(obj), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            Reporter.log("[LICENSE] Could not persist grace cache (non-fatal): " + e.getMessage(), LogLevel.DEBUG);
        }
    }

    // ──────────────────────── RSA Signature Verification ────────────────────────

    private static boolean verifyRsaSignature(String signingInput, String signatureB64) {
        try {
            java.security.PublicKey pubKey = getEmbeddedPublicKey();
            if (pubKey == null) return false;

            byte[] signatureBytes = Base64.getUrlDecoder().decode(addPadding(signatureB64));
            byte[] inputBytes     = signingInput.getBytes(StandardCharsets.UTF_8);

            java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA");
            sig.initVerify(pubKey);
            sig.update(inputBytes);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            Reporter.log("[LICENSE] RSA signature error: " + e.getMessage(), LogLevel.DEBUG);
            return false;
        }
    }

    /**
     * Returns the embedded RSA-2048 public key used for offline JWT signature verification.
     *
     * <p>Placeholder: returns null until the real public key PEM is embedded here.
     * Replace the {@code EMBEDDED_PUBLIC_KEY_PEM} constant with the actual PEM before
     * shipping Tier 3. The matching private key is held only by the licensing server.</p>
     */
    private static java.security.PublicKey getEmbeddedPublicKey() {
        if (EMBEDDED_PUBLIC_KEY_PEM == null || EMBEDDED_PUBLIC_KEY_PEM.isBlank()) return null;

        try {
            String pem = EMBEDDED_PUBLIC_KEY_PEM
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(pem);
            java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(keyBytes);
            return java.security.KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            Reporter.log("[LICENSE] Could not load embedded public key: " + e.getMessage(), LogLevel.DEBUG);
            return null;
        }
    }

    // ──────────────────────── Utilities ────────────────────────

    private static String getMachineId() {
        // Use hostname + user.name as a stable-enough machine fingerprint
        try {
            return sha256Hex(java.net.InetAddress.getLocalHost().getHostName()
                    + "|" + System.getProperty("user.name", "unknown"));
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String getFrameworkVersion() {
        Package pkg = LicenseValidator.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }
        return "unknown";
    }

    private static String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static String addPadding(String base64url) {
        int pad = (4 - base64url.length() % 4) % 4;
        return base64url + "=".repeat(pad);
    }

    private static String sha256Hex(String input) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ──────────────────────── Result Model ────────────────────────

    static class ValidationResult {
        final boolean tier3Enabled;
        final Instant expiry;
        final String  org;

        ValidationResult(boolean tier3Enabled, Instant expiry, String org) {
            this.tier3Enabled = tier3Enabled;
            this.expiry = expiry;
            this.org    = org;
        }

        static ValidationResult disabled() {
            return new ValidationResult(false, Instant.EPOCH, "");
        }
    }

    private static class LicenseRejectedException extends RuntimeException {
        LicenseRejectedException(String message) { super(message); }
    }
}
