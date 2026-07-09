package Ellithium.core.ai.models;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cross-tier candidate handoff for one heal operation. Earlier tiers append the near-miss
 * candidates they had to abstain on (a token-vote tie, a below-threshold DOM-scan best, a mutation
 * that resolved but failed cross-validation); later tiers read the shortlist and score it FIRST —
 * a relative comparison between 2–3 finalists is both cheaper and more reliable than a page-wide
 * ranking against an absolute threshold.
 *
 * <p>Shortlist entries are ADVISORY: a later tier considers them first but never auto-accepts one —
 * earlier-tier evidence is literal-token evidence and must not become a semantic prior. Candidates
 * carry an optional reconstructed locator so a later tier can re-resolve an element that went
 * stale between tiers. One instance per heal operation, single-threaded; capped so a later tier's
 * shortlist pass stays O(1).
 */
public final class HealingHints {

    /** Max shortlist entries retained — later tiers embed/score each one, so this bounds their work. */
    private static final int MAX_HINTS = 5;

    /**
     * @param element a live element reference at the time the hint was added — Tier-3 today only
     *                 consumes {@link #describe()} (a string), never this reference directly, so a
     *                 future consumer that dereferences {@code element} after other tiers have run
     *                 must handle it possibly having gone stale (the DOM may have changed between
     *                 the tier that added the hint and the tier that reads it).
     */
    public record CandidateHint(WebElement element, By locator, double score,
                                String evidence, int sourceTier) {
    }

    private final List<CandidateHint> shortlist = new ArrayList<>();

    /** Appends a candidate; silently ignored once the cap is reached or when {@code element} is null. */
    public void add(WebElement element, By locator, double score, String evidence, int sourceTier) {
        if (element == null || shortlist.size() >= MAX_HINTS) return;
        for (CandidateHint existing : shortlist) {
            if (existing.element().equals(element)) return;
        }
        shortlist.add(new CandidateHint(element, locator, score, evidence, sourceTier));
    }

    public List<CandidateHint> shortlist() {
        return Collections.unmodifiableList(shortlist);
    }

    public boolean isEmpty() {
        return shortlist.isEmpty();
    }

    public int size() {
        return shortlist.size();
    }

    /** Compact single-line description for reporting and LLM prompt context. */
    public String describe() {
        if (shortlist.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (CandidateHint h : shortlist) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(h.locator() != null ? h.locator() : "(element)")
              .append(" [tier ").append(h.sourceTier())
              .append(", ").append(h.evidence())
              .append(", score ").append(String.format(java.util.Locale.ROOT, "%.2f", h.score()))
              .append("]");
        }
        return sb.toString();
    }
}
