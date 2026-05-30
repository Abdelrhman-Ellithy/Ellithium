package Ellithium.core.ai;

import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Spec for the two-retriever candidate union ordering. The resolver's exact gold/silver hits must be
 * scored BEFORE the broad collector pool, so they are never dropped by the per-attempt candidate cap
 * on a page with more interactive elements than the cap.
 */
public class CandidateMergeTest {

    @Test
    public void resolverHits_orderedBeforePool() {
        WebElement resolverHit = mock(WebElement.class);
        List<WebElement> pool = new ArrayList<>();
        for (int i = 0; i < 15; i++) pool.add(mock(WebElement.class));

        List<WebElement> merged = EnsembleHealer.mergeCandidates(List.of(resolverHit), pool);

        Assert.assertSame(merged.get(0), resolverHit,
                "resolver exact hit must be first so the candidate cap never drops it");
        Assert.assertEquals(merged.size(), 16);
    }

    @Test
    public void resolverHitAlsoInPool_isNotDuplicated() {
        WebElement shared = mock(WebElement.class);
        List<WebElement> pool = new ArrayList<>(List.of(mock(WebElement.class), shared, mock(WebElement.class)));

        List<WebElement> merged = EnsembleHealer.mergeCandidates(List.of(shared), pool);

        Assert.assertSame(merged.get(0), shared, "the shared element keeps the resolver's priority position");
        Assert.assertEquals(merged.size(), 3, "identity dedup keeps the union free of duplicates");
    }

    @Test
    public void nullResolverHits_returnsPoolOnly() {
        List<WebElement> pool = List.of(mock(WebElement.class), mock(WebElement.class));
        List<WebElement> merged = EnsembleHealer.mergeCandidates(null, pool);
        Assert.assertEquals(merged.size(), 2);
    }
}
