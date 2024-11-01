package Ellithium.core.reporting.internal;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.execution.Internal.Loader.StartUpLoader;
import Ellithium.core.logging.logsUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static Ellithium.Utilities.helpers.CommandExecutor.executeCommand;
import static Ellithium.Utilities.helpers.PropertyHelper.getDataFromProperties;
import static Ellithium.core.execution.Internal.Loader.StartUpLoader.extractAllureFolderFromJar;
import static org.apache.commons.io.FileUtils.deleteDirectory;

public class AllureHelper {
    private static File allureDirectory;
    private static File allureBinaryDirectory;
    public static void allureOpen() {
        String allurePropertiesFilePath = ConfigContext.getAllureFilePath();
        String generateReportFlag = getDataFromProperties(allurePropertiesFilePath, "allure.generate.report");
        String resultsPath = getDataFromProperties(allurePropertiesFilePath, "allure.results.directory");
        String reportPath = getDataFromProperties(allurePropertiesFilePath, "allure.report.directory");
        String lastReportPath="LastReport";
        if (generateReportFlag != null && generateReportFlag.equalsIgnoreCase("true")) {
            String allureBinaryPath = resolveAllureBinaryPath();
            if (allureBinaryPath != null) {
                String generateCommand = allureBinaryPath + "allure generate --single-file --name \"Test Report\" -o ."+File.separator  +lastReportPath + File.separator +" ."+ File.separator + resultsPath+File.separator+"";
                executeCommand(generateCommand);
                File indexFile = new File(lastReportPath.concat(File.separator + "index.html"));
                File renamedFile = new File(reportPath.concat(File.separator + "Ellithium-Test-Report-" + TestDataGenerator.getTimeStamp() + ".html"));
                String fileName=renamedFile.getPath();
                if (indexFile.exists()) {
                    indexFile.renameTo(renamedFile);
                }
                File lastReportDir = new File(lastReportPath);
                if (lastReportDir.exists()) {
                    lastReportDir.delete();
                }
                String openFlag = getDataFromProperties(allurePropertiesFilePath, "allure.open.afterExecution");
                if (openFlag != null && openFlag.equalsIgnoreCase("true")){
                    String openCommand;
                    if (SystemUtils.IS_OS_WINDOWS) {
                        openCommand = "start ".concat(fileName);
                    } else if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_LINUX) {
                        openCommand = SystemUtils.IS_OS_MAC ? "open ".concat(fileName) : "xdg-open ".concat(fileName);
                    } else {
                        openCommand=null;
                        logsUtils.error("Unsupported operating system.");
                    }
                    executeCommand(openCommand);
                }
            } else {
                logsUtils.info(Colors.RED +"Failed to resolve Allure binary path."+Colors.RESET);
            }
        }
    }
    private static String resolveAllureBinaryPath() {
        String allurePath = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository" + File.separator + "allure-Ellithium";
        allureDirectory = new File(allurePath);
        String configFilePath =ConfigContext.getConfigFilePath();
        if (allureDirectory.exists()) {
            logsUtils.info(Colors.GREEN + "Allure folder exists at: " + allurePath + Colors.RESET);
            File[] subDirs = allureDirectory.listFiles(File::isDirectory);
            if (subDirs != null && subDirs.length > 0) {
                allureBinaryDirectory = new File(subDirs[0], "bin");
                if (!allureBinaryDirectory.exists()) {
                    logsUtils.info(Colors.RED +"Binary directory not found in the expected location.");
                    return null;
                }
                logsUtils.info(Colors.GREEN + "Found Allure binary directory: " + allureBinaryDirectory.getAbsolutePath() + Colors.RESET);
                return allureBinaryDirectory.getAbsolutePath() + File.separator;
            } else {
                logsUtils.info(Colors.RED +"No subdirectories found in the Allure directory."+ Colors.RESET);
                return null;
            }
        } else {
            logsUtils.info(Colors.RED +"Allure folder not found. Extracting from JAR..."+ Colors.RESET);
            File jarFile = StartUpLoader.findJarFile();
            if (!jarFile.exists()) {
                logsUtils.info(Colors.RED +"Ellithium JAR file not found"+ Colors.RESET);
                return null;
            }
            try {
                extractAllureFolderFromJar(jarFile, allureDirectory);
                String allureVersion = getDataFromProperties(configFilePath, "allureVersion");
                allureBinaryDirectory = new File(allureDirectory, "-" + allureVersion + File.separator + "bin");
                addAllureToSystemPath(allureBinaryDirectory);
            } catch (IOException e) {
                logsUtils.info(Colors.RED +"Failed to extract Allure folder from JAR: "+ Colors.RESET);
                logsUtils.logException(e);
                return null;
            }
        }
        return allureBinaryDirectory != null ? allureBinaryDirectory.getAbsolutePath() + File.separator : null;
    }

    private static void addAllureToSystemPath(File allureDirectory) {
        String path = System.getenv("PATH");
        String allureBinaryPath = allureDirectory.getAbsolutePath();
        if (!path.contains(allureBinaryPath)) {
            System.setProperty("PATH", path + File.pathSeparator + allureBinaryPath);
            logsUtils.info(Colors.GREEN + "Added Allure to system PATH for current session: " + allureBinaryPath + Colors.RESET);

            if (SystemUtils.IS_OS_WINDOWS) {
                String command = "setx PATH \"%PATH%;" + allureBinaryPath + "\"";
                executeCommand(command);
                logsUtils.info(Colors.GREEN + "Allure binary path added to the system PATH (Windows)."+ Colors.RESET);
            } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_UNIX) {
                String shell = System.getenv("SHELL");
                String shellConfig;
                if (shell != null && shell.contains("zsh")) {
                    shellConfig = "~/.zshrc";
                } else {
                    shellConfig = "~/.bashrc"; // Fallback to bashrc if SHELL is null or not zsh
                }
                String command = "echo 'export PATH=\"$PATH:" + allureBinaryPath + "\"' >> " + shellConfig;
                executeCommand(command);
                logsUtils.info(Colors.GREEN + "Allure binary path added to " + shellConfig + " (Unix-based)." + Colors.RESET);
            } else {
                logsUtils.error(Colors.RED + "Unsupported OS." + Colors.RESET);
            }
        } else {
            logsUtils.info(Colors.GREEN + "Allure binary path already exists in the system PATH." + Colors.RESET);
        }
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
                logsUtils.logException(e);
            }
        }
    }
}
