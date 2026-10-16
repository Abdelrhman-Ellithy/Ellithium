package Ellithium.config.Internal;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.Utilities.helpers.JsonHelper;
import Ellithium.config.management.ConfigContext;
import Ellithium.core.logging.Logger;
import io.restassured.http.ContentType;

import static Ellithium.core.reporting.internal.Colors.*;
import static io.restassured.RestAssured.given;

public class VersionChecker {
    private static String getLatestVersion(){
        var response= given().
                baseUri("https://api.github.com").and().basePath("repos/Abdelrhman-Ellithy/Ellithium/releases/")
                    .accept(ContentType.JSON)
                .when().get("latest");
        return response.jsonPath().getString("name");
    }
    public static boolean isOlderVersion(String currentVersion, String latestVersion) {
        if (currentVersion == null || latestVersion == null) return false;
        String cleanCurrent = currentVersion.replaceAll("[^0-9.]", "").replaceAll("^\\.+|\\.+$", "");
        String cleanLatest = latestVersion.replaceAll("[^0-9.]", "").replaceAll("^\\.+|\\.+$", "");
        if (cleanCurrent.isEmpty() || cleanLatest.isEmpty()) {
            return !latestVersion.equalsIgnoreCase(currentVersion);
        }
        String[] parts1 = cleanCurrent.split("\\.");
        String[] parts2 = cleanLatest.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int v2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (v1 != v2) {
                return v1 < v2;
            }
        }
        return false;
    }

    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void solveVersion(){
        String path= ConfigContext.getCheckerFilePath();
        String Date= TestDataGenerator.getDayDateStamp();
        String currentDate=JsonHelper.getJsonKeyValue(path,"LastRunDate");
        if(currentDate==null||!(currentDate.equalsIgnoreCase(Date))) {
            JsonHelper.setJsonKeyValue(path,"LastRunDate",Date);
            try {
                String latestVersion=getLatestVersion();
                if (latestVersion == null) latestVersion = getLatestVersion();
                if (latestVersion == null) throw new IllegalStateException("Version check failed after retry");
                String currentVersion= ConfigContext.getEllithiumVersion();
                if (isOlderVersion(currentVersion, latestVersion)){
                    Logger.info(CYAN + "-------------------[VERSION CHECKER]---------------------"  + RESET);
                    Logger.info(BLUE + "---------------------------------------------------------" + RESET);
                    Logger.info(RED+   "You Are Using Old Version of Ellithium: "+currentVersion+"-----------"+ RESET);
                    Logger.info(BLUE + "---------------------------------------------------------" + RESET);
                    Logger.info(BLUE+  "You Need To update to the latest Version: "+latestVersion+"-----------"+ RESET);
                    Logger.info(BLUE + "---------------------------------------------------------" + RESET);
                    Logger.info(CYAN + "-------------------[VERSION CHECKER]---------------------"  + RESET);
                }
            }catch (Exception e){
                    Logger.info(CYAN + "-------------------[VERSION CHECKER]---------------------" + RESET);
                    Logger.info(BLUE + "---------------------------------------------------------" + RESET);
                    Logger.info(RED+   "Connection Problem Happened Version will be Checked Later" + RESET);
                    Logger.info(BLUE + "---------------------------------------------------------" + RESET);
                    Logger.info(CYAN + "-------------------[VERSION CHECKER]---------------------"  + RESET);
            }
        }
    }
}