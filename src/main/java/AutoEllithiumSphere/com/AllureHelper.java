package AutoEllithiumSphere.com;

import AutoEllithiumSphere.Utilities.CommandExecutor;
import AutoEllithiumSphere.Utilities.PropertyHelper;

import java.io.File;

public class AllureHelper {

    public static void allureOpen() {
        // Fetch the allure open flag from the properties file
        String openFlag = PropertyHelper.getDataFromProperties(System.getProperty("user.dir") + File.separator + "src" + File.separator
                        + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default",
                "allure", "allure.open.afterExecution");

        // Check if the flag is set to true
        if (openFlag.equalsIgnoreCase("true")) {
            // Define the commands to generate and open the Allure report
            String commands = String.join(" && ",
                    "cd Test-Output/Reports/Allure",                                // Change directory to Allure report location
                    "allure generate allure-results --clean -o allure-report",      // Generate the report
                    "start allure-report"                                           // Open the report in the default browser (for Windows)
            );

            // Execute the commands using CommandExecutor
            CommandExecutor.executeCommand(commands);
        }
    }
}