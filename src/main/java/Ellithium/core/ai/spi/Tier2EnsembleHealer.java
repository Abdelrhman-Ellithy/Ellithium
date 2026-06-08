package Ellithium.core.ai.spi;

import Ellithium.core.ai.EnsembleHealer;
import Ellithium.core.ai.SemanticLocatorResolver;
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
        return true;
    }

    @Override
    public boolean persistsOwnHeal() {
        return false;
    }

    @Override
    public HealOutcome heal(HealingRequest request) {
        if (EnsembleHealer.isAvailable()) {
            return EnsembleHealer.tryEnsembleHeal(request.driver(), request.brokenLocator(),
                    request.actionType(), request.callerMethod(), request.fieldName(),
                    request.locatorValue(), request.baseline());
        }
        WebElement el = SemanticLocatorResolver.trySemanticHeal(request.driver(),
                request.callerMethod(), request.fieldName(),
                request.actionType(), request.locatorValue(), request.baseline());
        if (el == null) return null;
        double score = EnsembleHealer.scoreWithBatchedAttrs(request.baseline(), request.driver(), el);
        return HealOutcome.of(el, score, 2);
    }
}
