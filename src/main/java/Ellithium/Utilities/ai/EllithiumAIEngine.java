package Ellithium.Utilities.ai;

import Ellithium.Utilities.ai.config.AIConfigLoader;
import Ellithium.Utilities.ai.generators.FeatureFileModifier;
import Ellithium.Utilities.ai.generators.PomClassGenerator;
import Ellithium.Utilities.ai.models.GeneratedAssets;
import Ellithium.Utilities.ai.models.TestCaseSource;
import Ellithium.Utilities.ai.models.TraceabilityRecord;
import Ellithium.Utilities.ai.provider.LLMProvider;
import Ellithium.Utilities.ai.readers.JsonTestCaseReader;
import Ellithium.Utilities.ai.readers.TestCaseReader;
import Ellithium.Utilities.ai.readers.TextTestCaseReader;
import Ellithium.Utilities.ai.sanitizers.DataScrubber;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
 *   <li>Sends each new test case to the configured LLM with Ellithium's system prompt</li>
 *   <li>Parses the LLM's structured JSON response</li>
 *   <li>Generates the POM class (or injects into existing) and the test class</li>
 *   <li>Optionally generates or updates the BDD .feature file</li>
 *   <li>Saves the mapping to {@code ellithium-ai-mappings.json}</li>
 * </ol>
 */
public class EllithiumAIEngine {

    private static final String SYSTEM_PROMPT =
            "You are an expert Java Selenium and Appium test automation engineer who uses the Ellithium framework.\n"
            + "You write clean, readable, business-level Page Object Model (POM) code for Web or Mobile applications.\n\n"
            + "## Ellithium POM Rules:\n"
            + "1. Page Objects extend nothing — they hold a private final DriverActions<?> driverActions;\n"
            + "2. Locators are: private final By locatorName = By.cssSelector(\"...\"); (for Web)\n"
            + "   OR private final By locatorName = AppiumBy.accessibilityId(\"...\"); (for Mobile).\n"
            + "3. Business methods call driverActions.elements().click(locator), .type(locator, text), etc.\n"
            + "4. Business methods return 'this' for fluent chaining, or a new Page Object for navigation.\n"
            + "5. Method names are business-level: login(String user, String pass), searchForProduct(String name).\n"
            + "6. Never use Thread.sleep(). Never use raw driver.findElement().\n\n"
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
            + "  \"testBody\": \"new LoginPage(driver).login(email, pass);\",\n"
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

    /**
     * Processes a single test case through the LLM and writes generated files.
     */
    private GeneratedAssets processTestCase(TestCaseSource testCase) {
        try {
            Reporter.log("Generating assets for: " + testCase.getTestId() + " — " + testCase.getDescription(), LogLevel.INFO_BLUE);

            // Scrub PII from the description before sending to LLM
            String safeDescription = DataScrubber.scrub(testCase.getDescription());

            // Query LLM
            String userPrompt = "Generate Ellithium test code for the following test case:\n"
                    + "ID: " + testCase.getTestId() + "\n"
                    + "Description: " + safeDescription;

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

            // Generate TestNG test class
            generateTestClass(testPath, testPackage, testClass, testMethod, testBody, pomPackage, pomClass);

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

    /** Generates a TestNG test class file. */
    private void generateTestClass(String outputPath, String packageName, String className,
                                    String testMethod, String testBody, String pomPackage, String pomClass) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(outputPath);
            if (java.nio.file.Files.exists(path)) return; // Don't overwrite existing

            String content = "package " + packageName + ";\n\n"
                    + "import " + pomPackage + "." + pomClass + ";\n"
                    + "import org.openqa.selenium.WebDriver;\n"
                    + "import org.testng.annotations.Test;\n\n"
                    + "/**\n * Auto-generated by Ellithium AI Engine.\n */\n"
                    + "public class " + className + " {\n\n"
                    + "    private WebDriver driver; // Initialize via Ellithium's DriverFactory\n\n"
                    + "    @Test\n"
                    + "    public void " + testMethod + "() {\n"
                    + "        " + testBody.trim() + "\n"
                    + "    }\n"
                    + "}\n";

            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path, content);
            Reporter.log("Test class generated: " + outputPath, LogLevel.INFO_GREEN);
        } catch (java.io.IOException e) {
            Reporter.log("Failed to generate test class: " + e.getMessage(), LogLevel.ERROR);
        }
    }

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
}
