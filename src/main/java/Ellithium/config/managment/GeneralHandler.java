package Ellithium.config.managment;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.logsUtils;
import Ellithium.core.reporting.EmailReporter;
import Ellithium.core.reporting.Reporter;
import com.google.common.io.Files;
import io.qameta.allure.Allure;
import io.qameta.allure.listener.TestLifecycleListener;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import io.restassured.RestAssured;

public class GeneralHandler implements TestLifecycleListener {
    private static Boolean BDDMode, flagReaded=false;
    public static File testFailed( String browserName, String testName)  {
        try {
            TakesScreenshot camera =((TakesScreenshot) DriverFactory.getCurrentDriver());
            File screenshot = camera.getScreenshotAs(OutputType.FILE);
            String name = browserName.toUpperCase() + "-" + testName + "-" + TestDataGenerator.getTimeStamp();
            File screenShotFile = new File("Test-Output/ScreenShots/Failed/" + name + ".png");
            Files.move(screenshot, screenShotFile);
            return screenShotFile;
        } catch (IOException e) {
            logsUtils.logException(e);
            return null;
        }
    }
    public static void attachScreenshotToReport(File screenshot,String name, String browserName, String testName){
        try (FileInputStream fis = new FileInputStream(screenshot)) {
            Allure.description(browserName.toUpperCase() + "-" + testName + " FAILED");
            Allure.addAttachment(name, "image/png", fis, ".png");
        }catch (IOException e) {
            logsUtils.logException(e);
        }
    }
    public static void AttachLogs(){
        String logFilePath = PropertyHelper.getDataFromProperties(
                ConfigContext.getLogFilePath(),
                "property.basePath"
        );
        logFilePath = logFilePath.concat(File.separator).concat(
                PropertyHelper.getDataFromProperties(
                        ConfigContext.getLogFilePath(),
                        "property.fileName"
                )
        );
        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            Reporter.log("Log file not found at: ",LogLevel.ERROR, logFilePath);
            return;
        }
        try (FileInputStream fis = new FileInputStream(logFile)) {
            Allure.addAttachment("Execution Log File", "text/plain", fis, ".log");
            logsUtils.info("Log file successfully attached to the Allure report.");
        } catch (IOException e) {
            logsUtils.error("Failed to attach log file: ");
        }
    }
    public static boolean getBDDMode(){
        if(flagReaded.equals(false)){
            String mode=PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(),"runMode");
            BDDMode=mode.equalsIgnoreCase("BDD");
            flagReaded=true;
        }
        return BDDMode;
    }
    public static String getLatestVersion(){
        return RestAssured.given().
                baseUri("https://api.github.com/").and().basePath("repos/Abdelrhman-Ellithy/Ellithium/releases/")
                .when().get("latest")
                .thenReturn().body().jsonPath().getString("name");
    }
    public static void solveVersion(){
        String latestVersion=getLatestVersion();
        String currentVersion=PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(),"EllithiumVersion");
        if(!latestVersion.toLowerCase().contains(currentVersion.toLowerCase())){
            Reporter.log("You Are Using Old Version of Ellithium Version: "+currentVersion,
                            LogLevel.INFO_RED,
                    " You Need To update to the latest Version: "+latestVersion);
        }
    }
    public static void sendReportAfterExecution(){
        String openFlag=PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(),"sendEmailAfterExecution");
        if(openFlag.equalsIgnoreCase("true")){
            String username=PropertyHelper.getDataFromProperties(ConfigContext.getEmailFilePath(),"email.username");
            String password=PropertyHelper.getDataFromProperties(ConfigContext.getEmailFilePath(),"email.password");
            String host=PropertyHelper.getDataFromProperties(ConfigContext.getEmailFilePath(),"smtp.host");
            String port=PropertyHelper.getDataFromProperties(ConfigContext.getEmailFilePath(),"smtp.port");
            String subject=PropertyHelper.getDataFromProperties(ConfigContext.getEmailFilePath(),"email.subject");
            String recipientEmail=PropertyHelper.getDataFromProperties(ConfigContext.getEmailFilePath(),"recipient.email");
            EmailReporter emailReporter=new EmailReporter(host,port,username,password,recipientEmail,subject,ConfigContext.getReportPath());
            Reporter.log("Report Path", LogLevel.INFO_BLUE,ConfigContext.getReportPath());
            emailReporter.sendReport();
        }
    }
}
