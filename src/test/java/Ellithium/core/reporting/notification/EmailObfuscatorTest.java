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
}
