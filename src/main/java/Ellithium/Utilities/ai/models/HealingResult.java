package Ellithium.Utilities.ai.models;

/**
 * Represents the result of an AI healing attempt.
 * Contains the suggested new locator expression and a confidence score
 * to guard against false-positive healing (LLM hallucination).
 */
public class HealingResult {

    private final String newLocatorExpression;
    private final double confidence;
    private final String reasoning;

    /**
     * @param newLocatorExpression The new By expression as a string (e.g., "By.cssSelector(\"#new-id\")")
     * @param confidence           Confidence score from 0.0 to 1.0
     * @param reasoning            Human-readable explanation of why this locator was chosen
     */
    public HealingResult(String newLocatorExpression, double confidence, String reasoning) {
        this.newLocatorExpression = newLocatorExpression;
        this.confidence = confidence;
        this.reasoning = reasoning;
    }

    public String getNewLocatorExpression() {
        return newLocatorExpression;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    /**
     * Checks if the confidence exceeds the given threshold.
     *
     * @param threshold The minimum acceptable confidence (e.g., 0.85)
     * @return true if this result is confident enough for automatic healing
     */
    public boolean isConfidentEnough(double threshold) {
        return confidence >= threshold;
    }

    @Override
    public String toString() {
        return "HealingResult{" +
                "locator='" + newLocatorExpression + '\'' +
                ", confidence=" + String.format("%.2f", confidence) +
                ", reasoning='" + reasoning + '\'' +
                '}';
    }
}
