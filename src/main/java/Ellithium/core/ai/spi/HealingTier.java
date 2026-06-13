package Ellithium.core.ai.spi;

import Ellithium.core.ai.models.HealingRequest;
import Ellithium.core.ai.models.HealOutcome;

public interface HealingTier {

    int order();

    boolean isAvailable();

    boolean persistsOwnHeal();

    HealOutcome heal(HealingRequest request);

    default int apiVersion() { return 1; }
}
