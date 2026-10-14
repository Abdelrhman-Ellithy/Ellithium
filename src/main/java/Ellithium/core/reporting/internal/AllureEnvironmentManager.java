package Ellithium.core.reporting.internal;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.management.ConfigContext;
import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.driver.DriverConfiguration;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.logging.Logger;
import org.apache.commons.lang3.SystemUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Manages Allure 3 dynamic environment and infrastructure metadata.
 * Automatically generates {@code environment.properties} and {@code executor.json}
 * in the Allure results directory so reports feature rich execution badges,
 * runtime configurations, CI build links, and infrastructure specifications.
 */
public final class AllureEnvironmentManager {

    private AllureEnvironmentManager() {
    }

    /**
     * Resolves the target Allure results directory.
     *
     * @return File object pointing to the results directory
     */
    public static File resolveAllureResultsDirectory() {
        String allurePropertiesFilePath = ConfigContext.getAllureFilePath();
        String resultsPath = System.getProperty("allure.results.directory");
        if (resultsPath == null || resultsPath.isBlank()) {
            resultsPath = PropertyHelper.getDataFromProperties(allurePropertiesFilePath, "allure.results.directory");
        }
        if (resultsPath == null || resultsPath.isBlank()) {
            resultsPath = "Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-results";
        }
        File dir = new File(resultsPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Generates both {@code environment.properties} and {@code executor.json}
     * in the active Allure results directory.
     */
    public static void writeEnvironmentMetadata() {
        try {
            File resultsDir = resolveAllureResultsDirectory();
            writeEnvironmentProperties(resultsDir);
            writeExecutorJson(resultsDir);
        } catch (Exception e) {
            Logger.warn("Failed to generate Allure environment metadata: " + e.getMessage());
        }
    }

    /**
     * Writes dynamic runtime environment properties to {@code environment.properties}.
     */
    public static void writeEnvironmentProperties(File resultsDir) {
        if (resultsDir == null || !resultsDir.exists()) return;
        File envFile = new File(resultsDir, "environment.properties");

        Properties props = new Properties();
        props.setProperty("Framework", "Ellithium 3.2.0");
        props.setProperty("Java.Version", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        props.setProperty("Operating.System", SystemUtils.OS_NAME + " " + SystemUtils.OS_VERSION + " (" + System.getProperty("os.arch") + ")");
        
        try {
            props.setProperty("Host.Name", InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            props.setProperty("Host.Name", "Unknown");
        }

        // Driver and Browser Configuration
        DriverConfiguration config = DriverFactory.getCurrentDriverConfiguration();
        if (config != null) {
            if (config.getDriverType() != null) {
                props.setProperty("Driver.Type", config.getDriverType().getName());
            }
            if (config.getHeadlessMode() != null) {
                props.setProperty("Headless.Mode", config.getHeadlessMode().getName());
            }
        }

        WebDriver driver = DriverFactory.getCurrentDriver();
        if (driver instanceof HasCapabilities hc) {
            Capabilities caps = hc.getCapabilities();
            if (caps != null) {
                if (caps.getBrowserName() != null && !caps.getBrowserName().isEmpty()) {
                    props.setProperty("Browser.Name", caps.getBrowserName());
                }
                if (caps.getBrowserVersion() != null && !caps.getBrowserVersion().isEmpty()) {
                    props.setProperty("Browser.Version", caps.getBrowserVersion());
                }
                if (caps.getPlatformName() != null) {
                    props.setProperty("Platform", caps.getPlatformName().toString());
                }
            }
        }

        // AI Healing Engine Configuration
        props.setProperty("AI.Healing.Strategy", AIConfigLoader.getHealingStrategy().name());
        props.setProperty("AI.Execution.Mode", AIConfigLoader.getExecutionMode().name());
        props.setProperty("AI.Tier1.Algorithmic", AIConfigLoader.isTier1Enabled() ? "Enabled" : "Disabled");
        props.setProperty("AI.Tier2.Vector", AIConfigLoader.isTier2Enabled() ? "Enabled" : "Disabled");
        props.setProperty("AI.Tier3.LLM", AIConfigLoader.isTier3Enabled() ? ("Enabled (" + AIConfigLoader.getLlmProviderName() + ")") : "Disabled");

        try (FileOutputStream fos = new FileOutputStream(envFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            props.store(writer, "Allure Execution Environment Metadata Generated by Ellithium");
        } catch (IOException e) {
            Logger.warn("Failed to write Allure environment.properties: " + e.getMessage());
        }
    }

    /**
     * Writes CI/Local execution metadata to {@code executor.json}.
     */
    public static void writeExecutorJson(File resultsDir) {
        if (resultsDir == null || !resultsDir.exists()) return;
        File execFile = new File(resultsDir, "executor.json");

        Map<String, String> execMap = new LinkedHashMap<>();
        String githubActions = System.getenv("GITHUB_ACTIONS");
        String gitlabCi = System.getenv("GITLAB_CI");
        String jenkinsUrl = System.getenv("JENKINS_URL");
        String azurePipelines = System.getenv("TF_BUILD");

        if (githubActions != null && githubActions.equalsIgnoreCase("true")) {
            execMap.put("name", "GitHub Actions");
            execMap.put("type", "github");
            execMap.put("buildName", System.getenv("GITHUB_WORKFLOW") + " #" + System.getenv("GITHUB_RUN_NUMBER"));
            String serverUrl = System.getenv("GITHUB_SERVER_URL") != null ? System.getenv("GITHUB_SERVER_URL") : "https://github.com";
            String repo = System.getenv("GITHUB_REPOSITORY");
            String runId = System.getenv("GITHUB_RUN_ID");
            if (repo != null && runId != null) {
                execMap.put("buildUrl", serverUrl + "/" + repo + "/actions/runs/" + runId);
            }
            execMap.put("buildOrder", System.getenv("GITHUB_RUN_NUMBER"));
            execMap.put("reportName", "Ellithium Allure Report");
        } else if (gitlabCi != null) {
            execMap.put("name", "GitLab CI");
            execMap.put("type", "gitlab");
            execMap.put("buildName", System.getenv("CI_JOB_NAME") + " #" + System.getenv("CI_JOB_ID"));
            execMap.put("buildUrl", System.getenv("CI_JOB_URL"));
            execMap.put("buildOrder", System.getenv("CI_JOB_ID"));
            execMap.put("reportName", "Ellithium Allure Report");
        } else if (jenkinsUrl != null) {
            execMap.put("name", "Jenkins");
            execMap.put("type", "jenkins");
            execMap.put("buildName", System.getenv("JOB_NAME") + " #" + System.getenv("BUILD_NUMBER"));
            execMap.put("buildUrl", System.getenv("BUILD_URL"));
            execMap.put("buildOrder", System.getenv("BUILD_NUMBER"));
            execMap.put("reportName", "Ellithium Allure Report");
        } else if (azurePipelines != null) {
            execMap.put("name", "Azure DevOps Pipelines");
            execMap.put("type", "azure");
            execMap.put("buildName", System.getenv("BUILD_DEFINITIONNAME") + " #" + System.getenv("BUILD_BUILDNUMBER"));
            execMap.put("buildUrl", System.getenv("SYSTEM_TEAMFOUNDATIONCOLLECTIONURI") + System.getenv("SYSTEM_TEAMPROJECT") + "/_build/results?buildId=" + System.getenv("BUILD_BUILDID"));
            execMap.put("buildOrder", System.getenv("BUILD_BUILDID"));
            execMap.put("reportName", "Ellithium Allure Report");
        } else {
            execMap.put("name", System.getProperty("user.name") + " (Local Workstation)");
            execMap.put("type", "local");
            execMap.put("buildName", "Local Run - " + TestDataGenerator.getTimeStamp());
            execMap.put("reportName", "Ellithium Allure Report");
        }

        StringBuilder json = new StringBuilder("{\n");
        int count = 0;
        for (Map.Entry<String, String> entry : execMap.entrySet()) {
            if (entry.getValue() == null) continue;
            if (count++ > 0) json.append(",\n");
            json.append("  \"").append(entry.getKey()).append("\": \"")
                .append(escapeJson(entry.getValue())).append("\"");
        }
        json.append("\n}\n");

        try (FileOutputStream fos = new FileOutputStream(execFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(json.toString());
        } catch (IOException e) {
            Logger.warn("Failed to write Allure executor.json: " + e.getMessage());
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
