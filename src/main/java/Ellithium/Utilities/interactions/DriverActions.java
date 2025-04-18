package Ellithium.Utilities.interactions;

import Ellithium.Utilities.generators.TestDataGenerator;
import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.common.io.Files;
import org.openqa.selenium.*;
import java.io.File;
/**
 * Provides a comprehensive set of WebDriver interaction methods with built-in waits and reporting.
 * @param <T> The specific WebDriver type
 */
public class DriverActions<T extends WebDriver> extends BaseActions<T> {
    /**
     * Creates a new DriverActions instance.
     * @param driver WebDriver instance to wrap
     */
    @SuppressWarnings("unchecked")
    public DriverActions(T driver) {
        super(driver);
    }

    public JavaScriptActions JSActions(){
        return new JavaScriptActions<>(driver);
    }
    public Sleep sleep(){
        return new Sleep();
    }
    public AlertActions alerts() {
        return new AlertActions<>(driver);
    }

    public FrameActions frames() {
        return new FrameActions<>(driver);
    }

    public WindowActions windows() {
        return new WindowActions<>(driver);
    }

    public WaitActions waits() {
        return new WaitActions<>(driver);
    }
    public ElementActions elements() {
        return new ElementActions<>(driver);
    }

    public SelectActions select() {
        return new SelectActions<>(driver);
    }

    public NavigationActions navigation() {
        return new NavigationActions<>(driver);
    }

    public MouseActions mouse() {
        return new MouseActions<>(driver);
    }

    /**
     * Provides access to key press and keyboard actions.
     * @return KeyPressActions instance
     */
    public KeyPressActions<T> keyPress() {
        return new KeyPressActions<>(driver);
    }
}