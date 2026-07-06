package Ellithium.core.ai.spi;

import Ellithium.core.ai.models.HealingRequest;
import Ellithium.core.ai.models.HealOutcome;

/**
 * A tier in the healing cascade. {@link HealingOrchestrator} tries each registered tier, ordered by
 * {@link #order()}; returning {@code null} (or a {@link HealOutcome} with a null element) means
 * "fall through to the next tier."
 *
 * <p><b>{@link #apiVersion()} contract:</b> bump the default ONLY for a change that can break an
 * existing external implementation at the interface/method-signature level (e.g. a new abstract
 * method, a changed return/parameter type). Because {@code apiVersion()} is a default method, an
 * external implementation that does NOT override it will silently report whatever version the
 * currently-loaded {@code HealingTier} interface defines — a bump does not, by itself, protect
 * against every kind of change. Purely additive changes to {@link HealingRequest} (new fields with a
 * preserved legacy constructor, like {@code hints}) do NOT require a bump: existing implementations
 * keep compiling and running correctly since they only ever read via named accessors (see
 * {@link HealingRequest}'s Javadoc for the one usage pattern that would NOT survive such a change).
 */
public interface HealingTier {

    int order();

    boolean isAvailable();

    boolean persistsOwnHeal();

    HealOutcome heal(HealingRequest request);

    default int apiVersion() { return 1; }
}
