# AGENTS.md — Ellithium

Reference for an AI coding agent helping a user **write tests with Ellithium** in
their own Maven project — setup, the public API surface, configuration, and the
bundled CLI tools. This is about *using* the framework, not developing it.

Full docs: **https://abdelrhman-ellithy.github.io/ellithium.github.io/**
Quick-start & feature tour: [README.md](README.md). Use this file for the parts
an agent needs inline (commands, method names, config keys) without a browsing tool.

## What Ellithium is

A unified Java test automation framework — Web (Selenium), Mobile (Appium), API
(Rest Assured), SQL + NoSQL DB testing — with BDD (Cucumber) and non-BDD (TestNG)
modes, Allure reporting, and a built-in AI locator self-healing engine. Maven
coordinate: `io.github.abdelrhman-ellithy:ellithium`. Requires JDK 25 and Maven
≥ 3.9.9.

## Add it to a project

```xml
<properties>
    <maven.compiler.source>25</maven.compiler.source>
    <maven.compiler.target>25</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <Ellithiumversion>3.1.0</Ellithiumversion>
</properties>
<dependencies>
    <dependency>
        <groupId>io.github.abdelrhman-ellithy</groupId>
        <artifactId>ellithium</artifactId>
        <version>${Ellithiumversion}</version>
    </dependency>
</dependencies>
<build>
  <plugins>
    <plugin> <!-- maven-compiler-plugin: release 25 --> </plugin>
    <plugin>
        <!-- maven-surefire-plugin: MUST register the listener or reporting/
             video/healing lifecycle hooks never fire -->
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.5.6</version>
        <configuration>
            <testFailureIgnore>true</testFailureIgnore>
            <useFile>false</useFile>
            <properties>
                <property>
                    <name>listener</name>
                    <value>Ellithium.core.execution.listener.CustomTestNGListener</value>
                </property>
            </properties>
        </configuration>
    </plugin>
    <plugin>
        <!-- exec-maven-plugin bound to `initialize`: on first `mvn clean test`
             this extracts config.properties / ai-config.properties / allure.properties /
             log4j2.properties / notifications.properties into the consumer project;
             on later runs it syncs in any new keys added by a framework upgrade.
             Without this plugin, no properties files are ever created. -->
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.6.3</version>
        <executions>
            <execution>
                <id>initialize</id>
                <phase>initialize</phase>
                <goals><goal>java</goal></goals>
                <configuration>
                    <mainClass>Ellithium.core.execution.Internal.Loader.StartUpLoader</mainClass>
                    <includePluginDependencies>true</includePluginDependencies>
                    <classpathScope>compile</classpathScope>
                </configuration>
            </execution>
        </executions>
    </plugin>
  </plugins>
  <resources>
      <resource>
          <directory>src/main/resources/properties</directory>
          <includes><include>**/*</include></includes>
      </resource>
  </resources>
</build>
```

Full copy-pasteable block (with exact versions) is in [README.md](README.md) §"Update
the pom.xml". Then: `mvn clean test`.

## Commands

```bash
mvn clean test                          # run the suite
mvn test -Dtest=LoginTests              # run one class
mvn test -Dtest=LoginTests#validLogin   # run one method
mvn clean test -Dsurefire.parallel=classes -Dsurefire.threadCount=4   # parallel run
```

Output lands under `Test-Output/` in the project root: Allure results/report
(`Test-Output/Reports/Allure/`), screenshots/videos, logs, and — if AI healing ran
and healed anything — `Test-Output/Reports/healing-report.md` (human-readable,
one table row per healed locator with confidence + reasoning) and
`Test-Output/healing-telemetry.json` (structured per-tier stats: attempts, used,
fell-through, avg score).

## Choose BDD or non-BDD

**BDD (Cucumber):** a runner class extends `Ellithium.core.base.BDDSetup`:

```java
@CucumberOptions(glue = "stepDefinitions", features = "src/main/resources/features", tags = "@Run")
public class TestRunner extends BDDSetup {}
```

**Non-BDD (plain TestNG):** no Ellithium base class needed — the reporting/lifecycle
hook is wired entirely through the Surefire `<listener>` property above. Just write
a plain class with `@BeforeClass`/`@AfterClass` that owns the `WebDriver`:

```java
public class BaseTests {
    protected WebDriver driver;
    @BeforeClass public void setup() {
        driver = DriverFactory.getNewDriver(new LocalDriverConfig(LocalDriverType.Chrome));
    }
    @AfterClass public void tearDown() { DriverFactory.quitDriver(); }
}
```

Full worked examples (step definitions, Page Objects, data providers) are in
README.md §"Getting Started".

## Getting a driver — `DriverFactory` / `*DriverConfig`

