package Ellithium.core.execution.Internal.Loader;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.managment.ConfigContext;
import static Ellithium.Utilities.helpers.JarExtractor.extractFileFromJar;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

public class StartUpLoader {
    private static String
                        testPath,
                        ScreenShotPath,
                        allurePath,
                        notificationPath,
                        configPath,
                        logPath,
                        checkerFilePath,
                        checkerFolderPath
    ;
    public static void main(String[] args) throws IOException {
        testPath = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "TestData";
        checkerFilePath=ConfigContext.getCheckerFilePath();
        checkerFolderPath=ConfigContext.getCheckerFolderPath();
        ScreenShotPath = ConfigContext.getFailedScreenShotPath();
        allurePath = ConfigContext.getAllureFilePath();
        configPath = ConfigContext.getConfigFilePath();
        logPath = ConfigContext.getLogFilePath();
        notificationPath=ConfigContext.getNotificationFilePath();
        System.out.println("Application started with properties initialized.");
        initializePropertyFiles("allure");
        initializePropertyFiles("config");
        initializePropertyFiles("log4j2");
        initializePropertyFiles("notifications");
        TestOutputSolver();
    }
    private static void initializePropertyFiles(String propertyFileType) {
        switch (propertyFileType) {
            case "allure":
                if (!checkFileExists(allurePath)) {
                    File jarFile = findJarFile();
                    if (jarFile != null) {
                        extractFileFromJar(jarFile, "properties/allure.properties", new File(allurePath));
                    } else {
                        System.err.println("JAR file not found.");
                    }
                }
                break;
            case "config":
                if (!checkFileExists(configPath)) {
                    File jarFile = findJarFile();
                    if (jarFile != null) {
                        extractFileFromJar(jarFile, "properties/config.properties", new File(configPath));
                    } else {
                        System.err.println("JAR file not found.");
                    }
                }
                break;
            case "log4j2":
                if (!checkFileExists(logPath)) {
                    File jarFile = findJarFile();
                    if (jarFile != null) {
                        extractFileFromJar(jarFile, "properties/log4j2.properties", new File(logPath));
                    } else {
                        System.err.println("JAR file not found.");
                    }
                }
                break;
            case "notifications":
                if (!checkFileExists(notificationPath)) {
                    File jarFile = findJarFile();
                    if (jarFile != null) {
                        extractFileFromJar(jarFile, "properties/notifications.properties", new File(notificationPath));
                    } else {
                        System.err.println("JAR file not found.");
                    }
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
    public static File findJarFile() {
        String repoPath = ConfigContext.getEllithiumRepoPath();
        File repoDir = new File(repoPath);
        File[] versionDirs = repoDir.listFiles(File::isDirectory);
        if (versionDirs != null && versionDirs.length > 0) {
            Arrays.sort(versionDirs, (dir1, dir2) -> compareVersions(dir1.getName(), dir2.getName()));
            File highestVersionDir = versionDirs[versionDirs.length - 1];
            Pattern jarPattern = Pattern.compile("^ellithium-\\d+(\\.\\d+)*\\.jar$");
            File[] jarFiles = highestVersionDir.listFiles((dir, name) -> jarPattern.matcher(name).matches());
            if (jarFiles != null && jarFiles.length > 0) {
                return jarFiles[0];
            }
        }
        return null;
    }
    /**
     * Compare two version strings in the format 'X.X.X' or similar.
     * This method returns a negative integer, zero, or a positive integer
     * if the first version is less than, equal to, or greater than the second version, respectively.
     */
    private static int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        // Normalize length (compare as many digits as possible)
        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (v1 != v2) {
                return Integer.compare(v1, v2);
            }
        }
        return 0; // Versions are equal
    }
    private static void createDirectoryIfNotExists(String path) {
        if (!checkFileExists(path)) {
            File directory = new File(path);
            boolean result = directory.mkdirs();
            if (!result) {
                System.err.println("Failed to Automatically create directory: " + path + " Due to IDE Permissions you need to make it manually");
            }
        }
    }

    private static void createFileIfNotExists(String path, String defaultContent, String fileTypeName) {
        if (!checkFileExists(path)) {
            File file = new File(path);
            try {
                boolean result = file.createNewFile();
                if (!result) {
                    System.err.println("Failed to Automatically create the " + fileTypeName + " file: " + file + " Due to IDE Permissions you need to make it manually");
                }
                if (defaultContent != null) {
                    Files.write(file.toPath(), defaultContent.getBytes());
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static void TestOutputSolver(){
        boolean exists = PropertyHelper.keyExists(allurePath,"allure.report.directory");
        String allureReportPath = exists ? 
            PropertyHelper.getDataFromProperties(allurePath,"allure.report.directory") : 
            "Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-report";
        createDirectoryIfNotExists(allureReportPath);

        exists = PropertyHelper.keyExists(allurePath,"allure.results.directory");
        String allureResultsPath = exists ? 
            PropertyHelper.getDataFromProperties(allurePath,"allure.results.directory") : 
            "Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-results";
        createDirectoryIfNotExists(allureResultsPath);

        createDirectoryIfNotExists(ScreenShotPath);
        
        ScreenShotPath = ConfigContext.getCapturedScreenShotPath();
        createDirectoryIfNotExists(ScreenShotPath);

        String recordedExecutionsPath = ConfigContext.getRecordedExecutionsPath();
        createDirectoryIfNotExists(recordedExecutionsPath);

        createDirectoryIfNotExists(testPath);
        createDirectoryIfNotExists(checkerFolderPath);

        createFileIfNotExists(checkerFilePath, "{\n LastDateRun\": null\n } ", "json");

        exists = PropertyHelper.keyExists(ConfigContext.getLogFilePath(), "property.basePath");
        String logFolderPath = exists ? 
            PropertyHelper.getDataFromProperties(ConfigContext.getLogFilePath(), "property.basePath") : 
            "Test-Output" + File.separator + "Logs";
        
        createDirectoryIfNotExists(logFolderPath);

        String logFilePath = logFolderPath.concat(File.separator)
                .concat(Objects.requireNonNull(PropertyHelper.getDataFromProperties(ConfigContext.getLogFilePath(), "property.fileName")));
        createFileIfNotExists(logFilePath, null, "text");
    }
}
