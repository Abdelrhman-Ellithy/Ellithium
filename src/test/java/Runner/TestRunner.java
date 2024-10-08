package Runner;
import Ellithium.core.base.BDDSetup;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        glue = "Tests", // path to your stepDefinitions package, note you should use . instead of /
        features="src/test/resources/features" // path to your features folder
        ,tags = "@Run"
)
public class TestRunner extends BDDSetup {
}