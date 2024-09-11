package Ellithium.com;

import Ellithium.Utilities.Colors;
import Ellithium.Utilities.CommandExecutor;
import Ellithium.Utilities.logsUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static Ellithium.Utilities.PropertyHelper.getDataFromProperties;
import static Ellithium.Utilities.JarExtractor.extractFileFromJar;
import static Ellithium.properties.StartUpLoader.extractAllureFolderFromJar;

public class AllureHelper {

    private static File allureDirectory;
    private static File allureBinaryDirectory;
    public static void allureOpen() {
        String allurePropertiesFilePath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "allure";
        String openFlag = getDataFromProperties(allurePropertiesFilePath, "allure.open.afterExecution");
        String resultsPath = getDataFromProperties(allurePropertiesFilePath, "allure.results.directory");
        String reportPath = getDataFromProperties(allurePropertiesFilePath, "allure.report.directory");
        if (openFlag != null && openFlag.equalsIgnoreCase("true")) {
            String allureBinaryPath = resolveAllureBinaryPath();
            if (allureBinaryPath != null) {
                String generateCommand = allureBinaryPath + "allure generate ." + File.separator + resultsPath + File.separator + " --clean -o ."+File.separator+reportPath;
                String openCommand = allureBinaryPath + "allure open ."+File.separator + reportPath;
                // Generate report
                CommandExecutor.executeCommand(generateCommand);
                // Open report and keep the server alive
                Process allureProcess = CommandExecutor.executeCommandNonBlocking(openCommand);
                // Keep the process alive until user interrupts (e.g., manually close)
                try {
                    logsUtils.info(Colors.GREEN +"Allure server is running. Press CTRL+C to stop the server."+Colors.RESET);
                    allureProcess.waitFor();  // This will keep the process running until terminated
                } catch (InterruptedException e) {
                    logsUtils.info(Colors.RED +"Allure server was interrupted."+Colors.RESET);
                    logsUtils.logException(e);
                } finally {
                    if (allureProcess.isAlive()) {
                        allureProcess.destroy();
                        logsUtils.info(Colors.GREEN +"Allure server process terminated."+Colors.RESET);
                    }
                }
            } else {
                logsUtils.info(Colors.RED +"Failed to resolve Allure binary path."+Colors.RESET);
            }
        }
    }
    private static String resolveAllureBinaryPath() {
        String allurePath = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository" + File.separator + "allure-Ellithium";
        allureDirectory = new File(allurePath);
        String configFilePath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "config";
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
            String version = getDataFromProperties(configFilePath, "EllithiumVersion");
            String allureJarPath = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository"
                    + File.separator + "io" + File.separator + "github" + File.separator + "Ellithium"
                    + File.separator + version + File.separator + "Ellithium-" + version + ".jar";

            File jarFile = new File(allureJarPath);
            if (!jarFile.exists()) {
                logsUtils.info(Colors.RED +"JAR file not found: " + allureJarPath+ Colors.RESET);
                return null;
            }
            try {
                extractAllureFolderFromJar(jarFile, allureDirectory);
                String allureVersion = getDataFromProperties(configFilePath, "allureVersion");
                allureBinaryDirectory = new File(allureDirectory, "-" + allureVersion + File.separator + "bin");
                String allureLogoDirectoryPath = allureDirectory.getPath() + File.separator + "-" + allureVersion + File.separator + "plugins" + File.separator + "custom-logo-plugin" + File.separator + "static";
                File allureLogoDirectory = new File(allureLogoDirectoryPath, "custom-logo.svg");
                allureLogoDirectory.delete();
                new File(allureLogoDirectoryPath, "styles.css").delete();
                File myStyle = new File(allureLogoDirectoryPath, "styles.css");
                extractFileFromJar(jarFile, "logo/styles.css", myStyle);
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
                CommandExecutor.executeCommand(command);
                logsUtils.info(Colors.GREEN + "Allure binary path added to the system PATH (Windows)."+ Colors.RESET);
            } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_UNIX) {
                String shellConfig = System.getenv("SHELL").contains("zsh") ? "~/.zshrc" : "~/.bashrc";
                String command = "echo 'export PATH=\"$PATH:" + allureBinaryPath + "\"' >> " + shellConfig;
                CommandExecutor.executeCommand(command);
                logsUtils.info(Colors.GREEN + "Allure binary path added to " + shellConfig + " (Unix-based)." + Colors.RESET);
            } else {
                logsUtils.error(Colors.RED + "Unsupported OS." + Colors.RESET);
            }
        } else {
            logsUtils.info(Colors.GREEN + "Allure binary path already exists in the system PATH." + Colors.RESET);
        }
    }
}
