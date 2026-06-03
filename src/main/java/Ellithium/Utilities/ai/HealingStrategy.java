package Ellithium.Utilities.ai;

/**
 * Defines how the framework should handle self-healing when a locator fails.
 */
public enum HealingStrategy {
    /**
     * Do not attempt to heal broken locators. Throw the exception immediately.
     */
    DISABLED,

    /**
     * Rewrite the broken POM file with the healed locator, log the change,
     * and automatically continue the current test execution.
     */
    HEAL_AND_CONTINUE,

    /**
     * Analyze the failure and suggest a healed locator in the console/logs,
     * but do NOT rewrite the POM file and do NOT continue the test.
     * Use this for strict manual review.
     */
    SUGGEST_ONLY,

    /**
     * Rewrite the broken POM file with the healed locator and notify the engineer
     * (e.g., via Slack, email, or a specific log report), then immediately
     * continue the current test execution.
     */
    HEAL_AND_NOTIFY
}
