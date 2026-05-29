package Ellithium.core.ai.spi;

import Ellithium.core.ai.BaselineStore;
import Ellithium.core.ai.HealingRequest;
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
        return false;
    }

    @Override
    public HealOutcome heal(HealingRequest request) {
        return BaselineStore.tryAlgorithmicHeal(request.driver(), request.brokenLocator());
    }
}
