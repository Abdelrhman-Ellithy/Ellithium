package AutoEllithiumSphere.properties;

import org.aeonbits.owner.ConfigFactory;

public class PropertyInitializer {

    private static Allure allureProperties;
    private static CustomConfig customConfig;
    private static Log4j log4jProperties;

    // Load and initialize all property configurations
    public static void initializeProperties() {
        allureProperties = ConfigFactory.create(Allure.class);
        customConfig = ConfigFactory.create(CustomConfig.class);
        log4jProperties = ConfigFactory.create(Log4j.class);

        // Optionally, you can print loaded properties or perform validation
        System.out.println("Loaded Allure Properties: ");
        System.out.println("Allure Results Directory: " + allureProperties.definedPath());
        System.out.println("Auto Open After Execution: " + allureProperties.openAfterExecution());

        System.out.println("Loaded Custom Config: ");
        System.out.println("AutoEllithiumSphere Version: " + customConfig.autoEllithiumSphereVersion());
        System.out.println("Allure Version: " + customConfig.allureVersion());

        System.out.println("Loaded Log4j Properties: ");
        System.out.println("Log Base Path: " + log4jProperties.basePath());
        System.out.println("Log Append Mode: " + log4jProperties.appenderFileAppend());

        // Call all methods of the Log4j interface and print the values
        callLog4jMethods();

        // Validate required properties
        validateProperties();
    }

    // Getters for accessing properties elsewhere in the application
    public static Allure getAllureProperties() {
        return allureProperties;
    }

    public static CustomConfig getCustomConfig() {
        return customConfig;
    }

    public static Log4j getLog4jProperties() {
        return log4jProperties;
    }

    // Additional logic to validate properties
    private static void validateProperties() {
        if (allureProperties.definedPath() == null || allureProperties.definedPath().isEmpty()) {
            throw new IllegalStateException("Allure defined path is not set.");
        }

        if (log4jProperties.basePath() == null || log4jProperties.basePath().isEmpty()) {
            throw new IllegalStateException("Log4j base path is not set.");
        }

        System.out.println("Property validation passed.");
    }

    // Call and print all Log4j properties
    private static void callLog4jMethods() {
        System.out.println("Log4j Base Path: " + log4jProperties.basePath());
        System.out.println("Log4j Appenders: " + log4jProperties.appenders());
        System.out.println("Log4j Console Appender Type: " + log4jProperties.appenderConsoleType());
        System.out.println("Log4j Console Appender Name: " + log4jProperties.appenderConsoleName());
        System.out.println("Log4j Console Layout Type: " + log4jProperties.appenderConsoleLayoutType());
        System.out.println("Log4j Console Layout Pattern: " + log4jProperties.appenderConsoleLayoutPattern());
        System.out.println("Log4j File Appender Type: " + log4jProperties.appenderFileType());
        System.out.println("Log4j File Appender Name: " + log4jProperties.appenderFileName());
        System.out.println("Log4j File Appender File Name: " + log4jProperties.appenderFile_FileName());
        System.out.println("Log4j File Append Mode: " + log4jProperties.appenderFileAppend());
        System.out.println("Log4j File Layout Type: " + log4jProperties.appenderFileLayoutType());
        System.out.println("Log4j File Layout Pattern: " + log4jProperties.appenderFileLayoutPattern());
        System.out.println("Log4j Root Logger Level: " + log4jProperties.rootLoggerLevel());
        System.out.println("Log4j Logger Appender Refs: " + log4jProperties.loggerAppenderRefs());
        System.out.println("Log4j File Appender Reference: " + log4jProperties.loggerFileAppender());
        System.out.println("Log4j Console Appender Reference: " + log4jProperties.loggerConsoleAppender());
    }

    // Method to reload properties if needed
    public static void reloadProperties() {
        initializeProperties();
        System.out.println("Properties reloaded.");
    }

    // Optional method to clear the loaded properties
    public static void clearProperties() {
        allureProperties = null;
        customConfig = null;
        log4jProperties = null;
        System.out.println("Properties cleared.");
    }
}
