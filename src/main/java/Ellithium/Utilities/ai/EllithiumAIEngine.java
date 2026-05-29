package Ellithium.Utilities.ai;

import Ellithium.core.ai.TraceabilityManager;
import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.ai.generators.FeatureFileModifier;
import Ellithium.core.ai.generators.PomClassGenerator;
import Ellithium.core.ai.models.GeneratedAssets;
import Ellithium.core.ai.models.TestCaseSource;
import Ellithium.core.ai.models.TraceabilityRecord;
import Ellithium.core.ai.provider.LLMProvider;
import Ellithium.core.ai.readers.JsonTestCaseReader;
import Ellithium.core.ai.readers.TestCaseReader;
import Ellithium.core.ai.readers.TextTestCaseReader;
import Ellithium.core.ai.sanitizers.DataScrubber;
import Ellithium.core.ai.sanitizers.DOMMinimizer;
import Ellithium.core.execution.listener.seleniumListener;
import Ellithium.core.ai.generators.LiveContextGenerator;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.HeadlessMode;
import Ellithium.core.driver.LocalDriverType;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The central facade and entry point for all Ellithium AI capabilities.
 *
 * <p>Usage:</p>
 * <pre>
 * EllithiumAIEngine engine = new EllithiumAIEngine(myLlmProvider);
 * engine.generateFrom("src/test/resources/test-cases.json");
 * engine.generateFrom("src/test/resources/test-cases.txt");
 * </pre>
 *
 * <p>On each run, the engine:</p>
 * <ol>
 *   <li>Reads natural-language test cases from a JSON or text file</li>
 *   <li>Checks {@link TraceabilityManager} — skips already-generated tests</li>
 *   <li><b>Live DOM grounding (if URL provided):</b> opens a headless browser,
 *       navigates to the target URL, captures and minimizes the live DOM</li>
 *   <li>Sends each new test case (+ live DOM if available) to the configured LLM</li>
 *   <li>Parses the LLM's structured JSON response</li>
 *   <li>Generates the POM class (or injects into existing) and the test class</li>
 *   <li>Generated test classes include proper TestNG scaffolding:
 *       {@code @BeforeMethod}/{@code @AfterMethod} with DriverFactory lifecycle</li>
 *   <li>Optionally generates or updates the BDD .feature file</li>
 *   <li>Saves the mapping to {@code ellithium-ai-mappings.json}</li>
 * </ol>
 */
public class EllithiumAIEngine {

    // ──────────────────────── Static In-Context Generation API ────────────────────────

    /**
     * Generates and <b>immediately executes</b> code from natural-language steps
     * using the tester's <b>currently running, authenticated browser</b>.
     *
     * <p>No new browser is opened. No credentials are shared. The AI sees only
     * the current page's Accessibility Tree and generates locators from real elements.</p>
     *
     * <h3>Example</h3>
     * <pre>
     * // In a running test — driver is already logged in, on the checkout page:
     * EllithiumAIEngine.continueFrom(driver, llmProvider,
     *     "Fill shipping name 'John', city 'Cairo', then click 'Place Order'");
     * </pre>
     *
     * @param driver             The tester's live, authenticated WebDriver
     * @param llmProvider        The LLM provider (Gemini, OpenAI, etc.)
     * @param naturalLanguageSteps The steps to generate, in plain English
     */
    public static void continueFrom(WebDriver driver, LLMProvider llmProvider, String naturalLanguageSteps) {
        LiveContextGenerator.continueFrom(driver, llmProvider, naturalLanguageSteps);
    }

    /**
     * Reads natural-language steps from a file and generates/executes code using
     * the tester's current browser state.
     *
     * <h3>Example file (checkout-steps.txt)</h3>
     * <pre>
     * Fill in shipping name with "John Doe"
     * Select country "Egypt" from dropdown
     * Click "Place Order" button
     * Verify order confirmation message appears
     * </pre>
     *
     * @param driver      The tester's live, authenticated WebDriver
     * @param llmProvider The LLM provider
     * @param filePath    Path to the steps file
     */
    public static void continueFromFile(WebDriver driver, LLMProvider llmProvider, String filePath) {
        LiveContextGenerator.continueFromFile(driver, llmProvider, filePath);
    }

