<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=8a702946-9899-4a56-895b-82f8261a3c9a" width="0" height="0" style="display:none;" />

# 🌀 Ellithium
<table border="0" align="center">
  <tr>
    <td colspan="3" align="center">
      <a href="https://www.linkedin.com/in/abdelrahman-ellithy-3841a7270/" target="_blank">
        <img width="600" alt="Ellithium" src="src\main\resources\logo\Ellithium.png">
      </a>
    </td>
  </tr>
  <tr>
     <td colspan="3" align="center">
        <a href="https://app.codacy.com/gh/Abdelrhman-Ellithy/Ellithium/dashboard" target="_blank">
         <img alt="Codacy" src="https://img.shields.io/codacy/grade/4d6d48aba396411fa3170184330ba089?style=for-the-badge&color=blue&label=Code%20Quality" width="170">
        </a>
        <a href="https://github.com/Abdelrhman-Ellithy/Ellithium/blob/main/LICENSE" target="_blank">
            <img alt="License" src="https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Apache_Maven_logo.svg/1280px-Apache_Maven_logo.svg.png" width="170">
        </a>
        <a href="https://central.sonatype.com/artifact/io.github.abdelrhman-ellithy/ellithium" target="_blank">
            <img alt="Codacy" src="https://central.sonatype.com/maven-central-logo.svg?" width="200">
        </a>
        <a href="https://www.udemy.com/course/master-ellithium-unified-test-automation-framework/?couponCode=DISCOUNT" target="_blank">
            <img alt="Udemy Course" src="https://5.imimg.com/data5/SELLER/Default/2023/11/358764345/KZ/ZL/SV/12869081/udemy-e-learning.png" width="90" height="30">
        </a>
        <a href="https://abdelrhman-ellithy.github.io/ellithium.github.io/" target="_blank">
            <img alt="Documentation" src="https://img.shields.io/badge/Documentation-User%20Guide-blue" width="200">
        </a>
    </td>
  </tr>
</table>


## 🌀 Unified Test Automation Framework for Web, Mobile, API, SQL and NoSQL DBs Testing🚀

**Ellithium** is a Unified powerful, flexible, and scalable test automation framework designed to streamline and enhance the testing process. Leveraging tools such as **TestNG**, **Cucumber**, **Rest Assured**, and others, it provides an end-to-end solution for automated testing. With support for **BDD**, **cross-browser testing**, **parallel execution**, **headless testing**, and detailed **Allure reporting**, Ellithium aims to make your test automation faster, more reliable, and easier to maintain.  

### Supported Testing PlatForms
|Web |Mobile| API| DB|
| :---: |:---: |:---: |:---: |
| ✅  |✅  |✅   |✅  |

### Supported Cloud Mobile Device Test Labs
|BrowserStack | LambdaTest | Sauce Labs |
| :---: |:---: |:---: |
| ✅  |✅  |✅   | 

### Supported DB Types with Caching Mechanisms 🚀
|Mongo | Couchebase |Redis | MY_SQL| SQL_SERVER | ORACLE | IBM_DB2| POSTGRES_SQL|SQLITE |
| :---: |:---: |:---: |:---: |:---: |:---: |:---: |:---: |:---: |
| ✅  |✅  |✅   |✅  |✅  |✅  |✅   |✅  |✅  |

### Key Features
 
|BDD Support|Parallel Execution|Cross-Browser Testing|Headless Testing|Logging| Automatic Screenshots / Video Recording | User Stories Linking|Reporting|
|:--------:|:--------:|:--------:|:------:|:------:|:------:|:------:|:-----:|
|✅        |✅         |✅         |✅        |✅      |✅       |✅       |✅      |

|Command line Executor Interface|Synchronization Handling | CI/CD integration   |Test Data Generation|Notification System|
|:------:|:---: |:---: |:---: |:---: |
|✅       |✅  |✅  |✅   |✅   |

|AI Self-Healing|Codegen Recorder|LLM Test Generation|Live In-Session Test Extension|Manual Test → Script|BDD Feature Generation|
|:------:|:---: |:---: |:---: |:---: |:---: |
|✅       |✅  |✅  |✅   |✅   |✅   |

