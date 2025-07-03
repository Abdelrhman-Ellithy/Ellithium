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
        System.out.println("Application started with properties initialized.");
        initializePropertyFiles("allure");
        initializePropertyFiles("config");
        initializePropertyFiles("log4j2");
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
    public static void TestOutputSolver(){
        boolean result;
        boolean exists=PropertyHelper.keyExists(allurePath,"allure.report.directory");
        String allureReportPath;
        if (exists){
            allureReportPath= PropertyHelper.getDataFromProperties(allurePath,"allure.report.directory");
        }
        else{
            allureReportPath= "Test-Output"+File.separator+"Reports"+File.separator+"Allure"+File.separator+"allure-report";
        }
        if (!checkFileExists(allureReportPath)) {
            File allureReportDirectory = new File(allureReportPath);
            result=allureReportDirectory.mkdirs();
            if (!result){
                System.err.println("Failed to Automatically create directory: " + allureReportPath+ " Due to IDE Permissions you need to make it manually");
            }
        }
        exists=PropertyHelper.keyExists(allurePath,"allure.results.directory");
        String allureResultsPath;
        if (exists){
             allureResultsPath= PropertyHelper.getDataFromProperties(allurePath,"allure.results.directory");
        }
        else {
            allureResultsPath="Test-Output"+File.separator+"Reports"+File.separator+"Allure"+File.separator+"allure-results";
        }
        if (!checkFileExists(allureResultsPath)) {
            File allureResultsDirectory = new File(allureResultsPath);
            result=allureResultsDirectory.mkdirs();
            if (!result){
                System.err.println("Failed to Automatically create directory: " + allureResultsPath+ " Due to IDE Permissions you need to make it manually");
            }
        }
        if (!checkFileExists(ScreenShotPath)) {
            File ScreenShotsDirectory = new File(ScreenShotPath);
            result=ScreenShotsDirectory.mkdirs();
            if (!result){
                System.err.println("Failed to Automatically create directory: " + ScreenShotsDirectory+ " Due to IDE Permissions you need to make it manually");
            }
        }
        ScreenShotPath=ConfigContext.getCapturedScreenShotPath();
        if (!checkFileExists(ScreenShotPath)) {
            File ScreenShotsDirectory = new File(ScreenShotPath);
            result=ScreenShotsDirectory.mkdirs();
            if (!result){
                System.err.println("Failed to Automatically create directory: " + ScreenShotsDirectory+ " Due to IDE Permissions you need to make it manually");
            }
        }
        String RecordedExecutionsPath =ConfigContext.getRecordedExecutionsPath();
        if (!checkFileExists(RecordedExecutionsPath)) {
            File ScreenShotsDirectory = new File(RecordedExecutionsPath);
            result=ScreenShotsDirectory.mkdirs();
            if (!result){
                System.err.println("Failed to Automatically create directory: " + RecordedExecutionsPath+ " Due to IDE Permissions you need to make it manually");
            }
        }
        if (!checkFileExists(testPath)) {
            File testDataDirectory = new File(testPath);
            result=testDataDirectory.mkdirs();
            if (!result){
                System.err.println("Failed to Automatically create directory: " + testDataDirectory+ " Due to IDE Permissions you need to make it manually");
            }
        }
        if(!checkFileExists(checkerFolderPath)){
            File checkerDirectory = new File(checkerFolderPath);
            result=checkerDirectory.mkdirs();
            if (!result){
                System.err.println("Failed to Automatically create directory: " + checkerDirectory+ " Due to IDE Permissions you need to make it manually");
            }
        }
        if (!checkFileExists(checkerFilePath)) {
            File checkerFile = new File(checkerFilePath);
            try {
                result=checkerFile.createNewFile();
                if (!result){
                    System.err.println("Failed to Automatically create the json file: " + checkerFile+ " Due to IDE Permissions you need to make it manually");
                }
                Files.write(checkerFile.toPath(), (
                        "{\n LastDateRun\": null\n } ").getBytes());
            }catch (Exception e){
                System.err.println(e.getMessage());
            }
        }
        exists=PropertyHelper.keyExists(ConfigContext.getLogFilePath(), "property.basePath");
        String logFolderPath;
        if (exists){
            logFolderPath = PropertyHelper.getDataFromProperties(ConfigContext.getLogFilePath(), "property.basePath");
        }
        else{
            logFolderPath= "Test-Output"+File.separator+"Logs";
        }
        String logFilePath = logFolderPath.concat(File.separator)
                .concat(Objects.requireNonNull(PropertyHelper.getDataFromProperties(ConfigContext.getLogFilePath(), "property.fileName")));
        if (!checkFileExists(logFolderPath)) {
            File logDirectory = new File(logFolderPath);
            result=logDirectory.mkdirs();
            if (!result){
                System.err.println("Failed to Automatically create directory: " + logDirectory+ " Due to IDE Permissions you need to make it manually");
            }
        }
        if (!checkFileExists(logFilePath)) {
            File logFile = new File(logFilePath);
            try {
                result=logFile.createNewFile();
                if (!result){
                    System.err.println("Failed to Automatically create the text file: " + logFile+ " Due to IDE Permissions you need to make it manually");
                }
            }
            catch (IOException e){
                System.err.println(e.getMessage());
            }
        }
    }
}