`DriverFactory.getNewDriver(DriverConfigBuilder)` dispatches on the concrete
config type. Config classes (all fluent, all in `Ellithium.core.driver`):

| Config class | Use for | Example |
|---|---|---|
| `LocalDriverConfig` | Local browser | `new LocalDriverConfig(LocalDriverType.Chrome, HeadlessMode.False, PrivateMode.False, PageLoadStrategyMode.Normal, WebSecurityMode.SecureMode, SandboxMode.Sandbox)` — or just `new LocalDriverConfig(LocalDriverType.Chrome)` for defaults |
| `RemoteDriverConfig` | Selenium Grid | `new RemoteDriverConfig(RemoteDriverType.Remote_Chrome, new URL("http://localhost:4444"), HeadlessMode.False, ...)` |
| `MobileDriverConfig` | Appium (Android/iOS) | `DriverFactory.getNewMobileDriver(MobileDriverType.Android, new URL("http://127.0.0.1:4723"), capabilities)`; fluent setters like `.setDeviceName()`, `.setPlatformVersion()`, `.setApp()`, `.setAppPackage()/.setAppActivity()` (Android), `.setBundleId()` (iOS), `.addCapability(key, value)` escape hatch for anything else |
| `CloudMobileDriverConfig` | BrowserStack / Sauce Labs / LambdaTest | extends `MobileDriverConfig`; `.setCloudProvider(CloudProviderType.BROWSERSTACK)`, `.setUsername()`, `.setAccessKey()`, `.setProjectName()`, `.setBuildName()` |

`DriverFactory.getCurrentDriver()` returns the thread-local driver for the calling
test thread; `DriverFactory.quitDriver()` tears it down.

## Interacting with elements — the `DriverActions` facade

One `DriverActions` (or `new DriverActions<>(driver)`) per thread; every call below
also has `(..., int timeoutSeconds)` and `(..., int timeout, int pollingMs)`
overloads, and auto-heals broken locators per your `ai-config.properties`. No
`Thread.sleep()`/explicit `WebDriverWait` needed.

| Accessor | Returns | Common methods |
|---|---|---|
| `.elements()` | `ElementActions` | `sendData(By, String)`, `clickOnElement(By)`, `getText(By)`, `getAttributeValue(By, String)`, `clearElement(By)`, `isElementDisplayed(By)`, `isElementPresent(By)`, `isElementEnabled(By)`, `uploadFile(By, String)` |
| `.waits()` | `WaitActions` | `waitForElementToBeVisible(By)`, `waitForElementToBeClickable(By)`, `waitForElementPresence(By)`, `waitForElementToDisappear(By, ...)`, `waitForTextToBePresentInElement(By, String)`, `waitForUrlContains(String)`, `waitForTitleContains(String)` |
| `.select()` | `SelectActions` | `selectDropdownByText/ByValue/ByIndex(By, ...)`, `getDropdownSelectedOptions(By)`, `deselectAll(By)` |
| `.mouse()` | `MouseActions` | `hoverOverElement(By)`, `hoverAndClick(By, By)`, `dragAndDrop(By, By)`, `rightClick(By)`, `doubleClick(By)`, `moveSliderTo(By, By, float)` |
| `.frames()` | `FrameActions` | `switchToFrameByIndex/ByNameOrID/ByElement(...)`, `switchToDefaultContent()` |
| `.windows()` | `WindowActions` | `switchToNewWindow(String)`, `switchToPopupWindow(String)`, `closeCurrentWindow()`, `getAllWindowHandles()`, `maximizeWindow()` |
| `.alerts()` | `AlertActions` | `accept()`, `dismiss()`, `getText()`, `sendData(String)` |
| `.cookies()` | `CookieActions` | `addCookie(Cookie)`, `getCookies()`, `deleteAllCookies()` |
| `.navigation()` | `NavigationActions` | `navigateToUrl(String)`, `refreshPage()`, `navigateBack()/navigateForward()` |
| `.JSActions()` | `JavaScriptActions` | `javascriptClick(By)`, `scrollToElement(By)`, `setElementValueUsingJS(By, String)` |
| `.mobileActions()` | `MobileActions` (Appium only) | `tap(By)`, `longPress(By)`, `swipe(By, direction, percent)`, `scroll(By, direction)`, `pinch(By, zoomIn, percent)` |
| `.sleep()` | `Sleep` | static-style `sleepMillis/Seconds/Minutes` — last resort only, prefer `.waits()` |

## Database & API testing

