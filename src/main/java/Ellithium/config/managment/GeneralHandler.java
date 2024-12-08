package Ellithium.config.managment;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.Utilities.interactions.WaitManager;
import Ellithium.config.Internal.APIFilterHelper;
import Ellithium.config.Internal.VersionChecker;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.DriverType;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.logsUtils;
import Ellithium.core.reporting.Reporter;
import com.google.common.io.Files;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Parameter;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GeneralHandler {

    public static File testFailed( String browserName, String testName)  {
        try {
            TakesScreenshot camera =((TakesScreenshot) DriverFactory.getCurrentDriver());
            assert camera != null;
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
    public static void StartRoutine(){
        APIFilterHelper.applyFilter();
        VersionChecker.solveVersion();
        WaitManager.initializeTimeoutAndPolling();
        initRetryCount();
    }
    public static List<Parameter> getParameters(){
        List<io.qameta.allure.model.Parameter>parameters=new ArrayList<>();
        DriverType type=ConfigContext.getDriverType();
        parameters.add(new io.qameta.allure.model.Parameter().setName("DriverType").setValue(ConfigContext.getValue(type)));
        if(type== DriverType.Chrome||type== DriverType.Safari||type== DriverType.FireFox||type== DriverType.Edge||type==DriverType.REMOTE_Chrome||type==DriverType.REMOTE_Edge||type==DriverType.REMOTE_FireFox||type==DriverType.REMOTE_Safari){
            parameters.add(new io.qameta.allure.model.Parameter().setName("HeadlessMode").setValue(ConfigContext.getValue(ConfigContext.getHeadlessMode())));
            parameters.add(new io.qameta.allure.model.Parameter().setName("PageLoadStrategyMode").setValue(ConfigContext.getValue(ConfigContext.getPageLoadStrategy())));
            parameters.add(new io.qameta.allure.model.Parameter().setName("PrivateMode").setValue(ConfigContext.getValue(ConfigContext.getPrivateMode())));
            parameters.add(new io.qameta.allure.model.Parameter().setName("SandboxMode").setValue(ConfigContext.getValue(ConfigContext.getSandboxMode())));
            parameters.add(new io.qameta.allure.model.Parameter().setName("WebSecurityMode").setValue(ConfigContext.getValue(ConfigContext.getWebSecurityMode())));
        }
        if(type==DriverType.REMOTE_Chrome||type==DriverType.REMOTE_Edge||type==DriverType.REMOTE_FireFox||type==DriverType.REMOTE_Safari||type==DriverType.Android||type==DriverType.IOS){
            parameters.add(new io.qameta.allure.model.Parameter().setName("Remote Address").setValue(ConfigContext.getRemoteAddress().toString()));
            parameters.add(new io.qameta.allure.model.Parameter().setName("Capabilities").setValue(ConfigContext.getCapabilities().toString()));
        }
        return parameters;
    }
    public static void initRetryCount(){
        String countStr=PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(),"retryCountOnFailure");
        int count =Integer.parseInt(countStr);
        ConfigContext.setRetryCount(count);
    }
}