- **Allure Reporting**: Generate rich, interactive test reports with **Allure**, including test history and trend analysis.
- **Modular Design**: A well-structured and modular framework promoting code reuse and easy maintenance.
- **Executing OS Commands**: Execute system commands via the built-in **Command Executor Interface**.
- **API Testing**: Full support for API testing with **Rest Assured** for RESTful services.
- **Database Testing**: Extends coverage to both SQL and NoSQL databases, including MySQL, SQL Server, PostgreSQL, Oracle, IBM DB2, SQLite, Couchbase, MongoDB, and Redis, enabling comprehensive backend testing. 
- **Mobile Testing**: Test native, hybrid, and mobile apps on Android and IOS, with Appium integration and support for real devices and emulators. 
- **CI/CD Integration**: Seamless integration with popular CI/CD tools such as **Jenkins**, **GitHub Actions**, and **GitLab**.
- **Cloud Mobile Device Test Labs**: Reliable exeuction with cloud platforms such as **BrowserStack**, **LambdaTest**, and **Sauce Labs** with mobile app uploader.
- **Automatic Video Recording**: Configurable Web and Mobile execution Recording in synchronous and asynchronous modes based on video recording attachment flag.
- **Test Data Generation**: Dynamically generate test data using **Java Faker** for realistic names, emails, addresses, and more.
- **Email Notifications**: Automated SMTP email delivery with rich HTML reports and configurable triggers.
- **Slack Integration**: Webhook-based notifications with structured messages and channel targeting.
- **Exception Handling**: Robust mechanisms for capturing exceptions during test execution.

---

### 🤖 AI-Powered Features

Ellithium ships a built-in AI engine and Healing System — all features configured via `ai-config.properties`.

**🔧 AI Self-Healing** — When a locator breaks at runtime, Ellithium recovers automatically through a three-tier cascade:

| Tier | Strategy | How |
|:----:|:--------:|:----|
| 1 | Algorithmic | Mutation heuristics + element fingerprint matching. Fully offline. |
| 2 | Embedded Local Model | On-device model scores semantic similarity against the live DOM. |
| 3 | LLM | Scrubbed DOM snapshot sent to your configured LLM; returns a replacement locator. |

Supports `AUTO`, `SUGGEST_ONLY`, and `DISABLED` strategies — configured in `ai-config.properties`:

```properties
ai.healing.enabled=true
ai.healing.strategy=AUTO    # AUTO | SUGGEST_ONLY | DISABLED
ai.llm.provider=openai
ai.llm.apiKey=sk-...
ai.llm.model=gpt-4o
```

**🎥 Codegen Recorder** — Records browser interactions and generates ready-to-run code with uniqueness-verified, stability-ranked locators. Supports iframes, Shadow DOM, and Appium.

| Option | Default | Description |
|:------:|:-------:|-------------|
| `--target` | `test` | `test` — runnable TestNG class · `pom` — reusable Page Object |
| `--browser` | `chrome` | `chrome` · `edge` · `firefox` · `safari` |
| `--assert` | `soft` | `soft` (collect + assertAll) · `hard` (fail-fast) |
| `--output` | `src/test/java` | Output base directory |
| `--save-storage` / `--load-storage` | — | Persist or restore cookies + localStorage across sessions |

```bash
mvn -q exec:java -Dexec.mainClass=Ellithium.core.ai.codegen.CodegenCli \
    -Dexec.args="https://your-app.test --target test --output src/test/java --package pages"
```

**✍️ Live In-Session Test Extension** — Executes new test steps written in natural language against the currently open browser session, with locators resolved on the live DOM. No re-launch needed.

```java
EllithiumAIEngine.continueFrom(driver, llmProvider, "click the login button and verify the dashboard appears");
```

**🗂️ Manual Test Cases → Automated Scripts** — Converts JSON or plain-text manual test cases into Page Objects, TestNG tests, and BDD feature files in a single call. Idempotent across re-runs.

```java
EllithiumAIEngine engine = new EllithiumAIEngine(llmProvider);
engine.generateFrom("src/test/resources/test-cases.json");
// Produces: LoginPage.java · LoginTest.java · login.feature
```

**Supported LLM Providers:**

| OpenAI | Anthropic | Gemini | DeepSeek | Groq | Qwen | Local / Ollama | Custom Endpoint |
|:------:|:---------:|:------:|:--------:|:----:|:----:|:--------------:|:---------------:|
| ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