    /**
     * Starts a Playwright codegen-style interaction recorder.
     *
     * <p>Records every click, type, and navigation performed through the returned
     * decorated driver. Injects a floating toolbar into the page showing a
     * recording indicator and interaction count.</p>
     *
     * <p><b>Important:</b> Set {@code rootLogger.level=DEBUG} in {@code log4j2.properties}
     * before starting the recorder for full interaction capture.</p>
     *
     * <h3>Example</h3>
     * <pre>
     * var recorder = EllithiumAIEngine.startRecording(driver);
     * WebDriver recDriver = recorder.getDriver();
     * // ... perform manual actions via recDriver ...
     * recorder.stop();
     * recorder.generateCode(llmProvider);
     * </pre>
     *
     * @param driver The WebDriver to record interactions on
     * @return The recorder instance
     */
    public static void startRecording(WebDriver driver) {
        seleniumListener.startRecording(driver);
    }

    public static void stopRecording() {
        seleniumListener.stopRecording();
    }

    public static void generateCodeFromRecording(Object llmProvider) {
        seleniumListener.generateCode((Ellithium.core.ai.provider.LLMProvider) llmProvider);
    }

    // ──────────────────────── System Prompt ────────────────────────

    /**
     * The system prompt uses ACTUAL Ellithium API patterns from the real codebase:
     * - Page Objects hold a {@code DriverActions<?>} field
     * - Actions accessed via: driverActions.elements().sendData(), .clickOnElement(), .getText()
     * - Select via: driverActions.select().selectDropdownByText()
     * - Mouse via: driverActions.mouse().hoverOverElement(), .doubleClick()
     * - Navigation via: driverActions.navigation()
     * - Waits via: driverActions.waits()
     */
    private static final String SYSTEM_PROMPT =
            "You are an expert Java test automation engineer who uses the Ellithium framework.\n"
            + "You write clean, readable, business-level Page Object Model (POM) code.\n\n"
            + "## Reference Documentation:\n"
            + "- GitHub: https://github.com/Abdelrhman-Ellithy/Ellithium\n"
            + "- Website: https://abdelrhman-ellithy.github.io/ellithium.github.io/\n\n"
            + "## Ellithium POM Rules:\n"
            + "1. Page Objects extend nothing — they hold these fields:\n"
            + "   WebDriver driver;\n"
            + "   DriverActions driverActions;\n"
            + "2. Constructor takes WebDriver and initializes both:\n"
            + "   public LoginPage(WebDriver driver) {\n"
            + "       this.driver = driver;\n"
            + "       driverActions = new DriverActions<>(driver);\n"
            + "   }\n"
            + "3. Locators are: private final By locatorName = By.cssSelector(\"...\");\n"
            + "   OR AppiumBy.accessibilityId(\"...\") for mobile.\n"
            + "4. Business methods use REAL Ellithium API Structure (accessed via driverActions):\n"
            + "   - .elements() -> ElementActions (sendData, clickOnElement, getText, clearElement, isElementDisplayed)\n"
            + "   - .waits() -> WaitActions (waitForElementToBeVisible, waitForElementToBeClickable)\n"
            + "   - .select() -> SelectActions (selectDropdownByText, selectDropdownByIndex)\n"
            + "   - .mouse() -> MouseActions (hoverOverElement, doubleClick, rightClick)\n"
            + "   - .mobileActions() -> MobileActions (swipe, longPress, pinch, tap)\n"
            + "   - .windows() -> WindowActions (switchToWindow, closeWindow)\n"
            + "   - .frames() -> FrameActions (switchToFrame, switchToDefaultContent)\n"
            + "   - .alerts() -> AlertActions (acceptAlert, dismissAlert, getAlertText)\n"
            + "   - driverActions.elements().sendData(locator, data)\n"
            + "   - driverActions.elements().clickOnElement(locator)\n"
            + "   - driverActions.elements().getText(locator)\n"
            + "   - driverActions.elements().getAttributeValue(locator, attr)\n"
            + "   - driverActions.elements().clearElement(locator)\n"
            + "   - driverActions.elements().isElementDisplayed(locator)\n"
            + "   - driverActions.select().selectDropdownByText(locator, option)\n"
            + "   - driverActions.select().selectDropdownByIndex(locator, index)\n"
            + "   - driverActions.mouse().hoverOverElement(locator)\n"
            + "   - driverActions.mouse().doubleClick(locator)\n"
            + "   - driverActions.mouse().rightClick(locator)\n"
            + "   - driverActions.waits().waitForElementToBeVisible(locator, timeout, polling)\n"
            + "   - driverActions.waits().waitForElementToBeClickable(locator, timeout, polling)\n"
            + "5. Business methods return 'this' for fluent chaining, or a new Page Object for navigation.\n"
            + "6. Method names are business-level: login(String user, String pass), searchForProduct(String name).\n"
            + "7. Never use Thread.sleep(). Never use raw driver.findElement().\n"
            + "8. Required imports:\n"
            + "   import Ellithium.Utilities.interactions.DriverActions;\n"
            + "   import org.openqa.selenium.By;\n"
            + "   import org.openqa.selenium.WebDriver;\n\n"
            + "## Response Format (strict JSON, no markdown):\n"
            + "{\n"
            + "  \"pomClass\": \"LoginPage\",\n"
            + "  \"pomPackage\": \"pages\",\n"
            + "  \"pomMethods\": [\"login\", \"enterEmail\", \"enterPassword\"],\n"
            + "  \"locatorFields\": [\"private final By emailField = By.id(\\\"email\\\");\"],\n"
            + "  \"methodBodies\": [\"public LoginPage login(String email, String pass) { ... return this; }\"],\n"
            + "  \"testClass\": \"LoginTest\",\n"
            + "  \"testPackage\": \"tests\",\n"
            + "  \"testMethod\": \"testLoginWithValidCredentials\",\n"
            + "  \"testBody\": \"LoginPage loginPage = new LoginPage(driver);\\nloginPage.login(email, pass);\",\n"
            + "  \"featureFile\": \"src/test/resources/features/Login.feature\",\n"
            + "  \"scenarioTitle\": \"User logs in with valid credentials\",\n"
            + "  \"gherkinScenario\": \"Scenario: User logs in...\\n  Given ...\\n  When ...\\n  Then ...\"\n"
            + "}";

