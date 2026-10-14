package Ellithium.core.reporting.internal;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class AllureEnvironmentManagerTest {

    private File tempResultsDir;

    @BeforeClass
    public void setup() throws IOException {
        tempResultsDir = Files.createTempDirectory("allure-env-test").toFile();
    }

    @AfterClass
    public void tearDown() {
        if (tempResultsDir != null && tempResultsDir.exists()) {
            File[] files = tempResultsDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            tempResultsDir.delete();
        }
    }

    @Test
    public void writeEnvironmentProperties_writesCoreSystemAndFrameworkProperties() throws IOException {
        AllureEnvironmentManager.writeEnvironmentProperties(tempResultsDir);

        File envFile = new File(tempResultsDir, "environment.properties");
        assertTrue(envFile.exists(), "environment.properties should be created");

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(envFile)) {
            props.load(fis);
        }

        assertEqualsValue(props.getProperty("Framework"), "Ellithium 3.2.0");
        assertNotNull(props.getProperty("Operating.System"), "Operating.System should be recorded");
        assertNotNull(props.getProperty("Java.Version"), "Java.Version should be recorded");
        assertNotNull(props.getProperty("Host.Name"), "Host.Name should be recorded");
        assertNotNull(props.getProperty("AI.Healing.Strategy"), "AI.Healing.Strategy should be recorded");
        assertNotNull(props.getProperty("AI.Execution.Mode"), "AI.Execution.Mode should be recorded");
    }

    @Test
    public void writeExecutorJson_writesValidJsonStructure() throws IOException {
        AllureEnvironmentManager.writeExecutorJson(tempResultsDir);

        File execFile = new File(tempResultsDir, "executor.json");
        assertTrue(execFile.exists(), "executor.json should be created");

        String content = Files.readString(execFile.toPath());
        assertFalse(content.isBlank());
        assertTrue(content.contains("\"name\":"), "Should contain 'name' field");
        assertTrue(content.contains("\"type\":"), "Should contain 'type' field");
        assertTrue(content.contains("\"reportName\": \"Ellithium Allure Report\""), "Should contain Ellithium report name");
    }

    private void assertEqualsValue(String actual, String expected) {
        assertTrue(expected.equals(actual), "Expected [" + expected + "] but was [" + actual + "]");
    }
}
