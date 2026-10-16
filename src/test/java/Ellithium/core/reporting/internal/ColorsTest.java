package Ellithium.core.reporting.internal;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ColorsTest {

    @Test
    public void testAnsiColorCodes() {
        Assert.assertNotNull(Colors.RESET);
        Assert.assertNotNull(Colors.RED);
        Assert.assertNotNull(Colors.GREEN);
        Assert.assertNotNull(Colors.YELLOW);
        Assert.assertNotNull(Colors.BLUE);
        Assert.assertNotNull(Colors.PURPLE);
        Assert.assertNotNull(Colors.CYAN);
        Assert.assertNotNull(Colors.PINK);
        Assert.assertNotNull(Colors.BROWN);

        Assert.assertTrue(Colors.RESET.startsWith("\u001B["));
        Assert.assertTrue(Colors.RED.startsWith("\u001B["));
        Assert.assertTrue(Colors.GREEN.startsWith("\u001B["));
    }
}
