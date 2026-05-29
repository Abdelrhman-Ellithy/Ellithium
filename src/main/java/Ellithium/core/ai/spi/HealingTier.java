package Ellithium.core.ai.spi;

import Ellithium.core.ai.HealingRequest;
import Ellithium.core.ai.models.HealOutcome;

public interface HealingTier {

    int order();

    boolean isAvailable();

    boolean persistsOwnHeal();

    HealOutcome heal(HealingRequest request);
}
