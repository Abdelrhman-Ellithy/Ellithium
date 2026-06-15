package Ellithium.core.execution.listener;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

public class AppiumListenerTest {

    private final appiumListener listener = new appiumListener();

    // ── formatLocator ─────────────────────────────────────────────────────

    @Test
    public void formatLocator_stripsBy() throws Exception {
        Assert.assertEquals(invokeFormatLocator("By.id: login"), "id: login");
    }

    @Test
    public void formatLocator_stripsAppiumBy() throws Exception {
        Assert.assertEquals(invokeFormatLocator("AppiumBy.accessibilityId: btn"), "accessibilityId: btn");
    }

    @Test
    public void formatLocator_nullArg_returnsNull() throws Exception {
        Assert.assertEquals(invokeFormatLocator(null), "null");
    }

    @Test
    public void formatLocator_stripsBraces() throws Exception {
        String result = invokeFormatLocator("By.id: {elem}");
        Assert.assertFalse(result.contains("{"));
        Assert.assertFalse(result.contains("}"));
    }

    // ── toReadable ────────────────────────────────────────────────────────

    @Test
    public void toReadable_camelCase_insertSpaces() throws Exception {
        Assert.assertEquals(invokeToReadable("findElement"), "Find Element");
    }

    @Test
    public void toReadable_singleWord_capitalized() throws Exception {
        Assert.assertEquals(invokeToReadable("quit"), "Quit");
    }

    @Test
    public void toReadable_multipleUpperCase_insertsSpaceBeforeEach() throws Exception {
        String result = invokeToReadable("executeScript");
        Assert.assertTrue(result.contains(" "));
    }

    // ── isUtilityMethod ───────────────────────────────────────────────────

    @Test
    public void isUtilityMethod_getCurrentUrl_returnsTrue() throws Exception {
        Assert.assertTrue(invokeIsUtilityMethod("getCurrentUrl"));
    }

    @Test
    public void isUtilityMethod_getAttribute_returnsTrue() throws Exception {
        Assert.assertTrue(invokeIsUtilityMethod("getAttribute"));
    }

    @Test
    public void isUtilityMethod_isDisplayed_returnsTrue() throws Exception {
        Assert.assertTrue(invokeIsUtilityMethod("isDisplayed"));
    }

    @Test
    public void isUtilityMethod_findElement_returnsFalse() throws Exception {
        Assert.assertFalse(invokeIsUtilityMethod("findElement"));
    }

    @Test
    public void isUtilityMethod_unknownMethod_returnsFalse() throws Exception {
        Assert.assertFalse(invokeIsUtilityMethod("somethingCustom"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String invokeFormatLocator(Object arg) throws Exception {
        Method m = appiumListener.class.getDeclaredMethod("formatLocator", Object.class);
        m.setAccessible(true);
        return (String) m.invoke(listener, arg);
    }

    private String invokeToReadable(String input) throws Exception {
        Method m = appiumListener.class.getDeclaredMethod("toReadable", String.class);
        m.setAccessible(true);
        return (String) m.invoke(listener, input);
    }

    private boolean invokeIsUtilityMethod(String name) throws Exception {
        Method m = appiumListener.class.getDeclaredMethod("isUtilityMethod", String.class);
        m.setAccessible(true);
        return (boolean) m.invoke(listener, name);
    }
}
