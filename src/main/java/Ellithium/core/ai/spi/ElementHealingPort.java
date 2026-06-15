package Ellithium.core.ai.spi;

import Ellithium.core.ai.models.HealOutcome;
import Ellithium.core.ai.models.HealingRequest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public interface ElementHealingPort {
    HealOutcome heal(HealingRequest request);
    By getCachedLocator(WebDriver driver, By brokenLocator);
}
