package Ellithium.config.managment;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.Utilities.interactions.WaitManager;
import Ellithium.core.API.APIFilterHelper;
import Ellithium.config.Internal.VersionChecker;
import Ellithium.core.driver.*;
import Ellithium.core.execution.Analyzer.RetryAnalyzer;
import Ellithium.core.logging.Logger;
import Ellithium.core.reporting.internal.AllureHelper;
import com.google.common.io.Files;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.qameta.allure.model.Parameter;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GeneralHandler {
    
    /**
     * Internally used with The Ellithium Not for General Use.
     * @param browserName Slider element locator
     * @param testName X offset to move
     * @return The saved screenshot file
     */
    public static File testFailed( String browserName, String testName)  {
        try {
            TakesScreenshot camera = DriverFactory.getCurrentDriver();
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
    
    private static void AttachLogs(){
        String logs = Logger.getCurrentExecutionLogs();
        try (InputStream logStream = new ByteArrayInputStream(logs.getBytes(StandardCharsets.UTF_8))) {
            Allure.addAttachment("Execution Log File", "text/plain", logStream, ".log");
            Logger.info("Execution logs successfully attached to the Allure report.");
        } catch (IOException e) {
            Logger.error("Failed to attach execution logs: " + e.getMessage());
        }
    }

    @Step("Attachments")
    public static void addAttachments(){
        GeneralHandler.AttachLogs();
    }
    
    public static void StartRoutine(){
        ConfigContext.setIsLoggingOn(false);
        AllureHelper.deleteAllureResultsDir();
        AllureHelper.addEnvironmentDetailsToReport();
        ConfigContext.setIsLoggingOn(true);
        VersionChecker.solveVersion();
        APIFilterHelper.applyFilter();
        WaitManager.initializeTimeoutAndPolling();
        RetryAnalyzer.initRetryCount();
    }
    
    public static List<Parameter> getParameters(){
        List<io.qameta.allure.model.Parameter>parameters=new ArrayList<>();
        DriverConfiguration currentDriverConfiguration=DriverFactory.getCurrentDriverConfiguration();
        DriverType type=currentDriverConfiguration.getDriverType();
        if(type!=null){
            parameters.add(new io.qameta.allure.model.Parameter().setName("DriverType").setValue(type.getName()));
            if(type instanceof LocalDriverType || type instanceof RemoteDriverType){
                parameters.add(new io.qameta.allure.model.Parameter().setName("HeadlessMode").setValue(currentDriverConfiguration.getHeadlessMode().getName()));
                parameters.add(new io.qameta.allure.model.Parameter().setName("PageLoadStrategyMode").setValue(currentDriverConfiguration.getPageLoadStrategy().getName()));
                parameters.add(new io.qameta.allure.model.Parameter().setName("PrivateMode").setValue(currentDriverConfiguration.getPrivateMode().getName()));
                parameters.add(new io.qameta.allure.model.Parameter().setName("SandboxMode").setValue(currentDriverConfiguration.getSandboxMode().getName()));
                parameters.add(new io.qameta.allure.model.Parameter().setName("WebSecurityMode").setValue(currentDriverConfiguration.getWebSecurityMode().getName()));
            }
            if(type instanceof RemoteDriverType ||type instanceof MobileDriverType){
                parameters.add(new io.qameta.allure.model.Parameter().setName("Remote Address").setValue(currentDriverConfiguration.getRemoteAddress().toString()));
                parameters.add(new io.qameta.allure.model.Parameter().setName("Capabilities").setValue(currentDriverConfiguration.getCapabilities().asMap().toString()));
            }
        }
        return parameters;
    }
}
