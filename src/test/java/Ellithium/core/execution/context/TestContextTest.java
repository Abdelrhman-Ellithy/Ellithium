package Ellithium.core.execution.context;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestContextTest {

    @Test
    public void testId_withinFrameworkExecution_returnsNonNull() {
        // The framework listener binds TestContext for every running test
        String id = TestContext.testId();
        Assert.assertNotNull(id);
        Assert.assertTrue(id.contains("TestContextTest"));
    }

    @Test
    public void current_withinFrameworkExecution_returnsTestContextData() {
        TestContextData ctx = TestContext.current();
        Assert.assertNotNull(ctx);
        Assert.assertNotNull(ctx.testId());
    }

    @Test
    public void testContextData_record_holdsValues() {
        TestContextData data = new TestContextData("tid-1", "LoginTest", "chrome");
        Assert.assertEquals(data.testId(), "tid-1");
        Assert.assertEquals(data.testName(), "LoginTest");
        Assert.assertEquals(data.browser(), "chrome");
    }

    @Test
    public void testContextData_equality_sameFields() {
        TestContextData a = new TestContextData("x", "y", "z");
        TestContextData b = new TestContextData("x", "y", "z");
        Assert.assertEquals(a, b);
    }

    @Test
    public void testContextData_inequality_differentFields() {
        TestContextData a = new TestContextData("x", "y", "z");
        TestContextData b = new TestContextData("x", "y", "firefox");
        Assert.assertNotEquals(a, b);
    }
}
