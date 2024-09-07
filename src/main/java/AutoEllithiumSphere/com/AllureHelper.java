package AutoEllithiumSphere.com;

import AutoEllithiumSphere.Utilities.CommandExecutor;
import AutoEllithiumSphere.Utilities.PropertyHelper;

import java.io.File;

public class AllureHelper {

    public static void allureOpen(){
        // Properly construct the path using File.separator
        String propertiesFilePath = System.getProperty("user.dir") + File.separator + "src" + File.separator
                + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default"
                + File.separator + "allure";

        // Fetch the flag for allure report opening
        String openFlag = PropertyHelper.getDataFromProperties(propertiesFilePath, "allure.open.afterExecution");

        // Check if the flag is true, then execute the report generation command
        if(openFlag.equalsIgnoreCase("true")){
            // Define commands for generating and opening the Allure report
            String commands = String.join(
                    " && ",
                    "allure generate Test-Output/Reports/Allure/allure-results --clean -o Test-Output/Reports/Allure/allure-report"
                    ,"allure open Test-Output/Reports/Allure/allure-report"
            );

            // Execute the commands using CommandExecutor
            CommandExecutor.executeCommand(commands);
        }
    }
}
