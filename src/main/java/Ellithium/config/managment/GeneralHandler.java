package Ellithium.config.managment;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.Utilities.interactions.WaitManager;
import Ellithium.core.API.APIFilterHelper;
import Ellithium.config.Internal.VersionChecker;
import Ellithium.core.driver.*;
import Ellithium.core.execution.Analyzer.RetryAnalyzer;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.Reporter;
import com.google.common.io.Files;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.qameta.allure.model.Parameter;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GeneralHandler {
    /**
     * Internally used with The Ellithium Not for General Use.
     * @param browserName Slider element locator
     * @param testName X offset to move
     * @return The saved screenshot file
     */
    public static File testFailed( String browserName, String testName)  {
        try {
            TakesScreenshot camera =((TakesScreenshot) DriverFactory.getCurrentDriver());
            assert camera != null;
            File screenshot = camera.getScreenshotAs(OutputType.FILE);
            String name = browserName.toUpperCase() + "-" + testName + "-" + TestDataGenerator.getTimeStamp();
            File screenShotFile = new File("Test-Output"+ File.separator +"ScreenShots"+ File.separator +"Failed"+ File.separator + name + ".png");
            Files.move(screenshot, screenShotFile);
            return screenShotFile;
        } catch (IOException e) {
            Logger.logException(e);
            return null;
        }
    }
    private static File getLogFile(){
        String basePath = PropertyHelper.getDataFromProperties(
                ConfigContext.getLogFilePath(),
                "property.basePath"
        );
        String logFilePath = basePath.concat(File.separator).concat(
                Objects.requireNonNull(PropertyHelper.getDataFromProperties(
                        ConfigContext.getLogFilePath(),
                        "property.TestCaseLogFile"
                ))
        );
        return new File(logFilePath);
    }
    private static void AttachLogs(){
        attachFile(getLogFile());
    }
    public static void clearTestLogFile(){
        try (FileOutputStream fos = new FileOutputStream(getLogFile(), false)) {
        } catch (IOException e) {
        }
    }
    private static void attachFile(File file){
        if (!file.exists()) {
            Reporter.log("Log file not found at: ",LogLevel.ERROR, file.getPath());
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            Allure.addAttachment("Execution Log File", "text/plain", fis, ".log");
            Logger.info("Log file successfully attached to the Allure report.");
        } catch (IOException e) {
            Logger.error("Failed to attach log file: ");
        }
    }

    @Step("Attachments")
    public static void addAttachments(){
        GeneralHandler.AttachLogs();
    }
    public static void StartRoutine(){
        VersionChecker.solveVersion();
        APIFilterHelper.applyFilter();
        WaitManager.initializeTimeoutAndPolling();
        RetryAnalyzer.initRetryCount();
    }
    public static List<Parameter> getParameters(){
        List<io.qameta.allure.model.Parameter>parameters=new ArrayList<>();
        DriverType type=ConfigContext.getDriverType();
        if(type!=null){
            parameters.add(new io.qameta.allure.model.Parameter().setName("DriverType").setValue(ConfigContext.getValue(type)));
            if(type instanceof LocalDriverType || type instanceof RemoteDriverType){
                parameters.add(new io.qameta.allure.model.Parameter().setName("HeadlessMode").setValue(ConfigContext.getValue(ConfigContext.getHeadlessMode())));
                parameters.add(new io.qameta.allure.model.Parameter().setName("PageLoadStrategyMode").setValue(ConfigContext.getValue(ConfigContext.getPageLoadStrategy())));
                parameters.add(new io.qameta.allure.model.Parameter().setName("PrivateMode").setValue(ConfigContext.getValue(ConfigContext.getPrivateMode())));
                parameters.add(new io.qameta.allure.model.Parameter().setName("SandboxMode").setValue(ConfigContext.getValue(ConfigContext.getSandboxMode())));
                parameters.add(new io.qameta.allure.model.Parameter().setName("WebSecurityMode").setValue(ConfigContext.getValue(ConfigContext.getWebSecurityMode())));
            }
            if(type instanceof RemoteDriverType ||type instanceof MobileDriverType){
                parameters.add(new io.qameta.allure.model.Parameter().setName("Remote Address").setValue(ConfigContext.getRemoteAddress().toString()));
                parameters.add(new io.qameta.allure.model.Parameter().setName("Capabilities").setValue(ConfigContext.getCapabilities().toString()));
            }
        }
        return parameters;
    }
}
