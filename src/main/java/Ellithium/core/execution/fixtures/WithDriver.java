package Ellithium.core.execution.fixtures;

import Ellithium.core.driver.*;

import java.lang.annotation.*;

/**
 * Declarative test fixture annotation for automatic thread-safe WebDriver allocation,
 * parameter injection, and lifecycle teardown.
 *
 * <p>Can be placed at the method level or class level.
 * Supports injecting {@link org.openqa.selenium.WebDriver}, {@link Ellithium.Utilities.interactions.DriverActions},
 * {@link Ellithium.Utilities.interactions.ElementActions}, {@link Ellithium.Utilities.interactions.WaitActions},
 * and {@link Ellithium.Utilities.interactions.NavigationActions} directly into test method parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface WithDriver {

    /**
     * Browser driver type to instantiate. Defaults to Chrome.
     */
    LocalDriverType type() default LocalDriverType.Chrome;

    /**
     * Headless mode execution. Defaults to false.
     */
    boolean headless() default false;

    /**
     * Incognito / private browsing mode. Defaults to false.
     */
    boolean incognito() default false;

    /**
     * Page load strategy. Defaults to Normal.
     */
    PageLoadStrategyMode pageLoadStrategy() default PageLoadStrategyMode.Normal;

    /**
     * Web security mode. Defaults to SecureMode.
     */
    WebSecurityMode webSecurity() default WebSecurityMode.SecureMode;

    /**
     * Sandbox mode. Defaults to Sandbox.
     */
    SandboxMode sandbox() default SandboxMode.Sandbox;

    /**
     * Automatically quits and tears down the allocated WebDriver upon test completion.
     * Defaults to true.
     */
    boolean autoQuit() default true;
}
