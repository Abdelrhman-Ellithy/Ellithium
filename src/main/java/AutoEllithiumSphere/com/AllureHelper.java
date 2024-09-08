package AutoEllithiumSphere.com;

import AutoEllithiumSphere.Utilities.CommandExecutor;
import AutoEllithiumSphere.Utilities.PropertyHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AllureHelper {
    private static File allureDirectory;

    public static void allureOpen() {
        // Fetch the properties file path
        String propertiesFilePath = System.getProperty("user.dir") + File.separator + "src" + File.separator
                + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default"
                + File.separator + "allure";

        // Check if we should open the Allure report after execution
        String openFlag = PropertyHelper.getDataFromProperties(propertiesFilePath, "allure.open.afterExecution");
        if (openFlag.equalsIgnoreCase("true")) {
            // Check or resolve Allure binary path
            String allureBinaryPath = resolveAllureBinaryPath();

            if (allureBinaryPath != null) {
                // Define the commands to generate and open the report using the resolved binary path
                String generateCommand = allureBinaryPath + "allure generate .Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-results --clean -o Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-report";
                String openCommand = allureBinaryPath + "allure open .\\Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-report";

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

        // Check if Allure folder exists in .m2/repository
        if (allureDirectory.exists()) {
            System.out.println("Allure folder exists at: " + allurePath);
        } else {
            // Extract Allure from the JAR to .m2/repository/allure
            System.out.println("Allure folder not found. Extracting from JAR...");
            String version = System.getProperty("AutoEllithiumSphereVersion");
            String allureJarPath = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository"
                    + File.separator + "org" + File.separator + "autoellithiumsphere" + File.separator + "autoellithiumsphere"
                    + File.separator + version + File.separator + "autoellithiumsphere-" + version + ".jar";

            File jarFile = new File(allureJarPath);
            if (!jarFile.exists()) {
                System.err.println("JAR file not found: " + allureJarPath);
                return null;
            }

            try {
                extractAllureFolderFromJar(jarFile, allureDirectory);
            } catch (IOException e) {
                System.err.println("Failed to extract Allure folder from JAR: " + e.getMessage());
                return null;
            }
        }

        // Add the Allure binary folder to the system PATH
        addAllureToSystemPath(allureDirectory);

        // Return the path to the Allure binary
        return allureDirectory.getAbsolutePath() + File.separator;
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
            System.setProperty("PATH", path + File.pathSeparator + allureBinaryPath);
            System.out.println("Added Allure to system PATH: " + allureBinaryPath);
        }
    }
}
