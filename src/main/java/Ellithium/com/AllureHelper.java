package Ellithium.com;

import Ellithium.Utilities.Colors;
import Ellithium.Utilities.CommandExecutor;
import Ellithium.Utilities.logsUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;

import static Ellithium.Utilities.PropertyHelper.getDataFromProperties;
import static Ellithium.Utilities.JarExtractor.extractFileFromJar;
import static Ellithium.properties.StartUpLoader.extractAllureFolderFromJar;

public class AllureHelper {

    private static File allureDirectory;
    private static File allureBinaryDirectory;

    public static void allureOpen() {
        String allurePropertiesFilePath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "allure";
        String openFlag = getDataFromProperties(allurePropertiesFilePath, "allure.open.afterExecution");

        if (openFlag != null && openFlag.equalsIgnoreCase("true")) {
            String allureBinaryPath = resolveAllureBinaryPath();

            if (allureBinaryPath != null) {
                String generateCommand = allureBinaryPath + "allure generate ." + File.separator + "Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-results --clean -o Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-report";
                String openCommand = allureBinaryPath + "allure open ." + File.separator + "Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-report";

                // Generate report
                CommandExecutor.executeCommand(generateCommand);

                // Open report and keep the server alive
                Process allureProcess = CommandExecutor.executeCommandNonBlocking(openCommand);

                // Keep the process alive until user interrupts (e.g., manually close)
                try {
                    System.out.println("Allure server is running. Press CTRL+C to stop the server.");
                    allureProcess.waitFor();  // This will keep the process running until terminated
                } catch (InterruptedException e) {
                    System.err.println("Allure server was interrupted.");
                } finally {
                    if (allureProcess.isAlive()) {
                        allureProcess.destroy();
                        System.out.println("Allure server process terminated.");
                    }
                }
            } else {
                System.err.println("Failed to resolve Allure binary path.");
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
                    System.err.println("Binary directory not found in the expected location.");
                    return null;
                }
                logsUtils.info(Colors.GREEN + "Found Allure binary directory: " + allureBinaryDirectory.getAbsolutePath() + Colors.RESET);
                return allureBinaryDirectory.getAbsolutePath() + File.separator;
            } else {
                System.err.println("No subdirectories found in the Allure directory.");
                return null;
            }
        } else {
            System.out.println("Allure folder not found. Extracting from JAR...");
            String version = getDataFromProperties(configFilePath, "EllithiumVersion");
            String allureJarPath = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository"
                    + File.separator + "io" + File.separator + "github" + File.separator + "Ellithium"
                    + File.separator + version + File.separator + "Ellithium-" + version + ".jar";

            File jarFile = new File(allureJarPath);
            if (!jarFile.exists()) {
                System.err.println("JAR file not found: " + allureJarPath);
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
                System.err.println("Failed to extract Allure folder from JAR: ");
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
                logsUtils.info(Colors.GREEN + "Allure binary path added to the system PATH (Windows).");
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
