package Ellithium.properties;

import Ellithium.Utilities.logsUtils;
import Ellithium.com.Colors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class StartUpLoader {
    private static final String basePath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator;
    private static final String testPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "TestData";
    private static final String ScreenShotPath = System.getProperty("user.dir") + File.separator + "Test-Output" + File.separator + "ScreenShots" + File.separator + "Failed" ;
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

        // Check if TestData directory exists or create it
        File testDataDirectory = new File(testPath);
        if (testDataDirectory.exists()) {
            logsUtils.info(Colors.GREEN + "TestData Folder exists");
        } else {
            testDataDirectory.mkdirs();
            logsUtils.info(Colors.GREEN + "TestData Folder created");
        }

        // Check if ScreenShots directory exists or create it
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
                        File jarFile = findJarFile();
                        if (jarFile != null) {
                            extractFileFromJar(jarFile, "properties/default/allure.properties", new File(allurePath));
                        } else {
                            System.err.println("JAR file not found.");
                        }
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
                        File jarFile = findJarFile();
                        if (jarFile != null) {
                            extractFileFromJar(jarFile, "properties/default/config.properties", new File(configPath));
                            logsUtils.info(Colors.GREEN + "Config properties initialized.");
                        } else {
                            System.err.println("JAR file not found.");
                        }
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
                        File jarFile = findJarFile();
                        if (jarFile != null) {
                            extractFileFromJar(jarFile, "properties/default/log4j2.properties", new File(logPath));
                            logsUtils.info(Colors.GREEN + "Log4j2 properties initialized.");
                        } else {
                            System.err.println("JAR file not found.");
                        }
                    } catch (IOException e) {
                        System.err.println("Error initializing log4j2 properties: " + e.getMessage());
                    }
                } else {
                    logsUtils.info(Colors.GREEN + "Log4j2 properties already exist.");
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

    private static File findJarFile() {
        String repoPath = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository"
                + File.separator + "io" + File.separator + "github" + File.separator + "Ellithium";
        File repoDir = new File(repoPath);
        File[] versionDirs = repoDir.listFiles(File::isDirectory);
        if (versionDirs != null && versionDirs.length > 0) {
            File versionDir = versionDirs[0]; // Select the first version directory
            File[] jarFiles = versionDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles != null && jarFiles.length > 0) {
                return jarFiles[0]; // Return the first JAR file found
            }
        }
        return null;
    }
    public static void extractFolderFromJar(File jarFile, File targetDirectory) throws IOException {
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
    }

    public static void extractFileFromJar(File jarFile, String filePathInJar, File outputFile) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(filePathInJar);
            if (entry != null) {
                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }
                Files.copy(jar.getInputStream(entry), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Extracted file from JAR: " + filePathInJar);
            } else {
                System.err.println("File not found in JAR: " + filePathInJar);
            }
        }
    }

}
