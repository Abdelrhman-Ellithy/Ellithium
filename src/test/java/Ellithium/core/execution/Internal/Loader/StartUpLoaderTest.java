package Ellithium.core.execution.Internal.Loader;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

public class StartUpLoaderTest {

    @Test
    public void testCompareVersions_equalVersions() {
        Assert.assertEquals(StartUpLoader.compareVersions("3.3.0", "3.3.0"), 0);
        Assert.assertEquals(StartUpLoader.compareVersions("1.0", "1.0.0"), 0);
    }

    @Test
    public void testCompareVersions_majorMinorPatch() {
        Assert.assertTrue(StartUpLoader.compareVersions("3.3.1", "3.3.0") > 0);
        Assert.assertTrue(StartUpLoader.compareVersions("3.2.9", "3.3.0") < 0);
        Assert.assertTrue(StartUpLoader.compareVersions("4.0.0", "3.99.99") > 0);
        Assert.assertTrue(StartUpLoader.compareVersions("3.3.0.1", "3.3.0") > 0);
    }

    @Test
    public void testCompareVersions_qualifiers() {
        Assert.assertTrue(StartUpLoader.compareVersions("3.3.0-SNAPSHOT", "3.3.0") < 0);
        Assert.assertTrue(StartUpLoader.compareVersions("3.3.0", "3.3.0-SNAPSHOT") > 0);
        Assert.assertTrue(StartUpLoader.compareVersions("3.3.0-beta", "3.3.0-alpha") > 0);
    }

    @Test
    public void testCompareVersions_malformedAndNonNumericTokens() {
        // Must not throw NumberFormatException
        int cmp = StartUpLoader.compareVersions("3.x.0", "3.0.0");
        Assert.assertEquals(cmp, 0);

        int cmp2 = StartUpLoader.compareVersions("invalid", "1.0.0");
        Assert.assertTrue(cmp2 < 0);
    }

    @Test
    public void testFindJarFile_doesNotThrow() {
        File jar = StartUpLoader.findJarFile();
        // May be null when running in exploded IDE/target test environment
        if (jar != null) {
            Assert.assertTrue(jar.getName().endsWith(".jar"));
        }
    }
}
