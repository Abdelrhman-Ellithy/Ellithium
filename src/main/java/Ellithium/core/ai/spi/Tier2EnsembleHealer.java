package Ellithium.core.ai.spi;

import Ellithium.core.ai.healing.EnsembleHealer;
import Ellithium.core.ai.healing.SemanticLocatorResolver;
import Ellithium.core.ai.models.HealOutcome;
import Ellithium.core.ai.models.HealingRequest;
import org.openqa.selenium.WebElement;

public final class Tier2EnsembleHealer implements HealingTier {

    @Override
    public int order() {
        return 2;
    }

    @Override
    public boolean isAvailable() {
        return Ellithium.core.ai.config.AIConfigLoader.isTier2Enabled();
    }

    @Override
    public boolean persistsOwnHeal() {
        return false;
    }

    @Override
    public HealOutcome heal(HealingRequest request) {
        if (EnsembleHealer.isAvailable()) {
            HealOutcome outcome = EnsembleHealer.tryEnsembleHeal(request.driver(), request.brokenLocator(),
                    request.actionType(), request.callerMethod(), request.fieldName(),
                    request.locatorValue(), request.baseline(), request.hints());
            if (outcome != null) {
                if (Double.isNaN(outcome.score()) || outcome.score() < Ellithium.core.ai.config.AIConfigLoader.getConfidenceThreshold()) {
                    return null;
                }
                return outcome;
            }
        }
        return SemanticLocatorResolver.trySemanticHealOutcome(request.driver(),
                request.callerMethod(), request.fieldName(),
                request.actionType(), request.locatorValue(), request.baseline());
    }
}
