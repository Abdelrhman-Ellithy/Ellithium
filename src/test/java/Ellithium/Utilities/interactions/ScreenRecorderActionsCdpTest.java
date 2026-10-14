package Ellithium.Utilities.interactions;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * Unit test validating dynamic reflection compatibility of CDP Screencast
 * against modern Chrome DevTools Protocol versions (e.g., v155+).
 */
public class ScreenRecorderActionsCdpTest {

    @Test
    public void testCdpPageStartScreencastDynamicReflection() throws Exception {
        Class<?> pageClass = Class.forName("org.openqa.selenium.devtools.v155.page.Page");
        Assert.assertNotNull(pageClass, "Page class must be loaded for v155");

        Method startMethod = null;
        for (Method m : pageClass.getMethods()) {
            if ("startScreencast".equals(m.getName()) && Modifier.isStatic(m.getModifiers())) {
                if (m.getParameterCount() > 0) {
                    startMethod = m;
                    break;
                }
            }
        }
        Assert.assertNotNull(startMethod, "startScreencast method must be dynamically located");

        Class<?>[] paramTypes = startMethod.getParameterTypes();
        Assert.assertTrue(paramTypes.length >= 5, "startScreencast must have at least 5 params in modern CDP");

        Object format = null;
        for (Class<?> inner : pageClass.getDeclaredClasses()) {
            if ("StartScreencastFormat".equals(inner.getSimpleName())) {
                format = inner.getField("JPEG").get(null);
                break;
            }
        }
        Assert.assertNotNull(format, "StartScreencastFormat.JPEG must exist");

        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i].equals(Optional.class)) {
                if (i == 0) {
                    args[i] = Optional.of(format);
                } else if (i == 1) {
                    args[i] = Optional.of(50);
                } else if (i == 4) {
                    args[i] = Optional.of(4);
                } else {
                    args[i] = Optional.empty();
                }
            }
        }

        Object startCommand = startMethod.invoke(null, args);
        Assert.assertNotNull(startCommand, "startScreencast Command must be generated without exception");
    }

    @Test
    public void testCdpPageStopScreencastDynamicReflection() throws Exception {
        Class<?> pageClass = Class.forName("org.openqa.selenium.devtools.v155.page.Page");
        Method stopMethod = null;
        for (Method m : pageClass.getMethods()) {
            if ("stopScreencast".equals(m.getName()) && Modifier.isStatic(m.getModifiers())) {
                stopMethod = m;
                break;
            }
        }
        Assert.assertNotNull(stopMethod, "stopScreencast method must be dynamically located");

        Object stopCommand;
        if (stopMethod.getParameterCount() == 0) {
            stopCommand = stopMethod.invoke(null);
        } else {
            Object[] args = new Object[stopMethod.getParameterCount()];
            java.util.Arrays.fill(args, Optional.empty());
            stopCommand = stopMethod.invoke(null, args);
        }
        Assert.assertNotNull(stopCommand, "stopScreencast Command must be generated without exception");
    }

    @Test
    public void testCdpPageScreencastFrameAckDynamicReflection() throws Exception {
        Class<?> pageClass = Class.forName("org.openqa.selenium.devtools.v155.page.Page");
        Method ackMethod = null;
        for (Method m : pageClass.getMethods()) {
            if ("screencastFrameAck".equals(m.getName()) && Modifier.isStatic(m.getModifiers())
                    && m.getParameterCount() == 1) {
                ackMethod = m;
                break;
            }
        }
        Assert.assertNotNull(ackMethod, "screencastFrameAck method must be dynamically located");
        Object ackCommand = ackMethod.invoke(null, 12345);
        Assert.assertNotNull(ackCommand, "screencastFrameAck Command must be generated without exception");
    }
}
