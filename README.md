# 🌀 Ellithium
<table border="0" align="center">
 <tr>
  <td colspan="3" align="center">
   <a href="https://www.linkedin.com/in/abdelrahman-ellithy-3841a7270/" target="_blank"><img width="600" alt="SHAFT_ENGINE" src="src\main\resources\logo\Ellithium.png"></a>
  </td>
 </tr>
</table>

## 🌀 Enhanced Test Automation Framework for UI, API Tests 

**Ellithium** is a powerful, flexible, and scalable test automation framework designed to streamline and enhance the testing process. Leveraging tools such as **TestNG**, **Cucumber**, **Rest Assured**, and others, it provides an end-to-end solution for automated testing. With support for **BDD**, **cross-browser testing**, **parallel execution**, **headless testing**, and detailed **Allure reporting**, Ellithium aims to make your test automation faster, more reliable, and easier to maintain.

### 🚀 Key Features

- **Cross-Browser Testing**: Supports automated testing across multiple browsers, including **Chrome**, **Firefox**, and **Edge**.
- **Parallel Execution**: Execute tests in parallel to reduce overall test execution time and improve efficiency.
- **Headless Testing**: Run tests in headless mode for faster execution and to save resources.
- **BDD Support**: Implements Behavior-Driven Development (BDD) using **Cucumber** to create human-readable test scenarios.
- **Data-Driven Testing**: Read and write data from various formats such as **JSON, CSV, Excel, properties and Jar files**.
- **Custom Listeners**: Additional listeners for better control and enhanced test reporting.
- **Logging**: Comprehensive logging using **Log4j2** and **Dev Tools** for debugging and tracking purposes.
- **Allure Reporting**: Generate rich and interactive test reports with **Allure**.
- **Customizable Configurations**: Flexible configuration options for different test environments, browsers, and execution modes.
- **Modular Design**: A well-structured and modular design that promotes code reusability and easy maintenance.
- **Executing OS Commands** : Provides Command Executer Interface to execute Commands
- **Test Data Generation**: Supports dynamic test data generation using **Java Faker** for realistic names, emails, addresses, and more.
- **Attaching User Stories and Screenshots: Automatically attach relevant user stories and screenshots to test reports for enhanced traceability and visibility, allows seamless linking between test cases and their corresponding requirements or user stories.**
***
|BDD Support|Parallel Execution|Cross-Browser Testing | Logging | ScreenShot| User Stories Linking | Reporting |
|:------:|:-----------:|:-----------:|:-------:|:-----------:|:-----------:|:-----------:|
|✅         |     ✅    |         ✅           |       ✅|      ✅   |✅|      ✅ |
||

|Command Executer Interface|Test Data Generation|
|:------:|:-----------:|
|✅         |     ✅    |
||

|windows|Mac|Linux |
|:------:|:-----------:|:-----------:|
|✅         |     ✅    |✅    |
||

## 👨‍💻 Developed using:
<a href="https://www.oracle.com/eg/java/technologies/downloads/" target="_blank"><img src="https://www.chrisjmendez.com/content/images/2019/01/Java_logo_icon.png" alt="Java" height="50px"></a>
<a href="https://maven.apache.org/" target="_blank"><img src="https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Apache_Maven_logo.svg/340px-Apache_Maven_logo.svg.png" alt="Maven" height="50px"></a>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.jetbrains.com/idea/" target="_blank"><img src="https://upload.wikimedia.org/wikipedia/commons/thumb/9/9c/IntelliJ_IDEA_Icon.svg/1200px-IntelliJ_IDEA_Icon.svg.png" alt="IntelliJ IDEA" height="50px"></a>
<br/><br/>

## 🦸 Powered by:
<a href="https://www.selenium.dev/" target="_blank"><img src="https://www.selenium.dev/images/selenium_4_logo.png" alt="Selenium WebDriver" height="50px"></a>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://rest-assured.io/" target="_blank"><img src="https://avatars.githubusercontent.com/u/19369327?s=280&v=4" alt="REST Assured" height="50px"></a>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://cucumber.io/tools/cucumber-open/" target="_blank"><img src="https://raw.githubusercontent.com/cucumber/cucumber-ruby/main/docs/img/cucumber-open-logo.png" alt="Cucumber.io" height="50px"></a>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://testng.org/doc/" target="_blank"><img src="https://545767148-files.gitbook.io/~/files/v0/b/gitbook-x-prod.appspot.com/o/spaces%2F-MdBdUMSCcMYTyNwZf80%2Fuploads%2Fgit-blob-7e5b23257dbb5cc3262c56840d5cf9fa85b27dce%2Ftestng.png?alt=media" alt="TestNG" height="50px"></a>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://docs.qameta.io/allure/" target="_blank"><img src="https://avatars.githubusercontent.com/u/5879127?s=200&v=4" alt="Allure Reports" height="50px"></a>
<br/><br/>

### Prerequisites

Ensure you have the following installed:

- **Java Development Kit (JDK)**: Version 21
- **Maven**: 3.8.1 or higher
- **Browsers**: Latest versions of **Chrome, Firefox,** and **Edge**

### 📄 Supported File Formats

Ellithium supports reading and writing data from various file formats, including:

- **JSON**
- **CSV**
- **Excel**
- **Properties**
- **Jar**

