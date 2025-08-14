package Ellithium.config.Internal;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.config.managment.ConfigContext;
import Ellithium.core.logging.Logger;
import io.restassured.http.ContentType;

import static Ellithium.core.reporting.internal.Colors.*;
import static io.restassured.RestAssured.given;

public class VersionChecker {
    public static String getLatestVersion(){
        var response= given().
                baseUri("https://api.github.com").and().basePath("repos/Abdelrhman-Ellithy/Ellithium/releases/")
                    .accept(ContentType.JSON)
                .when().get("latest");
        return response.jsonPath().getString("name");
    }
    public static void solveVersion(){
        String path= ConfigContext.getCheckerFilePath();
        String Date= TestDataGenerator.getDayDateStamp();
        String currentDate=JsonHelper.getJsonKeyValue(path,"LastRunDate");
        if(currentDate==null||!(currentDate.equalsIgnoreCase(Date))) {
            JsonHelper.setJsonKeyValue(path,"LastRunDate",Date);
            try {
                String latestVersion=getLatestVersion();
                if (latestVersion==null)latestVersion=getLatestVersion();
                String currentVersion= ConfigContext.getEllithuiumVersion();
                if(!latestVersion.toLowerCase().contains(currentVersion.toLowerCase())){
                    Logger.info(CYAN + "------------[VERSION CHECKER]------------------" + RESET);
                    Logger.info(BLUE + "-----------------------------------------------" + RESET);
                    Logger.info(RED+   "You Are Using Old Version of Ellithium: "+currentVersion+ RESET);
                    Logger.info(BLUE+  "You Need To update to the latest Version: "+latestVersion+ RESET);
                    Logger.info(BLUE + "-----------------------------------------------" + RESET);
                    Logger.info(CYAN + "------------[VERSION CHECKER]------------------" + RESET);
                }
            }catch (Exception e){
                Logger.info(CYAN + "------------[VERSION CHECKER]----------------------------" + RESET);
                Logger.info(BLUE + "---------------------------------------------------------" + RESET);
                Logger.info(RED+   "Connection Problem Happened Version will be Checked Later"+ RESET);
                Logger.info(BLUE + "---------------------------------------------------------" + RESET);
                Logger.info(CYAN + "------------[VERSION CHECKER]----------------------------" + RESET);
            }
        }
    }
}