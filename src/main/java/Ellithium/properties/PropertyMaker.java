package Ellithium.properties;

import Ellithium.Utilities.logsUtils;
import Ellithium.com.Colors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyMaker {
    private static final String basePath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator;
    private static final String testPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "TestData";
    private static final String ScreenShotPath = System.getProperty("user.dir") + File.separator + "TestOutput" + File.separator + "ScreenShots" + File.separator + "Failed" ;
    private static final String allurePath = basePath + "allure.properties";
    private static final String configPath = basePath + "config.properties";
    private static final String logPath = basePath + "log4j2.properties";
    private static final String cucumberPath = basePath + "cucumber.properties";

    public static void main(String[] args) {
        System.out.println("Application started with properties initialized.");

        // Initialize each property file
        initializePropertyFiles("allure");
        initializePropertyFiles("config");
        initializePropertyFiles("log4j2");
        initializePropertyFiles("cucumber");

        // Check if TestData directory exists or create it
        File testDataDirectory = new File(testPath);
        if (testDataDirectory.exists()) {
            logsUtils.info(Colors.GREEN + "TestData Folder exists");
        } else {
            testDataDirectory.mkdirs();
            logsUtils.info(Colors.GREEN + "TestData Folder created");
        }
        // Check if TestData directory exists or create it
        File ScreenShotsDirectory = new File(ScreenShotPath);
        if (ScreenShotsDirectory.exists()) {
            logsUtils.info(Colors.GREEN + "ScreenShots Folder exists");
        } else {
            ScreenShotsDirectory.mkdirs();
            logsUtils.info(Colors.GREEN + "ScreenShots Folder created");
        }
    }

    private static void initializePropertyFiles(String propertyFileType) {
        switch (propertyFileType) {
            case "allure":
                if (!checkFileExists(allurePath)) {
                    try {
                        initializeFile(allurePath);
                        setDataToProperties(allurePath, "allure.results.directory", "Test-Output/Reports/Allure/allure-results");
                        setDataToProperties(allurePath, "allure.open.afterExecution", "true");
                        logsUtils.info(Colors.GREEN + "Allure properties initialized.");
                    } catch (IOException e) {
                        System.err.println("Error initializing allure properties: " + e.getMessage());
                    }
                } else {
                    logsUtils.info(Colors.GREEN + "Allure properties already exist.");
                }
                break;
            case "config":
                if (!checkFileExists(configPath)) {
                    try {
                        initializeFile(configPath);
                        setConfigProperties(configPath);
                        logsUtils.info(Colors.GREEN + "Config properties initialized.");
                    } catch (IOException e) {
                        System.err.println("Error initializing config properties: " + e.getMessage());
                    }
                } else {
                    logsUtils.info(Colors.GREEN + "Config properties already exist.");
                }
                break;
            case "log4j2":
                if (!checkFileExists(logPath)) {
                    try {
                        initializeFile(logPath);
                        setLogProperties(logPath);
                        logsUtils.info(Colors.GREEN + "Log4j2 properties initialized.");
                    } catch (IOException e) {
                        System.err.println("Error initializing log4j2 properties: " + e.getMessage());
                    }
                } else {
                    logsUtils.info(Colors.GREEN + "Log4j2 properties already exist.");
                }
                break;
            case "cucumber":
                if (!checkFileExists(cucumberPath)) {
                    try {
                        initializeFile(cucumberPath);
                        setCucumberProperties(cucumberPath);
                        logsUtils.info(Colors.GREEN + "Cucumber properties initialized.");
                    } catch (IOException e) {
                        System.err.println("Error initializing cucumber properties: " + e.getMessage());
                    }
                } else {
                    logsUtils.info(Colors.GREEN + "Cucumber properties already exist.");
                }
                break;
            default:
                System.err.println("Unknown property file type: " + propertyFileType);
                break;
        }
    }

    private static boolean checkFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    private static void initializeFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs(); // Create directory if it doesn't exist
            file.createNewFile(); // Create new file
        }
    }

    private static void setDataToProperties(String filePath, String key, String value) throws IOException {
        Properties properties = new Properties();
        File file = new File(filePath);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            }
        }
        properties.setProperty(key, value);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            properties.store(fos, null);
        }
    }

    private static void setConfigProperties(String configFilePath) throws IOException {
        Properties configProperties = new Properties();
        configProperties.setProperty("EllithiumVersion", "1.0.0");
        configProperties.setProperty("allureVersion", "2.30.0");
        configProperties.setProperty("closeDriverAfterScenario", "true");
        File configFile = new File(configFilePath);
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            configProperties.store(fos, null);
        }
    }

    private static void setCucumberProperties(String cucumberFilePath) throws IOException {
        Properties cucumberProperties = new Properties();
        cucumberProperties.setProperty("features", "");
        cucumberProperties.setProperty("glue", "");
        cucumberProperties.setProperty("tags", "");
        cucumberProperties.setProperty("monochrome", "true");
        cucumberProperties.setProperty("dryRun", "false");
        File cucumberFile = new File(cucumberFilePath);
        try (FileOutputStream fos = new FileOutputStream(cucumberFile)) {
            cucumberProperties.store(fos, null);
        }
    }

    private static void setLogProperties(String logFilePath) throws IOException {
        Properties logProperties = new Properties();
        logProperties.setProperty("property.basePath", "Test-Output/Logs");
        logProperties.setProperty("appenders", "file, console");
        logProperties.setProperty("appender.file.type", "File");
        logProperties.setProperty("appender.file.name", "FileAppender");
        logProperties.setProperty("appender.file.fileName", "${basePath}/Test.log");
        logProperties.setProperty("appender.file.append", "true");
        logProperties.setProperty("appender.file.layout.type", "PatternLayout");
        logProperties.setProperty("appender.file.layout.pattern", "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n");
        logProperties.setProperty("appender.console.type", "Console");
        logProperties.setProperty("appender.console.name", "ConsoleAppender");
        logProperties.setProperty("appender.console.layout.type", "PatternLayout");
        logProperties.setProperty("appender.console.layout.pattern", "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n");
        logProperties.setProperty("rootLogger.level", "TRACE");
        logProperties.setProperty("rootLogger.appenderRefs", "file, console");
        logProperties.setProperty("rootLogger.appenderRef.file.ref", "FileAppender");
        logProperties.setProperty("rootLogger.appenderRef.console.ref", "ConsoleAppender");
        File logFile = new File(logFilePath);
        try (FileOutputStream fos = new FileOutputStream(logFile)) {
            logProperties.store(fos, null);
        }
    }
}