    private final LLMProvider llmProvider;
    private final String outputBasePath;
    private final String testOutputBasePath;
    private final boolean generateBDD;

    /**
     * @param llmProvider    The AI model to use for generation
     * @param outputBasePath Base path for generated Java files (e.g., "src/main/java")
     * @param generateBDD    Whether to also generate/update BDD .feature files
     */
    public EllithiumAIEngine(LLMProvider llmProvider, String outputBasePath, boolean generateBDD) {
        this(llmProvider, outputBasePath, "src/test/java", generateBDD);
    }

    public EllithiumAIEngine(LLMProvider llmProvider, String outputBasePath, String testOutputBasePath, boolean generateBDD) {
        this.llmProvider = Objects.requireNonNull(llmProvider, "llmProvider must not be null");
        this.outputBasePath = (outputBasePath == null || outputBasePath.isBlank()) ? "src/main/java" : outputBasePath;
        this.testOutputBasePath = (testOutputBasePath == null || testOutputBasePath.isBlank()) ? "src/test/java" : testOutputBasePath;
        this.generateBDD = generateBDD;
        AIConfigLoader.initialize();
    }

    /** Convenience constructor — defaults to "src/main/java" and BDD enabled. */
    public EllithiumAIEngine(LLMProvider llmProvider) {
        this(llmProvider, "src/main/java", true);
    }

    // ──────────────────────── Main Entry Point ────────────────────────

