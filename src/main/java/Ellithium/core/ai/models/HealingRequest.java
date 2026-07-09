package Ellithium.core.ai.models;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * One heal operation's input, passed unchanged through the tier cascade. {@code hints} is the
 * mutable cross-tier candidate handoff ({@link HealingHints}): earlier tiers append near-miss
 * candidates as they abstain, later tiers read them — the record itself stays immutable.
 *
 * <p><b>Contract stability for {@link Ellithium.core.ai.spi.HealingTier} implementations
 * (including external ones loaded via {@code ServiceLoader}):</b> only the accessor methods
 * ({@code driver()}, {@code brokenLocator()}, ..., {@code hints()}) are part of the stable contract.
 * The component COUNT and ORDER may grow across versions as additive, backward-compatible fields
 * (like {@code hints}, added after {@code baseline}) — a legacy constructor is kept for each prior
 * shape. Do NOT match this record with an exhaustive record deconstruction pattern (e.g. a
 * {@code case HealingRequest(var a, var b, ...)} naming every component); such a pattern is tied to
 * the exact current component count and will fail to compile against a newer version that added a
 * field. Always read via the named accessors.
 */
public record HealingRequest(WebDriver driver, By brokenLocator, StackTraceElement[] stackTrace,
                             String actionType, String callerMethod, String fieldName,
                             String locatorValue, ElementFingerprint baseline, HealingHints hints) {

    public HealingRequest {
        if (hints == null) hints = new HealingHints();
    }

    public HealingRequest(WebDriver driver, By brokenLocator, StackTraceElement[] stackTrace,
                          String actionType, String callerMethod, String fieldName,
                          String locatorValue, ElementFingerprint baseline) {
        this(driver, brokenLocator, stackTrace, actionType, callerMethod, fieldName,
                locatorValue, baseline, new HealingHints());
    }
}
