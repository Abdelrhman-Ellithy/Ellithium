package AutoEllithiumSphere.properties;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
public class PropertyMaker {
    private static final String allurePath = System.getProperty("user.dir")+File.separator +"src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "allure";
    private static final String configPath = System.getProperty("user.dir")+File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "config";
    private static final String logPath =  System.getProperty("user.dir")+File.separator +"src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "log";
    public static void main(String[] args) {
        System.out.println("Application started with properties initialized.");
        try {
            // Set Allure properties
            setDataToProperties(allurePath, "allure.results.directory", "Test-Output/Reports/Allure/allure-results");
            setDataToProperties(allurePath, "allure.open.afterExecution", "true");

            // Create and set config properties
            setConfigProperties(configPath);

            // Create and set log properties
            setLogProperties(logPath);

        } catch (IOException e) {
            System.err.println("Error initializing properties: " + e.getMessage());
        }
    }
    private static void setDataToProperties(String filePath, String key, String value) throws IOException {
        Properties properties = new Properties();
        File file = new File(filePath);
        if (file.exists()) {
            properties.load(new java.io.FileInputStream(file));
        }
        properties.setProperty(key, value);
        properties.store(new FileOutputStream(file), null);
    }

    private static void setConfigProperties(String configFilePath) throws IOException {
        Properties configProperties = new Properties();
        configProperties.setProperty("AutoEllithiumSphereVersion", "1.0.0");
        configProperties.setProperty("allureVersion", "2.30.0");
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
        }
        configProperties.store(new FileOutputStream(configFile), null);
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
        if (!logFile.exists()) {
            logFile.getParentFile().mkdirs();
        }
        logProperties.store(new FileOutputStream(logFile), null);
    }
}
