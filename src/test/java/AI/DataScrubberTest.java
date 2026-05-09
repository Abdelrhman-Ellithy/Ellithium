package AI;

import Ellithium.Utilities.ai.sanitizers.DataScrubber;
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
}