> 📖 Full configuration, examples and API reference — [**official documentation**](https://abdelrhman-ellithy.github.io/ellithium.github.io/)

---

### 👨‍💻 Supported OS with OS Command Executor Interface for Desktop OS

|Windows|Mac|Linux|Android|IOS|
|:-----:|:--:|:--:|:--:|:--:|
|✅     |✅  |✅  |✅  |✅  |

### 📄 Supported File Formats for Reading and Writing

Ellithium supports reading and writing data from various file formats, including:

|JSON|CSV|Excel|Properties|Jar|PDF|Text|
|:---:|:--:|:---:|:-----:|:--:|:--:|:--:|
|✅   |✅  |✅   |    ✅     |✅  |✅  |✅  |


## 👨‍💻 Developed using:
<div style="display: flex; flex-wrap: wrap; gap: 20px; align-items: center;">
  <a href="https://www.oracle.com/eg/java/technologies/downloads/" target="_blank"><img src="https://www.chrisjmendez.com/content/images/2019/01/Java_logo_icon.png" alt="Java" style="height:50px; width:auto; max-width:100px;"></a>
  <a href="https://maven.apache.org/" target="_blank"><img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT_kfjhmEkppRhYhJjGc4qrXahjpchftSrMKn_NmAElHA&s=10" alt="Maven" style="height:50px; width:auto; max-width:100px;"></a>
  <a href="https://www.jetbrains.com/idea/" target="_blank"><img src="https://resources.jetbrains.com/storage/products/intellij-idea/img/meta/intellij-idea_logo_300x300.png" alt="IntelliJ IDEA" style="height:50px; width:auto; max-width:100px;"></a>
  <a href="https://code.visualstudio.com/" target="_blank"><img src="https://code.visualstudio.com/assets/branding/code-stable.png" alt="VS Code" style="height:50px; width:auto; max-width:100px;"></a>
</div>
<br/>

## 🦸 Powered by:
<div style="display: flex; flex-wrap: wrap; gap: 20px; align-items: center;">
  <a href="https://www.selenium.dev/" target="_blank"><img src="https://www.selenium.dev/images/selenium_4_logo.png" alt="Selenium WebDriver" style="height:50px; width:auto; max-width:120px;"></a>
  <a href="https://rest-assured.io/" target="_blank"><img src="https://avatars.githubusercontent.com/u/19369327?s=280&v=4" alt="REST Assured" style="height:50px; width:auto; max-width:120px;"></a>
  <a href="https://cucumber.io/tools/cucumber-open/" target="_blank"><img src="https://raw.githubusercontent.com/cucumber/cucumber-ruby/main/docs/img/cucumber-open-logo.png" alt="Cucumber.io" style="height:50px; width:auto; max-width:120px;"></a>
  <a href="https://testng.org/doc/" target="_blank"><img src="https://545767148-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2F-MdBdUMSCcMYTyNwZf80%2Fuploads%2Fgit-blob-7e5b23257dbb5cc3262c56840d5cf9fa85b27dce%2Ftestng.png?alt=media" alt="TestNG" style="height:50px; width:auto; max-width:120px;"></a>
  <a href="https://docs.qameta.io/allure/" target="_blank"><img src="https://avatars.githubusercontent.com/u/5879127?s=200&v=4" alt="Allure Reports" style="height:50px; width:auto; max-width:120px;"></a>
  <a href="https://appium.io/docs/en/2.0/" target="_blank"><img src="https://raw.githubusercontent.com/appium/appium/master/packages/appium/docs/overrides/assets/images/appium-logo-horiz.png" alt="Appium" style="height:50px; width:auto; max-width:120px;"></a>
  <a href="https://github.com/brettwooldridge/HikariCP" target="_blank"><img src="https://github.com/brettwooldridge/HikariCP/raw/dev/logo.png" alt="HikariCP" style="height:50px; width:auto; max-width:120px;"></a>
</div>
<br/>

## 📚 Documentation

For comprehensive documentation and user guides, visit our official documentation site:

- [**User Guide & Documentation**](https://abdelrhman-ellithy.github.io/ellithium.github.io/) - Complete documentation including:
  - Detailed setup instructions
  - Framework architecture
  - API documentation
  - Best practices
  - Configuration guides
  - Examples and tutorials


### Prerequisites

Ensure you have the following installed:
- **Java Development Kit (JDK)**: 25
- **Maven**: 3.9.9 or higher (last version 3.9.16 recommended)

## 🏁 Getting Started

- **Follow these steps to set up a new Maven project with Ellithium:**

---
### Step 1: Create a New Maven Project

- **Create a new Maven project using your preferred IDE (e.g., IntelliJ IDEA).**

### Step 2: Update the `pom.xml`

- **Add the following configuration to your `pom.xml` to set the Java version, include the required dependencies, and configure the plugins.**

```xml

<properties>
    <maven.compiler.source>25</maven.compiler.source>
    <maven.compiler.target>25</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <Ellithiumversion>3.0.0-beta</Ellithiumversion>
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
    <!-- Maven Compiler Plugin -->
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.15.0</version>
        <configuration>
            <source>25</source>
            <target>25</target>
        </configuration>
    </plugin>

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.5.6</version>
        <configuration>
            <reportsDirectory>${project.build.directory}/surefire-reports</reportsDirectory>
            <testFailureIgnore>true</testFailureIgnore>
            <failIfNoTests>false</failIfNoTests>
            <systemPropertyVariables>
                <testng.dtd.http>true</testng.dtd.http>
            </systemPropertyVariables>
            <failIfNoSpecifiedTests>false</failIfNoSpecifiedTests>
            <trimStackTrace>false</trimStackTrace>
            <useFile>false</useFile>
            <encoding>UTF-8</encoding>
            <properties>
                <property>
                    <name>listener</name>
                    <value>Ellithium.core.execution.listener.CustomTestNGListener</value>
                </property>
            </properties>
        </configuration>
    </plugin>

    <!-- Exec Maven Plugin -->
    <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.6.3</version>
        <executions>
            <execution>
                <id>intialize</id>
                <phase>initialize</phase>
                <goals>
                    <goal>java</goal>
                </goals>
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
        <includes>
            <include>**/*</include>
        </includes>
    </resource>
</resources>
</build>
```



### Step 3: Open the Termenal in the Project directory then run this command
   ```bash
   mvn clean test
   ```

### Option 1: BDD Mode With Cucumber
- **[Demo-Project](https://github.com/Abdelrhman-Ellithy/Noon-Shopping-Website-Manual-Automation-) for setup use after follow the following steps**
### Step 1: Create a Test Runner Class

- **Create a Runner Package then create a new class named TestRunner that extends the `BDDSetup` class from Ellithium**.
- **Specify the paths for your feature files and step definitions using the `@CucumberOptions`.**

```java
package Runner;

import Ellithium.core.base.BDDSetup;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        glue = "stepDefinitions", // path to your stepDefinitions package, note you should use . instead of /
        features = "src/main/resources/features" // path to your features folder
        , tags = "@Run"
)
public class TestRunner extends BDDSetup {
}
```

### Step 2: Create a BaseStepDefinitions Class

- **Create a BaseStepDefinitions class to share the driver across all step definition classes**.

```java
package Base;

import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.*;
import org.openqa.selenium.WebDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import java.net.URL;

public class BaseStepDefinitions {
    protected WebDriver driver;
    protected AndroidDriver androidDriver;
    protected IOSDriver iosDriver;

    public BaseStepDefinitions() {

        // Web — Local
        DriverConfigBuilder config = new LocalDriverConfig(
                LocalDriverType.Chrome, HeadlessMode.False, PrivateMode.False,
                PageLoadStrategyMode.Normal, WebSecurityMode.SecureMode, SandboxMode.Sandbox);
        driver = DriverFactory.getNewDriver(config);

        // Web — Remote (Selenium Grid)
        DriverConfigBuilder remoteConfig = new RemoteDriverConfig(
                RemoteDriverType.Remote_Chrome, new URL("http://localhost:4444"),
                HeadlessMode.False, PrivateMode.False,
                PageLoadStrategyMode.Normal, WebSecurityMode.SecureMode, SandboxMode.Sandbox);
        driver = DriverFactory.getNewDriver(remoteConfig);

        // Mobile — Android / iOS
        androidDriver = DriverFactory.getNewMobileDriver(MobileDriverType.Android, new URL("http://localhost:4723"), options);
        iosDriver     = DriverFactory.getNewMobileDriver(MobileDriverType.IOS,     new URL("http://localhost:4723"), options);

        // DB — SQL  [MY_SQL | SQL_SERVER | POSTGRES_SQL | ORACLE | IBM_DB2]
        SQLDatabaseProvider db     = new SQLDatabaseProvider(SQLDBType.MY_SQL, username, password, serverIp, port, dbName);
        SQLDatabaseProvider sqlite = new SQLDatabaseProvider(SQLDBType.SQLITE, pathToSQLiteDatabase);

        // DB — NoSQL
        CouchbaseDatabaseProvider couchDB  = new CouchbaseDatabaseProvider(connectionString, username, password, bucketName);
        MongoDatabaseProvider     mongoDB  = new MongoDatabaseProvider(connectionString, dbName);
        RedisDatabaseProvider     redisDB  = new RedisDatabaseProvider(connectionString);
    }
}
```

**LocalDriverConfig defaults** (omitted params fall back to):

| Parameter | Default |
|-----------|---------|
| `HeadlessMode` | `False` |
| `PrivateMode` | `False` |
| `PageLoadStrategyMode` | `Normal` |
| `WebSecurityMode` | `SecureMode` |
| `SandboxMode` | `Sandbox` |

### Option 2: Non-BDD Mode
- **[Demo-Project](https://github.com/Abdelrhman-Ellithy/The-Internet-Herokuapp) for setup use after follow the following steps**

### Step 1: Create a BaseTest Class

- **Create a BaseTests class that extends `NonBDDSetup` — all test classes extend from it**.

```java
package UI_NonBDD;

import Ellithium.core.base.NonBDDSetup;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.*;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;

public class BaseTests extends NonBDDSetup {
    protected WebDriver driver;

    @BeforeClass
    public void setup() {
        DriverConfigBuilder config = new LocalDriverConfig(
                LocalDriverType.Chrome, HeadlessMode.False, PrivateMode.False,
                PageLoadStrategyMode.Normal, WebSecurityMode.SecureMode, SandboxMode.Sandbox);
        driver = DriverFactory.getNewDriver(config);
    }

    @AfterClass
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}
```

### Step 2: Create a Test Class extending BaseTests

```java
package UI_BDD;

import Base.BaseTests;
import Ellithium.Utilities.assertion.AssertionExecutor;
import Pages.LoginPage;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class loginTests extends BaseTests {
    @DataProvider(name = "invalidLoginData")
    Object[][] getInvalidTestData() {
        return new Object[][]{
                {"tomsmith", "hamada", "Your password is invalid"},
                {"hamada", "SuperSecretPassword!", "Your username is invalid"}
        };
    }

    LoginPage login;

    @Test(priority = 1, dataProvider = "invalidLoginData")
    public void invalidLogin(String username, String password, String expectedMessage) {
        login = home.clickFormAuthentication();
        login.setUserName(username);
        login.setPassword(password);
        var secureAreaPage = login.clickLoginBtn();
        String actualMessage = secureAreaPage.getLoginMassega();
        AssertionExecutor.hard.assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test(priority = 2)
    public void validLogin() {
        login = home.clickFormAuthentication();
        login.setPassword("SuperSecretPassword!");
        login.setUserName("tomsmith");
        var secureAreaPage = login.clickLoginBtn();
        String actualMessage = secureAreaPage.getLoginMassega();
        String expectedMessage = "You logged into a secure area!";
        AssertionExecutor.hard.assertTrue(actualMessage.contains(expectedMessage));
    }
}
```
### Step 3: Use DriverActions for Element Interactions and Synchronization

```java
package Pages;

import Ellithium.Utilities.interactions.DriverActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage {
    private final WebDriver driver;
    private final DriverActions driverActions;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        driverActions = new DriverActions(driver);
    }

    public void setUserName(String username) {
        driverActions.elements().sendData(By.id("username"), username, 5);
    }

    public void setPassword(String password) {
        driverActions.elements().sendData(By.id("password"), password, 5);
    }

    public void clickLoginBtn() {
        driverActions.elements().clickOnElement(By.cssSelector("button[type='submit']"));
    }

    public String getAlertMessage() {
        return driverActions.elements().getText(By.cssSelector(".flash"), 5);
    }
}
```

`DriverActions` handles waits, retries, and synchronization automatically — no explicit `Thread.sleep()` or `WebDriverWait` needed.
### *This should cover the steps to get your **Ellithium** framework up and running in a new Maven project.*

## 📬 Contact

- **For questions, suggestions, or feedback, feel free to reach out to Abdelrahman Ellithy**
**at [abdelarhmanellithy@gmail.com](mailto:abdelarhmanellithy@gmail.com).**

---
