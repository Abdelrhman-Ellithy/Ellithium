package Ellithium.core.ai.spi;

import Ellithium.core.ai.config.AIConfigLoader;
import Ellithium.core.ai.models.HealingRequest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HealingTiersSpiTest {

    @Test
    public void testTierContractProperties() {
        Tier1AlgorithmicHealer t1 = new Tier1AlgorithmicHealer();
        Assert.assertEquals(t1.order(), 1);
        Assert.assertFalse(t1.persistsOwnHeal(), "Tier 1 persistence is deferred to orchestrator");

        Tier2EnsembleHealer t2 = new Tier2EnsembleHealer();
        Assert.assertEquals(t2.order(), 2);
        Assert.assertFalse(t2.persistsOwnHeal(), "Tier 2 persistence is deferred to orchestrator");

        Tier3LLMHealer t3 = new Tier3LLMHealer();
        Assert.assertEquals(t3.order(), 3);
        Assert.assertTrue(t3.persistsOwnHeal(), "Tier 3 persists own heal");
    }

    @Test
    public void testTierAvailabilityMatchesConfig() {
        Tier1AlgorithmicHealer t1 = new Tier1AlgorithmicHealer();
        Assert.assertEquals(t1.isAvailable(), AIConfigLoader.isTier1Enabled());

        Tier2EnsembleHealer t2 = new Tier2EnsembleHealer();
        Assert.assertEquals(t2.isAvailable(), AIConfigLoader.isTier2Enabled());

        Tier3LLMHealer t3 = new Tier3LLMHealer();
        if (!AIConfigLoader.isTier3Enabled() || AIConfigLoader.getLlmApiKey() == null || AIConfigLoader.getLlmApiKey().isBlank()) {
            Assert.assertFalse(t3.isAvailable());
        }
    }
}
