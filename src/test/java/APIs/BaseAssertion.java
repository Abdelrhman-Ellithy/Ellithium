package APIs;

import Ellithium.Utilities.assertion.AssertionExecutor;
import org.testng.annotations.BeforeMethod;

public class BaseAssertion {
    protected AssertionExecutor.soft soft;
    @BeforeMethod
    public void assertSetup(){
        soft = new AssertionExecutor.soft();
    }
}
