package AutoEllithiumSphere.com;

import AutoEllithiumSphere.Utilities.CommandExecutor;
import AutoEllithiumSphere.Utilities.PropertyHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.Enumeration;

public class AllureHelper {
    private static File tempDir ;

    public static void allureOpen() {
        // Fetch the properties file path
        String propertiesFilePath = System.getProperty("user.dir") + File.separator + "src" + File.separator
                + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default"
                + File.separator + "allure";

        // Check if we should open the Allure report after execution
        String openFlag = PropertyHelper.getDataFromProperties(propertiesFilePath, "allure.open.afterExecution");
        if (openFlag.equalsIgnoreCase("true")) {

            // Dynamically resolve the Allure binary path
            String allureBinaryPath = System.getProperty("user.dir") + File.separator +"src"+ File.separator +"main"+ File.separator +"resources"+
                    File.separator + File.separator +"allure-2.30.0"+ File.separator +"bin";

            if (allureBinaryPath != null) {
                // Define the commands to generate and open the report using the resolved binary path
                String generateCommand = allureBinaryPath + "allure generate ."+File.separator+"Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-results --clean -o Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-report";
                String openCommand = allureBinaryPath + "allure open ."+File.separator+"Test-Output" + File.separator + "Reports" + File.separator + "Allure" + File.separator + "allure-report";

                // Execute the commands using the CommandExecutor class
                CommandExecutor.executeCommand(generateCommand);
                CommandExecutor.executeCommand(openCommand);
            } else {
                System.err.println("Failed to resolve Allure binary path.");
            }
        }
    }
}
