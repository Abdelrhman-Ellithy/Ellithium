package Ellithium.core.ai.spi;

import Ellithium.core.ai.healing.BaselineStore;
import Ellithium.core.ai.models.HealingRequest;
import Ellithium.core.ai.models.HealOutcome;

public final class Tier1AlgorithmicHealer implements HealingTier {

    @Override
    public int order() {
        return 1;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean persistsOwnHeal() {
        // false: persistence/source-patching is deferred to HealingOrchestrator's shared
        // post-resolveInteractiveElement() path (same as Tier 2/3), so a mutation/attribute/DOM-scan
        // match that the orchestrator later rejects (e.g. a non-interactive container for a click
        // action) never gets a baseline persisted or a source patch queued for a heal that wasn't
        // actually used.
        return false;
    }

    @Override
    public HealOutcome heal(HealingRequest request) {
        return BaselineStore.tryAlgorithmicHeal(request.driver(), request.brokenLocator(),
                request.stackTrace(), request.actionType(), request.hints());
    }
}
