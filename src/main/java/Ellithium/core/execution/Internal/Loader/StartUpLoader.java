package Ellithium.core.execution.Internal.Loader;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.management.ConfigContext;
import static Ellithium.Utilities.helpers.JarExtractor.extractFileFromJar;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;

public class StartUpLoader {
    private static String
                        testPath,
                        ScreenShotPath,
                        allurePath,
                        notificationPath,
                        configPath,
                        logPath,
                        aiPath,
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
        aiPath = ConfigContext.getAiFilePath();
        logPath = ConfigContext.getLogFilePath();
        notificationPath=ConfigContext.getNotificationFilePath();
        System.out.println("Application started with properties initialized.");
        initializePropertyFiles("ai-config");
        initializePropertyFiles("allure");
        initializePropertyFiles("config");
        initializePropertyFiles("log4j2");
        initializePropertyFiles("notifications");
        TestOutputSolver();
    }
    private static void initializePropertyFiles(String propertyFileType) {
        switch (propertyFileType) {
            case "allure"        -> syncOrExtract(allurePath, "properties/allure.properties");
            case "config"        -> syncOrExtract(configPath, "properties/config.properties");
            case "log4j2"        -> syncOrExtract(logPath, "properties/log4j2.properties");
            case "notifications" -> syncOrExtract(notificationPath, "properties/notifications.properties");
            case "ai-config"     -> syncOrExtract(aiPath, "properties/ai-config.properties");
            default -> System.err.println("Unknown property file type: " + propertyFileType);
        }
    }

    /**
     * Extracts the bundled default file when the target doesn't exist yet (first run), or —
     * when the user already has their own copy — appends any property key that a newer
     * Ellithium version added but this file predates, so upgrading never leaves a config file
     * silently missing a key it doesn't know exists.
     */
    private static void syncOrExtract(String targetPath, String jarEntryPath) {
        File jarFile = findJarFile();
        if (jarFile == null) {
            if (!checkFileExists(targetPath)) System.err.println("JAR file not found.");
            return;
        }
        if (!checkFileExists(targetPath)) {
            extractFileFromJar(jarFile, jarEntryPath, new File(targetPath));
        } else {
            PropertyFileSynchronizer.syncMissingKeys(jarFile, jarEntryPath, targetPath,
                    ConfigContext.getEllithuiumVersion());
        }
    }
    private static boolean checkFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }
    /**
     * Locates the Ellithium JAR that is actually in use for this build. Resolved from the
     * running JVM's own code source first — the exact jar {@link StartUpLoader}'s class was
     * loaded from, which Maven guarantees matches the consuming project's declared dependency
     * version (release, beta, or SNAPSHOT alike) — falling back to a highest-installed-version
     * scan of the local Maven repository only when the class wasn't loaded from a jar at all
     * (e.g. running against exploded {@code target/classes} in an IDE/dev build).
     * <p>
     * The code-source resolution is load-bearing, not an optimization: a local {@code .m2}
     * repository commonly accumulates multiple installed Ellithium versions from different
     * projects on the same machine (e.g. one project pinned to a stable release, another testing
     * a beta). A "highest version wins" scan has no way to know which of those the *current*
     * build actually depends on, and can silently resolve and extract resources from the wrong
     * one — most visibly when a numerically-newer beta/pre-release is cached alongside an older
     * stable version a project still depends on.
     */
    public static File findJarFile() {
        File codeSourceJar = findJarFromCodeSource();
        return codeSourceJar != null ? codeSourceJar : findHighestVersionJarFromRepo();
    }

    private static File findJarFromCodeSource() {
        try {
            var codeSource = StartUpLoader.class.getProtectionDomain().getCodeSource();
            if (codeSource == null) return null;
            File location = new File(codeSource.getLocation().toURI());
            return (location.isFile() && location.getName().endsWith(".jar")) ? location : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Matches the main artifact by its exact deterministic filename
     * ({@code ellithium-<version>.jar}, where {@code <version>} is literally the containing
     * directory's name) rather than a pattern — a version directory also commonly contains
     * {@code ellithium-<version>-sources.jar} and {@code ellithium-<version>-javadoc.jar} (fetched
     * by an IDE or {@code -Dclassifier=sources}), and {@code File.listFiles()} order is
     * unspecified, so a loose pattern match could non-deterministically return a sources/javadoc
     * JAR — which has no {@code properties/*} resource entries, causing every downstream
     * extraction to silently no-op.
     */
    private static File findHighestVersionJarFromRepo() {
        String repoPath = ConfigContext.getEllithiumRepoPath();
        File repoDir = new File(repoPath);
        File[] versionDirs = repoDir.listFiles(File::isDirectory);
        if (versionDirs != null && versionDirs.length > 0) {
            Arrays.sort(versionDirs, (dir1, dir2) -> compareVersions(dir1.getName(), dir2.getName()));
            File highestVersionDir = versionDirs[versionDirs.length - 1];
            File mainJar = new File(highestVersionDir, "ellithium-" + highestVersionDir.getName() + ".jar");
            if (mainJar.exists()) {
                return mainJar;
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
        String[] parts1 = version1.replaceAll("-.*$", "").split("\\.");
        String[] parts2 = version2.replaceAll("-.*$", "").split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (v1 != v2) return Integer.compare(v1, v2);
        }
        String q1 = version1.contains("-") ? version1.substring(version1.indexOf('-') + 1) : "";
        String q2 = version2.contains("-") ? version2.substring(version2.indexOf('-') + 1) : "";
        if (q1.isEmpty() && q2.isEmpty()) return 0;
        if (q1.isEmpty()) return 1;
        if (q2.isEmpty()) return -1;
        return q1.compareToIgnoreCase(q2);
    }
    private static void TestOutputSolver(){
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
                        "{\n \"LastRunDate\": null\n}").getBytes());
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
