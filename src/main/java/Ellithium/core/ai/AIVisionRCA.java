package Ellithium.core.ai;

import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.ai.provider.LLMProvider;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.qameta.allure.Allure;

import java.io.File;
import java.nio.file.Files;

/**
 * Handles Visual Root Cause Analysis (RCA) for test failures.
 * Takes the failure screenshot, sends it to a vision-capable LLM,
 * and attaches the AI's explanation to the Allure report.
 */
public class AIVisionRCA {

    private static final String RCA_PROMPT = 
            "You are an expert test automation engineer. " +
            "Analyze this screenshot of a failed automated test. " +
            "The test has just failed at this exact screen. " +
            "Identify any obvious visual reasons why an interaction might have failed " +
            "(e.g., an error popup, a loading spinner blocking the screen, " +
            "the desired element is not visible, a cookie banner is in the way). " +
            "Keep your explanation concise and focused on the root cause.";

    /**
     * Performs Vision RCA on a failed test screenshot.
     * 
     * @param screenshot   The saved screenshot file
     * @param errorMessage The exception message or error details
     * @param provider     The configured LLM Provider
     */
    public static void analyze(File screenshot, String errorMessage, LLMProvider provider) {
        if (!AIConfigLoader.isVisionRcaEnabled()) {
            return;
        }

        if (provider == null) {
            Reporter.log("AIVisionRCA skipped: No LLM Provider configured.", LogLevel.INFO_YELLOW);
            return;
        }

        if (!provider.supportsVision()) {
            Reporter.log("AIVisionRCA skipped: Provider '" + provider.getModelName() + "' does not support vision.", LogLevel.INFO_YELLOW);
            return;
        }

        if (screenshot == null || !screenshot.exists()) {
            Reporter.log("AIVisionRCA skipped: Screenshot file not found.", LogLevel.INFO_YELLOW);
            return;
        }

        try {
            Reporter.log("Starting AI Vision RCA for failed test...", LogLevel.INFO_BLUE);
            byte[] imageBytes = Files.readAllBytes(screenshot.toPath());
            
            String promptWithContext = RCA_PROMPT + "\n\nError Context: " + errorMessage;
            String rcaResult = provider.askWithVision(promptWithContext, imageBytes);
            
            Reporter.log("AI Vision RCA Result: " + rcaResult, LogLevel.INFO_YELLOW);
            
            // Attach to Allure Report
            Allure.addAttachment("AI Vision RCA", "text/plain", rcaResult);
            
        } catch (Exception e) {
            Reporter.log("AIVisionRCA failed: " + e.getMessage(), LogLevel.ERROR);
        }
    }
}
