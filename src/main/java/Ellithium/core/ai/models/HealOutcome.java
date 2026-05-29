package Ellithium.core.ai.models;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public record HealOutcome(WebElement element, By reconstructedLocator, double score, int tier) {

    public static HealOutcome of(WebElement element, double score, int tier) {
        return new HealOutcome(element, ElementFingerprint.reconstructLocator(element), score, tier);
    }

    public static HealOutcome of(WebElement element, By reconstructedLocator, double score, int tier) {
        return new HealOutcome(element, reconstructedLocator, score, tier);
    }

    public boolean isHit() {
        return element != null;
    }
}
