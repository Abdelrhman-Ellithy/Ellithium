package Ellithium.core.ai.models;

import org.openqa.selenium.WebElement;

/**
 * A live DOM element matched by a Tier 2 semantic strategy, paired with the strategy's tier weight.
 * Used by the ensemble fusion to populate the graded f2 signal (gold=1.0, silver=0.75,
 * bronze=0.5, iron=0.3) per candidate.
 */
public final class SemanticHit {
    public final WebElement element;
    public final double tierWeight;
    public final String strategyDescription;

    public SemanticHit(WebElement element, double tierWeight, String strategyDescription) {
        this.element = element;
        this.tierWeight = tierWeight;
        this.strategyDescription = strategyDescription;
    }
}
