package Ellithium.core.ai.spi;

import Ellithium.core.ai.healing.AISelfHealer;
import Ellithium.core.ai.models.HealingRequest;
import Ellithium.core.ai.models.ElementFingerprint;
import Ellithium.core.ai.models.HealOutcome;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public final class Tier3LLMHealer implements HealingTier {

    @Override
    public int order() {
        return 3;
    }

    @Override
    public boolean isAvailable() {
        String key = Ellithium.core.ai.config.AIConfigLoader.getLlmApiKey();
        return key != null && !key.isBlank();
    }

    @Override
    public boolean persistsOwnHeal() {
        return true;
    }

    @Override
    public HealOutcome heal(HealingRequest request) {
        WebElement element = AISelfHealer.attemptHeal(
                request.driver(), request.brokenLocator(), request.stackTrace());
        if (element == null) return null;
        By healed = AISelfHealer.getCachedHealedLocator(request.driver(), request.brokenLocator());
        if (healed == null) healed = ElementFingerprint.reconstructLocator(element);
        return HealOutcome.of(element, healed, AISelfHealer.getLastHealConfidence(), 3);
    }
}
