package Ellithium.core.ai;

import Ellithium.core.ai.sanitizers.DataScrubber;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DataScrubberTest {

    @Test
    public void testScrub_MasksEmails() {
        String input = "Contact us at user@example.com for help.";
        String scrubbed = DataScrubber.scrub(input);
        Assert.assertTrue(scrubbed.contains("***@***.***"));
        Assert.assertFalse(scrubbed.contains("user@example.com"));
    }

    @Test
    public void testScrub_MasksPasswordsAndTokens() {
        String input = "<input type=\"password\" value=\"superSecret123\"/>" +
                       "<div id=\"token\">Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIxMjM.SflKxwRJS</div>";
        String scrubbed = DataScrubber.scrub(input);
        
        Assert.assertFalse(scrubbed.contains("superSecret123"));
        Assert.assertTrue(scrubbed.contains("value=\"[MASKED]\""));
        Assert.assertTrue(scrubbed.contains("[MASKED_TOKEN]"));
    }

    @Test
    public void testScrub_MasksAPIKeys() {
        String input = "<div api-key=\"AIzaSyA_testKey12345\"></div>";
        String scrubbed = DataScrubber.scrub(input);
        Assert.assertTrue(scrubbed.contains("[MASKED_KEY]"));
        Assert.assertFalse(scrubbed.contains("AIzaSyA_testKey12345"));
    }

    @Test
    public void testScrub_HandlesNullAndEmpty() {
        Assert.assertEquals(DataScrubber.scrub(null), null);
        Assert.assertEquals(DataScrubber.scrub(""), "");
    }

    @Test
    public void testScrub_MasksMultiplePIIOnSameLine() {
        String input = "Users a@b.com, c@d.com and e@f.com joined.";
        String scrubbed = DataScrubber.scrub(input);
        Assert.assertEquals(scrubbed, "Users ***@***.***, ***@***.*** and ***@***.*** joined.");
    }

    @Test
    public void testScrub_MasksEdgeCaseAPIKeys() {
        String input1 = "authorization: \"Bearer custom_token_123\"";
        String input2 = "x-api-key=\"secret_4567890\"";
        
        Assert.assertTrue(DataScrubber.scrub(input1).contains("[MASKED_KEY]"));
        Assert.assertTrue(DataScrubber.scrub(input2).contains("[MASKED_KEY]"));
    }

    @Test
    public void testScrub_HandlesInvalidJWTGracefully() {
        // Just somewhat looking like a JWT but invalid
        String invalidJwt = "eyJhbG.invalid.format";
        String scrubbed = DataScrubber.scrub(invalidJwt);
        // It shouldn't crash and shouldn't mask because it doesn't match the strict regex
        Assert.assertEquals(scrubbed, invalidJwt);
    }
}
