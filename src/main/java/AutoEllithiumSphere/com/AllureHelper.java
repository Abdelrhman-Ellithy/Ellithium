package AutoEllithiumSphere.com;

import AutoEllithiumSphere.Utilities.CommandExecutor;
import AutoEllithiumSphere.Utilities.logsUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.xmlbeans.SystemProperties;
import org.checkerframework.checker.units.qual.C;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static AutoEllithiumSphere.Utilities.PropertyHelper.getDataFromProperties;

public class AllureHelper {

    private static File allureDirectory;
    private static File allureBinaryDirectory;

    public static void allureOpen() {
        // Fetch the properties file path
        String allurePropertiesFilePath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "allure";
        // Check if we should open the Allure report after execution
        String openFlag = getDataFromProperties(allurePropertiesFilePath, "allure.open.afterExecution");
        if (openFlag != null && openFlag.equalsIgnoreCase("true")) {
            // Check or resolve Allure binary path
            String allureBinaryPath = resolveAllureBinaryPath();

            if (allureBinaryPath != null) {
                // Define the commands to generate and open the report using the resolved binary path
                String generateCommand = allureBinaryPath + "allure generate ."+ File.separator + "Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-results --clean -o Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-report";
                String openCommand = allureBinaryPath + "allure open ."+ File.separator + "Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-report";

                // Execute the commands using the CommandExecutor class
                CommandExecutor.executeCommand(generateCommand);
                CommandExecutor.executeCommand(openCommand);
            } else {
                System.err.println("Failed to resolve Allure binary path.");
            }
        }
    }

    /**
     * Resolve the Allure binary path dynamically.
     * If the folder doesn't exist in .m2, extract it from the JAR and add it to the system PATH.
     *
     * @return The resolved path to the Allure binary.
     */
    private static String resolveAllureBinaryPath() {
        String allurePath = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository" + File.separator + "allure";
        allureDirectory = new File(allurePath);
        String configFilePath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default" + File.separator + "config";
        // Check if Allure folder exists in .m2/repository
        if (allureDirectory.exists()) {
            logsUtils.info(Colors.GREEN+"Allure folder exists at: " + allurePath+ Colors.RESET);
            // Identify the bin folder within the allure directory

            File[] subDirs = allureDirectory.listFiles(File::isDirectory);
            if (subDirs != null && subDirs.length > 0) {
                // Assuming the first subdirectory contains the bin folder
                allureBinaryDirectory = new File(subDirs[0], "bin");
                if (!allureBinaryDirectory.exists()) {
                    System.err.println("Binary directory not found in the expected location.");
                    return null;
                }
                logsUtils.info(Colors.GREEN+"Found Allure binary directory: " + allureBinaryDirectory.getAbsolutePath()+ Colors.RESET);
                return allureBinaryDirectory.getAbsolutePath() + File.separator;
            } else {
                System.err.println("No subdirectories found in the Allure directory.");
                return null;
            }
        } else {
            // Extract Allure from the JAR to .m2/repository/allure
            System.out.println("Allure folder not found. Extracting from JAR...");
            String version = getDataFromProperties(configFilePath, "AutoEllithiumSphereVersion");
            String allureJarPath = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository"
                    + File.separator + "io" + File.separator + "github" + File.separator + "autoellithiumsphere"
                    + File.separator + version + File.separator + "autoellithiumsphere-" + version + ".jar";

            File jarFile = new File(allureJarPath);
            if (!jarFile.exists()) {
                System.err.println("JAR file not found: " + allureJarPath);
                return null;
            }

            try {
                extractAllureFolderFromJar(jarFile, allureDirectory);
                String allureVersion = getDataFromProperties(configFilePath, "allureVersion");
                // Define the allure binary directory based on the extracted version
                allureBinaryDirectory = new File(allureDirectory, "-" + allureVersion + File.separator + "bin");
                addAllureToSystemPath(allureBinaryDirectory);
            } catch (IOException e) {
                System.err.println("Failed to extract Allure folder from JAR: " + e.getMessage());
                return null;
            }
        }
        // Return the path to the Allure binary
        return allureBinaryDirectory != null ? allureBinaryDirectory.getAbsolutePath() + File.separator : null;
    }

    /**
     * Extract the Allure folder from the provided JAR file to the specified directory.
     *
     * @param jarFile The JAR file containing the Allure folder.
     * @param targetDirectory The directory where the Allure folder will be extracted.
     * @throws IOException if extraction fails.
     */
    private static void extractAllureFolderFromJar(File jarFile, File targetDirectory) throws IOException {
        if (!targetDirectory.exists()) {
            Files.createDirectory(targetDirectory.toPath());
        }

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith("allure")) {  // Only extract allure folder
                    File targetFile = new File(targetDirectory, entry.getName().substring("allure".length()));
                    // Create directories if it's a directory
                    if (entry.isDirectory()) {
                        targetFile.mkdirs();
                    } else {
                        // Copy the file from the JAR to the target directory
                        Files.copy(jar.getInputStream(entry), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        if (entry.getName().endsWith("allure") || entry.getName().endsWith("allure.bat")) {
                            targetFile.setExecutable(true);  // Ensure the binary is executable
                        }
                    }
                }
            }
        }
    }

    /**
     * Add the Allure binary folder to the system PATH.
     *
     * @param allureDirectory The directory where the Allure binary is located.
     */
    private static void addAllureToSystemPath(File allureDirectory) {
        String path = System.getenv("PATH");
        String allureBinaryPath = allureDirectory.getAbsolutePath();
        if (!path.contains(allureBinaryPath)) {
            // Update PATH locally for the current JVM session
            System.setProperty("PATH", path + File.pathSeparator + allureBinaryPath);
            logsUtils.info(Colors.GREEN+"Added Allure to system PATH for current session: " + allureBinaryPath+ Colors.RESET);

            // Check if the OS is Windows or Unix-based
            if (SystemUtils.IS_OS_WINDOWS) {
                // Windows-specific: use setx to persist the PATH update
                String command = "setx PATH \"%PATH%;" + allureBinaryPath + "\"";
                CommandExecutor.executeCommand(command);
                logsUtils.info(Colors.GREEN+"Allure binary path added to the system PATH (Windows).");

            } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_UNIX) {
                // Unix-based systems: append to ~/.bashrc or ~/.zshrc
                String shellConfig = System.getenv("SHELL").contains("zsh") ? "~/.zshrc" : "~/.bashrc";
                String command = "echo 'export PATH=\"$PATH:" + allureBinaryPath + "\"' >> " + shellConfig;
                CommandExecutor.executeCommand(command);
                logsUtils.info(Colors.GREEN+"Allure binary path added to " + shellConfig + " (Unix-based)."+ Colors.RESET);

            } else {
                System.out.println("Unsupported OS.");
            }
        } else {

            logsUtils.info(Colors.GREEN+"Allure binary path already exists in the system PATH."+ Colors.RESET);
        }
    }
}