    /**
     * Reads natural-language test cases from the given file and generates all required assets.
     * Automatically detects JSON or text format from the file extension.
     * Skips test cases that have already been generated (idempotent).
     *
     * @param testCaseFilePath Path to the JSON or text file containing test cases
     */
    public void generateFrom(String testCaseFilePath) {
        Reporter.log("EllithiumAIEngine: Starting generation from: " + testCaseFilePath, LogLevel.INFO_YELLOW);

        // Select reader based on file extension
        TestCaseReader reader = testCaseFilePath.toLowerCase().endsWith(".json")
                ? new JsonTestCaseReader()
                : new TextTestCaseReader();

        List<TestCaseSource> testCases = reader.read(testCaseFilePath);
        if (testCases.isEmpty()) {
            Reporter.log("No test cases found in: " + testCaseFilePath, LogLevel.ERROR);
            return;
        }

        int generated = 0, skipped = 0;
        for (TestCaseSource testCase : testCases) {
            // Idempotency — skip already-generated test cases
            if (TraceabilityManager.isAlreadyGenerated(testCase.getTestId(), testCase.getSourceFile())) {
                Reporter.log("Skipping already-generated test: " + testCase.getTestId(), LogLevel.INFO_BLUE);
                skipped++;
                continue;
            }

            GeneratedAssets assets = processTestCase(testCase);
            if (assets != null) {
                TraceabilityManager.saveRecord(new TraceabilityRecord(testCase, assets));
                generated++;
            }
        }

        Reporter.log("EllithiumAIEngine: Done. Generated=" + generated + " Skipped=" + skipped, LogLevel.INFO_GREEN);
    }

    // ──────────────────────── Test Case Processing ────────────────────────

