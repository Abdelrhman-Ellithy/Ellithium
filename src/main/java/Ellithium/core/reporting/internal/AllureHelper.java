package Ellithium.core.reporting.internal;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.Utilities.helpers.CommandExecutor;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.execution.Internal.Loader.StartUpLoader;
import Ellithium.core.logging.Logger;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static Ellithium.Utilities.helpers.CommandExecutor.executeCommand;
import static Ellithium.Utilities.helpers.PropertyHelper.getDataFromProperties;
import static org.apache.commons.io.FileUtils.deleteDirectory;

public class AllureHelper {
    public static void allureOpen() {
        String allurePropertiesFilePath = ConfigContext.getAllureFilePath();
        String generateReportFlag = getDataFromProperties(allurePropertiesFilePath, "allure.generate.report");
        String resultsPath = getDataFromProperties(allurePropertiesFilePath, "allure.results.directory");
        String reportPath = getDataFromProperties(allurePropertiesFilePath, "allure.report.directory");
        String lastReportPath="LastReport";
        if (generateReportFlag != null && generateReportFlag.equalsIgnoreCase("true")) {
            String allureBinaryPath = resolveAllureBinaryPath();
            File allureExecutable = new File(allureBinaryPath, "allure");
            if (!SystemUtils.IS_OS_WINDOWS) {
                if (!allureExecutable.exists()) {
                    Logger.error("Allure executable not found: " + allureExecutable.getAbsolutePath());
                    return;
                }
                try {
                    Set<PosixFilePermission> perms = EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_EXECUTE,
                            PosixFilePermission.GROUP_READ,
                            PosixFilePermission.GROUP_EXECUTE,
                            PosixFilePermission.OTHERS_READ,
                            PosixFilePermission.OTHERS_EXECUTE
                    );
                    Files.setPosixFilePermissions(allureExecutable.toPath(), perms);
                } catch (IOException e) {
                    Logger.error("Permission setting failed: " + e.getMessage());
                }
            }
            if (allureBinaryPath != null) {
                String generateCommand = String.format(
                        "\"%s\" generate --single-file --name \"Test Report\" -c -o \"%s\" \"%s\"",
                        allureExecutable,
                        new File(lastReportPath).getAbsolutePath(),
                        new File(resultsPath).getAbsolutePath()
                );
                executeCommand(generateCommand);
                File indexFile = new File(lastReportPath + File.separator + "index.html");
                File renamedFile = new File(reportPath + File.separator + "Ellithium-Test-Report-"
                        + TestDataGenerator.getTimeStamp() + ".html");
                if (indexFile.exists()) {
                    try {
                        File destinationDir = new File(reportPath);
                        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
                            Logger.error("Failed to create destination directory: " + reportPath);
                            return;
                        }
                        Files.move(indexFile.toPath(), renamedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        Logger.info("Report renamed to: " + renamedFile.getPath());
                    } catch (IOException e) {
                        Logger.error("Failed to rename report file: " + e.getMessage());
                    }
                } else {
                    Logger.error("Generated index.html not found. Allure report generation failed.");
                }
                File lastReportDir = new File(lastReportPath);
                if (lastReportDir.exists()) {
                    lastReportDir.delete();
                }
                String openFlag = getDataFromProperties(allurePropertiesFilePath, "allure.open.afterExecution");
                if (openFlag != null && openFlag.equalsIgnoreCase("true")){
                    CommandExecutor.openFile(renamedFile.getPath());
                }
            } else {
                Logger.info(Colors.RED +"Failed to resolve Allure binary path."+Colors.RESET);
            }
        }
    }
    private static String resolveAllureBinaryPath() {
        String allurePath = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository" + File.separator + "allure-Ellithium";
        File allureDirectory = new File(allurePath);
        File allureBinaryDirectory;
        if (allureDirectory.exists()) {
            Logger.info(Colors.GREEN + "Allure folder exists at: " + allurePath + Colors.RESET);
            File[] subDirs = allureDirectory.listFiles(File::isDirectory);
            if (subDirs != null && subDirs.length > 0) {
                allureBinaryDirectory = new File(subDirs[0], "bin");
                if (!allureBinaryDirectory.exists()) {
                    Logger.info(Colors.RED +"Binary directory not found in the expected location.");
                    return null;
                }
                Logger.info(Colors.GREEN + "Found Allure binary directory: " + allureBinaryDirectory.getAbsolutePath() + Colors.RESET);
                return allureBinaryDirectory.getAbsolutePath() + File.separator;
            } else {
                Logger.info(Colors.RED +"No subdirectories found in the Allure directory."+ Colors.RESET);
                return null;
            }
        } else {
            Logger.info(Colors.RED +"Allure folder not found. Extracting from JAR..."+ Colors.RESET);
            File jarFile = StartUpLoader.findJarFile();
            if (!jarFile.exists()) {
                Logger.info(Colors.RED +"Ellithium JAR file not found"+ Colors.RESET);
                return null;
            }
            try {
                extractAllureFolderFromJar(jarFile, allureDirectory);
                String allureVersion = ConfigContext.getAllureVersion();
                allureBinaryDirectory = new File(allureDirectory, "-" + allureVersion + File.separator + "bin");
            } catch (IOException e) {
                Logger.info(Colors.RED +"Failed to extract Allure folder from JAR: "+ Colors.RESET);
                Logger.logException(e);
                return null;
            }
        }
        return allureBinaryDirectory.getAbsolutePath() + File.separator;
    }

    public static void deleteAllureResultsDir(){
        String allurePropertiesFilePath = ConfigContext.getAllureFilePath();
        String resultsPath = getDataFromProperties(allurePropertiesFilePath, "allure.results.directory");
        File allureResultsFolder = new File(resultsPath);
        if (allureResultsFolder.exists()) {
            try {
                File[] files = allureResultsFolder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            deleteDirectory(file);
                        } else {
                            Files.deleteIfExists(file.toPath());
                        }
                    }
                }
            } catch (IOException e) {
                Logger.logException(e);
            }
        }
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
}