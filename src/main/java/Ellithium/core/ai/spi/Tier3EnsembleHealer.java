package Ellithium.core.ai.spi;

import Ellithium.core.ai.EnsembleHealer;
import Ellithium.core.ai.HealingRequest;
import Ellithium.core.ai.models.HealOutcome;

public final class Tier3EnsembleHealer implements HealingTier {

    @Override
    public int order() {
        return 2;
    }

    @Override
    public boolean isAvailable() {
        return EnsembleHealer.isAvailable();
    }

    @Override
    public boolean persistsOwnHeal() {
        return false;
    }

    @Override
    public HealOutcome heal(HealingRequest request) {
        return EnsembleHealer.tryEnsembleHeal(request.driver(), request.brokenLocator(),
                request.actionType(), request.callerMethod(), request.fieldName(),
                request.locatorValue(), request.baseline());
    }
}
