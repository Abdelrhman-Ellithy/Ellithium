package AI;

import Ellithium.Utilities.ai.sanitizers.DOMMinimizer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DOMMinimizerTest {

    @Test
    public void testMinimize_RemovesScriptsAndStyles() {
        String rawDom = "<html><head><script>alert('test');</script><style>body { color: red; }</style></head><body><div id='content'>Hello</div></body></html>";
        String minimized = DOMMinimizer.minimize(rawDom);
        
        Assert.assertFalse(minimized.contains("<script>"));
        Assert.assertFalse(minimized.contains("<style>"));
        Assert.assertTrue(minimized.contains("<div id='content'>Hello</div>"));
    }

    @Test
    public void testMinimize_RemovesHiddenElements() {
        String rawDom = "<body><div style='display: none;'>Hidden</div><div style='visibility: hidden;'>Also Hidden</div><div hidden>Hidden attr</div><div>Visible</div></body>";
        String minimized = DOMMinimizer.minimize(rawDom);
        
        Assert.assertFalse(minimized.contains("Hidden"));
        Assert.assertFalse(minimized.contains("Also Hidden"));
        Assert.assertTrue(minimized.contains("Visible"));
    }

    @Test
    public void testMinimize_RemovesLongDataAttributes() {
        String longValue = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        String rawDom = "<div data-reactid=\"" + longValue + "\" id=\"main\">Content</div>";
        String minimized = DOMMinimizer.minimize(rawDom);
        
        Assert.assertFalse(minimized.contains("data-reactid="));
        Assert.assertTrue(minimized.contains("id=\"main\""));
        Assert.assertTrue(minimized.contains("Content"));
    }
}
