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
}