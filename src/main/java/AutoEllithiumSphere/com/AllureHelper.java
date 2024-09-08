package AutoEllithiumSphere.com;

import AutoEllithiumSphere.Utilities.CommandExecutor;
import AutoEllithiumSphere.Utilities.PropertyHelper;

import java.io.File;

public class AllureHelper {

    public static void allureOpen(){

        String propertiesFilePath = System.getProperty("user.dir")+ File.separator + "src" + File.separator
                + "main" + File.separator + "resources" + File.separator + "properties" + File.separator + "default"
                + File.separator + "allure";

        String openFlag = PropertyHelper.getDataFromProperties(propertiesFilePath, "allure.open.afterExecution");
        String allureBinary =".allure" + File.separator +"allure-2.30.0" +File.separator+ "bin"
                + File.separator+"allure ";
        if(openFlag.equalsIgnoreCase("true")){

            String commands = String.join(
                    " && ",
                    allureBinary+" generate Test-Output/Reports/Allure/allure-results --clean -o Test-Output/Reports/Allure/allure-report"
                    ,allureBinary+"open Test-Output/Reports/Allure/allure-report"
            );
            CommandExecutor.executeCommand(commands);
        }
    }
}