|JSON|CSV|Excel | Logging | Properties| Jar |
|:------:|:-----------:|:-----------:|:-------:|:-----------:|:-----------:|
|✅         |     ✅    |         ✅           |       ✅|      ✅   |✅
||

## 🏁 Getting Started

- **Follow these steps to set up a new Maven project with Ellithium:**
Here is the updated **Getting Started** section formatted for your README file:

---

### Step 1: Create a New Maven Project

- **Create a new Maven project using your preferred IDE (e.g., IntelliJ IDEA).**

### Step 2: Update the `pom.xml`

- **Add the following configuration to your `pom.xml` to set the Java version, include the required dependencies, and configure the plugins.**

```xml
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <restversion>5.4.0</restversion>
        <Ellithiumversion>1.0.0</Ellithiumversion>
    </properties>
    <repositories>
        <repository>
            <id>github</id>
            <url>https://maven.pkg.github.com/Abdelrhman-Ellithy/Ellithium/</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>
    <dependencies>
        <dependency>
            <groupId>io.github</groupId>
            <artifactId>Ellithium</artifactId>
            <version>1.0.0</version>
        </dependency>
        <!-- Rest Assured Dependencies -->
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <version>${restversion}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <!-- Maven Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>

            <!-- Maven Surefire Plugin for TestNG Execution -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.0</version>
                <configuration>
                    <suiteXmlFiles>
                        <suiteXmlFile>TestNGRunner.xml</suiteXmlFile>
                    </suiteXmlFiles>
                    <properties>
                        <property>
                            <name>listener</name>
                            <value>Ellithium.com.CustomTestNGListener</value>
                        </property>
                    </properties>
                    <reportsDirectory>${project.build.directory}/surefire-reports</reportsDirectory>
                    <testFailureIgnore>true</testFailureIgnore>
                    <failIfNoTests>false</failIfNoTests>
                </configuration>
            </plugin>

            <!-- Exec Maven Plugin -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.0.0</version>
                <executions>
                    <execution>
                        <id>intialize</id>
                        <phase>initialize</phase>
                        <goals>
                            <goal>java</goal>
                        </goals>
                        <configuration>
                            <mainClass>Ellithium.properties.StartUpLoader</mainClass>
                            <includePluginDependencies>true</includePluginDependencies>
                            <classpathScope>compile</classpathScope>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
        <resources>
            <resource>
                <directory>src/main/resources/properties/default</directory>
                <includes>
                    <include>**/*.properties</include>
                </includes>
            </resource>
        </resources>
    </build>
```

### Step 3: Create a Test Runner Class

- **Create a runner class that extends the `SETUP` class from Ellithium**.
- **Specify the paths for your feature files and step definitions using the `@CucumberOptions`.**

```
package Runner;

import Ellithium.DriverSetup.SETUP;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        glue = "stepDefinitions", // path to your stepDefinitions package, note you should use . instead of /
        features="src/test/resources/features" // path to your features folder
)
public class TestRunner extends SETUP {
}
```
### Step 4: To Create a BaseStepDefinitions Class.

- **Create a BaseStepDefinitions class that will be used to extend the other StepDefinitions Classes from it**.
```
package Base;
import Ellithium.DriverSetup.DriverFactory;
import org.openqa.selenium.WebDriver;
public class BaseStepDefinitions {
    protected WebDriver driver;
    public BaseStepDefinitions(){
        driver= DriverFactory.getDriver();
    }
}
```

### 🗒️ Note:

- **To Create a WebDriver Instance use 'DriverFactory.getDriver()' class from Ellithium.DriverSetup**
- **like 'WebDriver driver= DriverFactory.getDriver();'**

### Step 5: Create a `TestNGRunner.xml` File

- **Next to your `pom.xml`, create a `TestNGRunner.xml` file for TestNG execution. You can modify the parameters as needed.**

```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="UI Chrome Browser Testing" verbose="1" data-provider-thread-count="1">
    <test name="UI Chrome">
        <parameter name="BrowserName" value="Chrome"/>
        <parameter name="HeadlessMode" value="false"/>
        <parameter name="PageLoadStrategy" value="Eager"/>
        <parameter name="PrivateMode" value="True"/>
        <parameter name="SandboxMode" value="Sandbox"/>
        <parameter name="WebSecurityMode" value="True"/>
        <classes>
            <class name="Runner.TestRunner"/>
        </classes>
    </test>
</suite>
```

- **The default values** if you **didn't add the paramaters** to the **TestNGRunner.xml** File are:

```
    @default("Chrome") String BrowserName, // can be Chrome or Edge or Firefox
    @default("false") String HeadlessMode, // can be true or false
    @default("Normal") String PageLoadStrategy, // can be Normal or Eager
    @default("True") String PrivateMode, // can be true or false
    @default("Sandbox") String SandboxMode, // can be Sandbox or NoSandbox
    @default("True") String WebSecurityMode // can be True or False 
)
```

- **Edit the number of data-provider-thread-count to the number of Scenarios you to run at a time** 
- **For parallel execution make it 2 or above**
 
- **Note you cannot run the Tests in paralle with diffrent Configurations**
---
- **This should cover the steps to get your **Ellithium** framework up and running in a new Maven project.**


## 📬 Contact

- **For questions, suggestions, or feedback, feel free to reach out to Abdelrahman Ellithy**
**at [abdelarhmanellithy@gmail.com](mailto:abdelarhmanellithy@gmail.com).**

---