package Ellithium.core.ai.models;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RecordedInteractionTest {

    private RecordedInteraction make(String action, String locator, String data,
                                     String name, String tag) {
        return new RecordedInteraction(action, locator, data, name, tag);
    }

    @Test
    public void getActionType_returnsValue() {
        Assert.assertEquals(make("click", "By.id(\"btn\")", null, "Submit", "button").getActionType(), "click");
    }

    @Test
    public void getLocator_returnsValue() {
        Assert.assertEquals(make("click", "By.id(\"x\")", null, null, "button").getLocator(), "By.id(\"x\")");
    }

    @Test
    public void getData_returnsValue() {
        Assert.assertEquals(make("sendData", "By.id(\"u\")", "admin", null, "input").getData(), "admin");
    }

    @Test
    public void getElementName_returnsValue() {
        Assert.assertEquals(make("click", null, null, "Login Button", "button").getElementName(), "Login Button");
    }

    @Test
    public void getTagName_returnsValue() {
        Assert.assertEquals(make("click", null, null, null, "a").getTagName(), "a");
    }

    @Test
    public void getTimestamp_isPositive() {
        Assert.assertTrue(make("click", null, null, null, "button").getTimestamp() > 0);
    }

    @Test
    public void toString_withElementNameAndLocator_containsBoth() {
        RecordedInteraction r = make("click", "By.id(\"btn\")", null, "Submit", "button");
        String s = r.toString();
        Assert.assertTrue(s.contains("click"));
        Assert.assertTrue(s.contains("Submit"));
        Assert.assertTrue(s.contains("By.id(\"btn\")"));
    }

    @Test
    public void toString_withData_containsData() {
        RecordedInteraction r = make("sendData", "By.id(\"u\")", "admin", "Username", "input");
        Assert.assertTrue(r.toString().contains("admin"));
    }

    @Test
    public void toString_nullData_doesNotContainDataSection() {
        RecordedInteraction r = make("click", "By.id(\"b\")", null, null, "button");
        Assert.assertFalse(r.toString().contains("with data"));
    }
}
