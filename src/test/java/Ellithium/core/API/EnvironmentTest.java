package Ellithium.core.API;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

public class EnvironmentTest {

    private Environment env;
    private final String envName = "test_env_coverage";

    @BeforeClass
    public void setup() {
        env = new Environment(envName);
        env.clear();
    }

    @AfterClass
    public void tearDown() {
        if (env != null) {
            env.clear();
            File f = new File(env.path());
            if (f.exists()) f.delete();
        }
    }

    @Test
    public void testSetAndGetString() {
        env.set("appName", "EllithiumEngine");
        Assert.assertEquals(env.get("appName"), "EllithiumEngine");
        Assert.assertEquals(env.get("appName", String.class), "EllithiumEngine");
        Assert.assertTrue(env.has("appName"));
    }

    @Test
    public void testSetAndGetInteger() {
        env.set("port", 8080);
        Assert.assertEquals(env.getAsInteger("port"), 8080);
        Assert.assertEquals(env.get("port", Integer.class), Integer.valueOf(8080));
    }

    @Test
    public void testSetAndGetDouble() {
        env.set("threshold", 0.85);
        Assert.assertEquals(env.getAsDouble("threshold"), 0.85, 0.001);
        Assert.assertEquals(env.get("threshold", Double.class), Double.valueOf(0.85));
    }

    @Test
    public void testSetAndGetBoolean() {
        env.set("featureEnabled", true);
        Assert.assertTrue(env.getAsBoolean("featureEnabled"));
        Assert.assertEquals(env.get("featureEnabled", Boolean.class), Boolean.TRUE);

        env.set("featureDisabled", false);
        Assert.assertFalse(env.getAsBoolean("featureDisabled"));

        // Test alternate string representations for booleans
        env.set("flagYes", "yes");
        Assert.assertTrue(env.getAsBoolean("flagYes"));

        env.set("flagOne", "1");
        Assert.assertTrue(env.getAsBoolean("flagOne"));

        env.set("flagOn", "on");
        Assert.assertTrue(env.getAsBoolean("flagOn"));

        env.set("flagEnabled", "enabled");
        Assert.assertTrue(env.getAsBoolean("flagEnabled"));

        env.set("flagNo", "no");
        Assert.assertFalse(env.getAsBoolean("flagNo"));

        env.set("flagZero", "0");
        Assert.assertFalse(env.getAsBoolean("flagZero"));

        env.set("flagOff", "off");
        Assert.assertFalse(env.getAsBoolean("flagOff"));

        env.set("flagDisabled", "disabled");
        Assert.assertFalse(env.getAsBoolean("flagDisabled"));
    }

    @Test
    public void testInvalidKeyHandling() {
        env.set(null, "val");
        env.set("   ", "val");
        Assert.assertNull(env.get(null));
        Assert.assertNull(env.get("   "));
        Assert.assertNull(env.get("nonExistentKey"));
        Assert.assertEquals(env.getAsInteger("nonExistentKey"), 0);
        Assert.assertEquals(env.getAsDouble("nonExistentKey"), 0.0, 0.001);
        Assert.assertFalse(env.getAsBoolean("nonExistentKey"));
    }

    @Test
    public void testConversionFailureHandledGracefully() {
        env.set("notAnInt", "abc");
        Assert.assertNull(env.get("notAnInt", Integer.class));
        Assert.assertEquals(env.getAsInteger("notAnInt"), 0);

        env.set("notADouble", "invalid_double");
        Assert.assertNull(env.get("notADouble", Double.class));
        Assert.assertEquals(env.getAsDouble("notADouble"), 0.0, 0.001);
    }

    @Test
    public void testRemoveAndClear() {
        env.set("keyToRemove", "value");
        Assert.assertTrue(env.has("keyToRemove"));
        env.remove("keyToRemove");
        Assert.assertFalse(env.has("keyToRemove"));

        env.remove(null); // should not throw
        env.remove("  "); // should not throw

        env.set("k1", "v1");
        env.set("k2", "v2");
        List<String> keys = env.keys();
        Assert.assertTrue(keys.contains("k1"));
        Assert.assertTrue(keys.contains("k2"));

        env.clear();
        Assert.assertFalse(env.has("k1"));
        Assert.assertFalse(env.has("k2"));
    }

    @Test
    public void testMetadata() {
        Assert.assertEquals(env.name(), envName);
        Assert.assertTrue(env.path().contains(envName + ".json"));
    }
}