    /**
     * Processes a single test case through the LLM and writes generated files.
     * If the test case has a targetUrl, captures live DOM for grounded generation.
     */
    private GeneratedAssets processTestCase(TestCaseSource testCase) {
        try {
            Reporter.log("Generating assets for: " + testCase.getTestId() + " — " + testCase.getDescription(), LogLevel.INFO_BLUE);

            // Scrub PII from the description before sending to LLM
            String safeDescription = DataScrubber.scrub(testCase.getDescription());

            // Build user prompt with optional live DOM context
            String userPrompt = buildUserPrompt(testCase, safeDescription);

            // Query LLM
            String response = llmProvider.ask(SYSTEM_PROMPT, userPrompt);
            if (response == null || response.trim().isEmpty()) {
                Reporter.log("LLM returned an empty response for test: " + testCase.getTestId(), LogLevel.ERROR);
                return null;
            }

            // Parse structured JSON response
            return parseAndGenerateAssets(response, testCase);

        } catch (Exception e) {
            Reporter.log("Failed to generate assets for test " + testCase.getTestId()
                    + ": " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    /**
     * Builds the user prompt. If a target URL is provided, captures the live DOM
     * using a headless browser for grounded code generation.
     */
    private String buildUserPrompt(TestCaseSource testCase, String safeDescription) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate Ellithium test code for the following test case:\n");
        prompt.append("ID: ").append(testCase.getTestId()).append("\n");
        prompt.append("Description: ").append(safeDescription).append("\n");

        if (testCase.hasTargetUrl()) {
            prompt.append("Target URL: ").append(testCase.getTargetUrl()).append("\n");

            // Live DOM grounding — capture real DOM from the target URL
            String liveDom = captureLiveDom(testCase.getTargetUrl());
            if (liveDom != null && !liveDom.isBlank()) {
                prompt.append("\n## Live DOM Snapshot (from ").append(testCase.getTargetUrl()).append("):\n");
                prompt.append("Use these REAL elements for your locators. Do NOT hallucinate locators.\n");
                prompt.append("```\n").append(liveDom).append("\n```\n");
            }
        }

        return prompt.toString();
    }

    // ──────────────────────── Live DOM Capture ────────────────────────

    /**
     * Opens a headless Chrome browser, navigates to the URL, captures the page source,
     * minimizes it, and closes the browser. This provides real DOM context to the LLM
     * so it generates locators that actually exist on the page.
     *
     * @param url The target URL to capture
     * @return Minimized DOM string, or null if capture failed
     */
    private String captureLiveDom(String url) {
        WebDriver headlessDriver = null;
        try {
            Reporter.log("Live DOM Grounding: Capturing DOM from " + url, LogLevel.INFO_BLUE);

            // Use Ellithium's DriverFactory for headless Chrome
            headlessDriver = DriverFactory.getNewLocalDriver(LocalDriverType.Chrome, HeadlessMode.True);
            headlessDriver.get(url);

            // Small wait for dynamic content to render
            Thread.sleep(2000);

            // Use AX tree (universal, works on all browsers) with HTML fallback
            String optimizedDom = DOMMinimizer
                    .getOptimalDOMRepresentation(headlessDriver);
            String scrubbed = DataScrubber.scrub(optimizedDom);

            Reporter.log("Live DOM Grounding: Captured " + scrubbed.length() + " chars from " + url, LogLevel.INFO_GREEN);
            return scrubbed;

        } catch (Exception e) {
            Reporter.log("Live DOM Grounding failed for " + url + ": " + e.getMessage()
                    + " — proceeding without DOM context", LogLevel.WARN);
            return null;
        } finally {
            if (headlessDriver != null) {
                try {
                    DriverFactory.quitDriver();
                } catch (Exception ignored) {}
            }
        }
    }

    // ──────────────────────── Response Parsing & File Generation ────────────────────────

    /**
     * Parses the LLM's JSON response and writes the POM, test, and feature files.
     */
    private GeneratedAssets parseAndGenerateAssets(String llmResponse, TestCaseSource testCase) {
        try {
            JsonObject json = JsonParser.parseString(llmResponse).getAsJsonObject();

            String pomClass = readRequiredString(json, "pomClass", testCase);
            String pomPackage = readRequiredString(json, "pomPackage", testCase);
            String testClass = readRequiredString(json, "testClass", testCase);
            String testPackage = readRequiredString(json, "testPackage", testCase);
            String testMethod = readRequiredString(json, "testMethod", testCase);
            String testBody = readRequiredString(json, "testBody", testCase);
            if (pomClass == null || pomPackage == null || testClass == null
                    || testPackage == null || testMethod == null || testBody == null) {
                return null;
            }

            List<String> locatorFields = jsonArrayToList(json, "locatorFields");
            List<String> methodBodies  = jsonArrayToList(json, "methodBodies");
            List<String> pomMethods    = jsonArrayToList(json, "pomMethods");

            // Build file paths
            String pomPath  = outputBasePath + "/" + pomPackage.replace('.', '/') + "/" + pomClass + ".java";
            String testPath = testOutputBasePath + "/" + testPackage.replace('.', '/') + "/" + testClass + ".java";

            // Generate POM class
            if (!new java.io.File(pomPath).exists()) {
                PomClassGenerator.createPomClass(pomPath, pomPackage, pomClass, locatorFields, methodBodies);
            } else {
                // Inject fields and methods into existing class (allow uneven list lengths).
                int max = Math.max(locatorFields.size(), methodBodies.size());
                for (int i = 0; i < max; i++) {
                    String locatorField = i < locatorFields.size() ? locatorFields.get(i) : null;
                    String fullMethod = i < methodBodies.size() ? methodBodies.get(i) : null;
                    String methodSignature = fullMethod != null ? extractMethodSignature(fullMethod) : null;
                    String methodBody = fullMethod != null ? extractMethodBody(fullMethod) : null;
                    PomClassGenerator.injectIntoExistingPom(pomPath, locatorField, methodSignature, methodBody);
                }
            }

            // Generate TestNG test class with PROPER scaffolding
            String targetUrl = testCase.hasTargetUrl() ? testCase.getTargetUrl() : null;
            generateTestClass(testPath, testPackage, testClass, testMethod, testBody,
                    pomPackage, pomClass, targetUrl);

            // Generate BDD feature file if enabled
            String featureFile = null;
            if (generateBDD && json.has("featureFile")) {
                featureFile = json.get("featureFile").getAsString();
                String scenarioTitle = json.has("scenarioTitle") ? json.get("scenarioTitle").getAsString() : "";
                String gherkin = json.has("gherkinScenario") ? json.get("gherkinScenario").getAsString() : "";
                if (featureFile != null && !featureFile.isBlank() && gherkin != null && !gherkin.isBlank()
                        && !FeatureFileModifier.scenarioExists(featureFile, scenarioTitle)) {
                    FeatureFileModifier.appendScenarios(featureFile, gherkin);
                }
            }

            // Build and return asset record
            GeneratedAssets assets = new GeneratedAssets(testPackage, testClass, testMethod, pomClass, pomMethods, featureFile);
            Reporter.log("Assets generated for: " + testCase.getTestId(), LogLevel.INFO_GREEN);
            return assets;

        } catch (Exception e) {
            Reporter.log("Failed to parse LLM response for test " + testCase.getTestId()
                    + ": " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    // ──────────────────────── Test Class Generation (Fixed Scaffolding) ────────────────────────

    /**
     * Generates a TestNG test class with proper Ellithium scaffolding:
     * <ul>
     *   <li>{@code @BeforeMethod}: Initializes Chrome via {@code DriverFactory.getNewLocalDriver()}</li>
     *   <li>{@code @BeforeMethod}: Navigates to targetUrl if provided</li>
     *   <li>{@code @Test}: Contains the LLM-generated test body</li>
     *   <li>{@code @AfterMethod}: Calls {@code DriverFactory.quitDriver()}</li>
     * </ul>
     */
    private void generateTestClass(String outputPath, String packageName, String className,
                                    String testMethod, String testBody, String pomPackage,
                                    String pomClass, String targetUrl) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(outputPath);
            if (java.nio.file.Files.exists(path)) return; // Don't overwrite existing

            StringBuilder content = new StringBuilder();

            // Package declaration
            content.append("package ").append(packageName).append(";\n\n");

            // Imports
            content.append("import ").append(pomPackage).append(".").append(pomClass).append(";\n");
            content.append("import Ellithium.core.driver.DriverFactory;\n");
            content.append("import Ellithium.core.driver.LocalDriverType;\n");
            content.append("import Ellithium.core.driver.HeadlessMode;\n");
            content.append("import org.openqa.selenium.WebDriver;\n");
            content.append("import org.testng.annotations.AfterMethod;\n");
            content.append("import org.testng.annotations.BeforeMethod;\n");
            content.append("import org.testng.annotations.Test;\n\n");

            // Class declaration
            content.append("/**\n");
            content.append(" * Auto-generated by Ellithium AI Engine.\n");
            content.append(" */\n");
            content.append("public class ").append(className).append(" {\n\n");

            // Field
            content.append("    private WebDriver driver;\n\n");

            // @BeforeMethod — driver init + navigation
            content.append("    @BeforeMethod\n");
            content.append("    public void setUp() {\n");
            content.append("        driver = DriverFactory.getNewLocalDriver(LocalDriverType.Chrome, HeadlessMode.False);\n");
            if (targetUrl != null && !targetUrl.isBlank()) {
                content.append("        driver.get(\"").append(escapeJavaString(targetUrl)).append("\");\n");
            }
            content.append("    }\n\n");

            // @Test
            content.append("    @Test\n");
            content.append("    public void ").append(testMethod).append("() {\n");
            // Indent test body properly
            for (String line : testBody.trim().split("\\n")) {
                content.append("        ").append(line.trim()).append("\n");
            }
            content.append("    }\n\n");

            // @AfterMethod — driver cleanup
            content.append("    @AfterMethod\n");
            content.append("    public void tearDown() {\n");
            content.append("        DriverFactory.quitDriver();\n");
            content.append("    }\n");

            content.append("}\n");

            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path, content.toString());
            Reporter.log("Test class generated: " + outputPath, LogLevel.INFO_GREEN);
        } catch (java.io.IOException e) {
            Reporter.log("Failed to generate test class: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    // ──────────────────────── Utilities ────────────────────────

    private List<String> jsonArrayToList(JsonObject json, String key) {
        List<String> result = new ArrayList<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            json.getAsJsonArray(key).forEach(e -> result.add(e.getAsString()));
        }
        return result;
    }

    private String readRequiredString(JsonObject json, String key, TestCaseSource testCase) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            Reporter.log("LLM response missing required field '" + key
                    + "' for test " + testCase.getTestId(), LogLevel.ERROR);
            return null;
        }
        String value = json.get(key).getAsString();
        if (value == null || value.isBlank()) {
            Reporter.log("LLM response field '" + key + "' is blank for test " + testCase.getTestId(), LogLevel.ERROR);
            return null;
        }
        return value.trim();
    }

    private String extractMethodSignature(String fullMethod) {
        int braceIdx = fullMethod.indexOf('{');
        return braceIdx > 0 ? fullMethod.substring(0, braceIdx).trim() : fullMethod;
    }

    private String extractMethodBody(String fullMethod) {
        int start = fullMethod.indexOf('{') + 1;
        int end = fullMethod.lastIndexOf('}');
        return (start > 0 && end > start) ? fullMethod.substring(start, end).trim() : "";
    }

    /**
     * Escapes a string for use inside a Java string literal.
     */
    private static String escapeJavaString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
