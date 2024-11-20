package Ellithium.config.managment;

import Ellithium.Utilities.helpers.PropertyHelper;
import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.config.Internal.APIFilterHelper;
import Ellithium.config.Internal.VersionChecker;
import Ellithium.core.driver.DriverFactory;
import Ellithium.core.driver.DriverType;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.logging.logsUtils;
import Ellithium.core.reporting.Reporter;
import com.google.common.io.Files;
import io.qameta.allure.Allure;
import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Parameter;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GeneralHandler implements TestLifecycleListener {
    private static Boolean BDDMode,NonBDDMode, flagReaded=false;
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
            NonBDDMode=false;
            flagReaded=true;
        }
        return BDDMode;
    }
    public static boolean getNonBDDMode(){
        if(flagReaded.equals(false)){
            String mode=PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(),"runMode");
            NonBDDMode=mode.equalsIgnoreCase("NonBDD");
            BDDMode=false;
            flagReaded=true;
        }
        return NonBDDMode;
    }
    public static void StartRoutine(){
        APIFilterHelper.applyFilter();
        VersionChecker.solveVersion();
        initRetryCount();
    }
    public static List<Parameter> getParameters(){
        List<io.qameta.allure.model.Parameter>parameters=new ArrayList<>();
        parameters.add(new io.qameta.allure.model.Parameter().setName("DriverType").setValue(ConfigContext.getValue(ConfigContext.getDriverType())));
        if(ConfigContext.getDriverType()== DriverType.Chrome||ConfigContext.getDriverType()== DriverType.Safari||ConfigContext.getDriverType()== DriverType.FireFox||ConfigContext.getDriverType()== DriverType.Edge){
            parameters.add(new io.qameta.allure.model.Parameter().setName("HeadlessMode").setValue(ConfigContext.getValue(ConfigContext.getHeadlessMode())));
            parameters.add(new io.qameta.allure.model.Parameter().setName("PageLoadStrategyMode").setValue(ConfigContext.getValue(ConfigContext.getPageLoadStrategy())));
            parameters.add(new io.qameta.allure.model.Parameter().setName("PrivateMode").setValue(ConfigContext.getValue(ConfigContext.getPrivateMode())));
            parameters.add(new io.qameta.allure.model.Parameter().setName("SandboxMode").setValue(ConfigContext.getValue(ConfigContext.getSandboxMode())));
            parameters.add(new io.qameta.allure.model.Parameter().setName("WebSecurityMode").setValue(ConfigContext.getValue(ConfigContext.getWebSecurityMode())));
        }
        return parameters;
    }
    public static void initRetryCount(){
        String countStr=PropertyHelper.getDataFromProperties(ConfigContext.getConfigFilePath(),"retryCountOnFailure");
        int count =Integer.parseInt(countStr);
        ConfigContext.setRetryCount(count);
    }
}
