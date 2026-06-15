package Base;
import Ellithium.core.base.BDDSetup;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        glue = {"UI_BDD", "Base"},
        features="src/test/resources/features"
        ,tags="@test"
)
public class TestRunner extends BDDSetup {
}