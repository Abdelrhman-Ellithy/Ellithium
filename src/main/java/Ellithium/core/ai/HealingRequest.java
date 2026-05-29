package Ellithium.core.ai;

import Ellithium.core.ai.models.ElementFingerprint;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public record HealingRequest(WebDriver driver, By brokenLocator, StackTraceElement[] stackTrace,
                             String actionType, String callerMethod, String fieldName,
                             String locatorValue, ElementFingerprint baseline) {
}