- SQL: `new SQLDatabaseProvider(SQLDBType.MY_SQL | SQL_SERVER | POSTGRES_SQL | ORACLE | IBM_DB2, user, pass, host, port, dbName)`, or `SQLDBType.SQLITE` with just a file path.
- NoSQL: `MongoDatabaseProvider(connectionString, dbName)`, `RedisDatabaseProvider(connectionString)`, `CouchbaseDatabaseProvider(connectionString, user, pass, bucket)`.
- API: standard **Rest Assured** — Ellithium doesn't wrap it, just bundles it as a dependency + reports it in Allure.

## AI self-healing — configuring it in your project

Once bootstrapped, your project has `src/main/resources/properties/ai-config.properties`
with every key commented inline. The ones you'll actually touch:

| Key | Default | Meaning |
|---|---|---|
| `ai.healing.strategy` | `DISABLED` | `DISABLED` \| `HEAL_AND_CONTINUE` \| `HEAL_AND_NOTIFY` \| `SUGGEST_ONLY` |
| `ai.tier3.enabled` | `true` | Turn the LLM tier on/off (Tiers 1–2 are always-on, offline, free) |
| `ai.llm.provider` / `ai.llm.apiKey` / `ai.llm.model` | — | LLM provider config for Tier 3 (`openai`, `anthropic`, `gemini`, `deepseek`, `groq`, `qwen`, local/Ollama, or a custom endpoint) |
| `ai.onnx.similarityThreshold` | `0.70` | Minimum score for the local model tier to accept a heal |
| `ai.healing.storeThreshold` | `0.75` | Minimum score for a heal to be **persisted** (below this, it's used once but not remembered/patched into source) |
| `ai.execution.mode` | `LOCAL` | `LOCAL` patches healed locators directly into your source files; `CI` never touches source, just reports |
| `ai.vision.allowWeb` / `ai.vision.allowMobile` | `false` | Send screenshots to the LLM for visual healing — disable for any app handling personal data |

When a locator breaks and heals, check `Test-Output/Reports/healing-report.md`
(readable log) and `Test-Output/healing-telemetry.json` (per-tier stats) after the run.

**Calibrating thresholds against your own app:** after you've accumulated some
baselines, run

```bash
mvn -q exec:java -Dexec.mainClass=Ellithium.core.ai.healing.ModelCalibrationRunner
```

It reads your project's `Test-Output/healing-baselines.json`, sweeps thresholds
against real positive/negative pairs, and writes recommended
`ai.onnx.similarityThreshold` / `ai.healing.storeThreshold` values to
`Test-Output/calibration-results.json` (also printed to console).

## Codegen — record a browser session, get runnable code

```bash
mvn -q exec:java -Dexec.mainClass=Ellithium.core.ai.codegen.CodegenCli \
    -Dexec.args="https://your-app.test --target test --output src/test/java --package pages"
```

| Flag | Default | Meaning |
|---|---|---|
| `--target` | `test` | `test` (runnable TestNG class) \| `pom` (reusable Page Object) |
| `--browser` | `chrome` | `chrome` \| `edge` \| `firefox` \| `safari` |
| `--assert` | `soft` | `soft` (collect + assertAll) \| `hard` (fail-fast) |
| `--output` | `src/test/java` | Output base directory |
| `--package` | `Pages` | Package name for a `--target pom` class |
| `--class` | derived from URL | Explicit generated class name |
| `--headless` | off | Run the recording session headless |
| `--llm-polish` | off | Use the configured LLM to refine/name generated locators |
| `--save-storage <file>` / `--load-storage <file>` | — | Persist/restore cookies + localStorage across recording sessions |

URL is optional — omit it to open `about:blank` and navigate manually. The
recorded locators are uniqueness-verified and stability-ranked; supports iframes,
Shadow DOM, and Appium sessions.

## AI-assisted authoring

```java
// Extend a live, already-open browser session with new steps in plain English:
EllithiumAIEngine.continueFrom(driver, llmProvider, "click the login button and verify the dashboard appears");

// Turn manual test cases (JSON or plain text) into Page Object + TestNG test + BDD feature:
EllithiumAIEngine engine = new EllithiumAIEngine(llmProvider);
engine.generateFrom("src/test/resources/test-cases.json");
```

`generateFrom` is idempotent across re-runs — it tracks what it already generated
and skips duplicates.

## Where to go next

- Full docs, architecture, and tutorials: https://abdelrhman-ellithy.github.io/ellithium.github.io/
- Quick-start + copy-paste pom.xml + BDD/non-BDD examples: [README.md](README.md)
- Demo projects: [Noon-Shopping-Website-Manual-Automation](https://github.com/Abdelrhman-Ellithy/Noon-Shopping-Website-Manual-Automation-) (BDD), [The-Internet-Herokuapp](https://github.com/Abdelrhman-Ellithy/The-Internet-Herokuapp) (non-BDD)
- Questions: abdelarhmanellithy@gmail.com
