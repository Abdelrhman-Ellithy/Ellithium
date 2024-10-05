package Ellithium.core.execution.Internal.Loader;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.config.managment.ConfigContext;

import static Ellithium.Utilities.helpers.JarExtractor.extractFileFromJar;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class StartUpLoader {
    private static String basePath,
                        testPath,
                        ScreenShotPath,
                        allurePath,
                        configPath,
                        logPath,
                        allureListenerPath,
                        emailFilePath
    ;

    public static void main(String[] args) throws IOException {
        basePath=ConfigContext.getBasePropertyFolderPath();
        testPath = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "TestData";
        ScreenShotPath =  "Test-Output" + File.separator + "ScreenShots" + File.separator + "Failed" ;
        allurePath = ConfigContext.getAllureFilePath()+ ".properties";
        configPath = ConfigContext.getConfigFilePath()+ ".properties";
        logPath = ConfigContext.getLogFilePath()+ ".properties";
        emailFilePath=ConfigContext.getEmailFilePath()+ ".properties";
        System.out.println("Application started with properties initialized.");
        initializePropertyFiles("allure");
        initializePropertyFiles("config");
        initializePropertyFiles("log4j2");
        initializePropertyFiles("email");
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
            case "email":
                if (!checkFileExists(emailFilePath)) {
                    File jarFile = findJarFile();
                    if (jarFile != null) {
                        System.err.println(jarFile.getPath());
                        extractFileFromJar(jarFile, "properties/email.properties", new File(emailFilePath));
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
    public static void extractAllureFolderFromJar(File jarFile, File targetDirectory) throws IOException {
        if (!targetDirectory.exists()) {
            Files.createDirectory(targetDirectory.toPath());
        }
         try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith("allure")) {
                    File targetFile = new File(targetDirectory, entry.getName().substring("allure".length()));
                    if (entry.isDirectory()) {
                        targetFile.mkdirs();
                    } else {
                        Files.copy(jar.getInputStream(entry), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        if (entry.getName().endsWith("allure") || entry.getName().endsWith("allure.bat")) {
                            targetFile.setExecutable(true);
                        }
                    }
                }
            }
        }
        catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
    public static void TestOutputSolver(){
        final String allureFile = basePath + "allure";
        String allureReportPath= PropertyHelper.getDataFromProperties(allureFile,"allure.report.directory");
        if (!checkFileExists(allureReportPath)) {
            File allureReportDirectory = new File(allureReportPath);
            allureReportDirectory.mkdirs();
        }
        String allureResultsPath= PropertyHelper.getDataFromProperties(allureFile,"allure.report.directory");
        if (!checkFileExists(allureResultsPath)) {
            File allureResultsDirectory = new File(allureResultsPath);
            allureResultsDirectory.mkdirs();
        }
        if (!checkFileExists(ScreenShotPath)) {
            File ScreenShotsDirectory = new File(ScreenShotPath);
            ScreenShotsDirectory.mkdirs();
        }
        if (!checkFileExists(testPath)) {
            File testDataDirectory = new File(testPath);
            testDataDirectory.mkdirs();
        }
        String logFolderPath = PropertyHelper.getDataFromProperties(ConfigContext.getLogFilePath(), "property.basePath");
        String logFilePath = logFolderPath.concat(File.separator)
                .concat(PropertyHelper.getDataFromProperties(ConfigContext.getLogFilePath(), "property.fileName"));
        if (!checkFileExists(logFolderPath)) {
            File logDirectory = new File(logFolderPath);
            logDirectory.mkdirs();
        }
        if (!checkFileExists(logFilePath)) {
            File logFile = new File(logFilePath);
            try {
                logFile.createNewFile();
            }
            catch (IOException e){
                System.err.println(e.getMessage());
            }
        }
    }
}
