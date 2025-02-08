package Runner;
import Ellithium.core.base.BDDSetup;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        glue = {"UI_BDD","Runner"},
        features="src/test/resources/features"
)
public class TestRunner extends BDDSetup {
}