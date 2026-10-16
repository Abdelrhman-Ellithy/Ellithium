package Ellithium.config.Internal;

import org.testng.Assert;
import org.testng.annotations.Test;

public class VersionCheckerTest {

    @Test
    public void testSolveVersion_doesNotThrowException() {
        // Must handle both cached date and offline/connection issues gracefully
        VersionChecker.solveVersion();
    }

    @Test
    public void testIsOlderVersion_semanticComparisons() {
        // Obvious older versions
        Assert.assertTrue(VersionChecker.isOlderVersion("3.2.0", "3.3.0"));
        Assert.assertTrue(VersionChecker.isOlderVersion("3.3.0", "3.3.1"));
        Assert.assertTrue(VersionChecker.isOlderVersion("2.9.9", "3.0.0"));

        // Critical bug fix: substring overlap where "3.3.10" contains "3.3.1"
        Assert.assertTrue(VersionChecker.isOlderVersion("3.3.1", "3.3.10"));
        Assert.assertFalse(VersionChecker.isOlderVersion("3.3.10", "3.3.1"));

        // Identical versions
        Assert.assertFalse(VersionChecker.isOlderVersion("3.3.0", "3.3.0"));
        Assert.assertFalse(VersionChecker.isOlderVersion("v3.3.0", "3.3.0"));

        // Newer version in use (e.g. dev snapshot or next major release)
        Assert.assertFalse(VersionChecker.isOlderVersion("4.0.0", "3.3.0"));
        Assert.assertFalse(VersionChecker.isOlderVersion("3.3.5", "3.3.4"));

        // Prefix handling
        Assert.assertTrue(VersionChecker.isOlderVersion("v3.2.0", "v3.3.0"));
        Assert.assertTrue(VersionChecker.isOlderVersion("3.2.0", "v3.3.0"));

        // Null and non-numeric safety
        Assert.assertFalse(VersionChecker.isOlderVersion(null, "3.3.0"));
        Assert.assertFalse(VersionChecker.isOlderVersion("3.3.0", null));
        Assert.assertFalse(VersionChecker.isOlderVersion(null, null));
    }
}
