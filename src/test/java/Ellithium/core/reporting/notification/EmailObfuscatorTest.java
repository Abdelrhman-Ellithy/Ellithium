package Ellithium.core.reporting.notification;

import org.testng.Assert;
import org.testng.annotations.Test;

public class EmailObfuscatorTest {

    @Test
    public void obfuscate_null_returnsNullEmpty() {
        Assert.assertEquals(EmailObfuscator.obfuscate(null), "NULL/EMPTY");
    }

    @Test
    public void obfuscate_empty_returnsNullEmpty() {
        Assert.assertEquals(EmailObfuscator.obfuscate(""), "NULL/EMPTY");
    }

    @Test
    public void obfuscate_noAtSign_returnsUnchanged() {
        Assert.assertEquals(EmailObfuscator.obfuscate("notanemail"), "notanemail");
    }

    @Test
    public void obfuscate_shortLocalPart_twoChars_returnsUnchanged() {
        Assert.assertEquals(EmailObfuscator.obfuscate("ab@example.com"), "ab@example.com");
    }

    @Test
    public void obfuscate_shortLocalPart_oneChar_returnsUnchanged() {
        Assert.assertEquals(EmailObfuscator.obfuscate("a@example.com"), "a@example.com");
    }

    @Test
    public void obfuscate_normalEmail_masksMiddle() {
        String result = EmailObfuscator.obfuscate("alice@example.com");
        Assert.assertTrue(result.startsWith("a"));
        Assert.assertTrue(result.contains("***"));
        Assert.assertTrue(result.endsWith("@example.com"));
    }

    @Test
    public void obfuscate_normalEmail_preservesFirstAndLastChar() {
        String result = EmailObfuscator.obfuscate("alice@example.com");
        Assert.assertTrue(result.startsWith("a"));
        Assert.assertTrue(result.contains("e@"));
    }

    @Test
    public void obfuscate_normalEmail_formatCheck() {
        Assert.assertEquals(EmailObfuscator.obfuscate("alice@gmail.com"), "a***e@gmail.com");
    }

    @Test
    public void obfuscate_longEmail_stillMasked() {
        String result = EmailObfuscator.obfuscate("abdelrahman@company.org");
        Assert.assertTrue(result.startsWith("a"));
        Assert.assertTrue(result.contains("***"));
        Assert.assertTrue(result.endsWith("@company.org"));
    }

    @Test
    public void obfuscate_threeCharLocal_masksMiddle() {
        String result = EmailObfuscator.obfuscate("abc@x.com");
        Assert.assertEquals(result, "a***c@x.com");
    }

    // ── Multi-address (comma-separated TO/CC lists) ─────────────────────────
    // Regression coverage: obfuscate() used to find only the first "@" in the whole string, so
    // with more than one address everything after the first email leaked into the "domain" part
    // unobfuscated (e.g. "alice@example.com,bob@example.com" -> "a***e@example.com,bob@example.com",
    // exposing bob's address in full).

    @Test
    public void obfuscate_twoEmails_bothMasked() {
        String result = EmailObfuscator.obfuscate("alice@example.com,bob@example.com");
        Assert.assertEquals(result, "a***e@example.com, b***b@example.com");
    }

    @Test
    public void obfuscate_threeEmails_allMasked() {
        String result = EmailObfuscator.obfuscate("alice@example.com,bob@example.com,carol@example.com");
        Assert.assertEquals(result, "a***e@example.com, b***b@example.com, c***l@example.com");
    }

    @Test
    public void obfuscate_multipleEmails_withSpacesAroundCommas_stillMasked() {
        String result = EmailObfuscator.obfuscate(" alice@example.com , bob@example.com ");
        Assert.assertEquals(result, "a***e@example.com, b***b@example.com");
    }
}
