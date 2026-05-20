package Ellithium.Utilities.ai;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-session cache of DOM element embedding vectors for Tier 3 local ONNX healing.
 *
 * <p>Stores {@code elementKey → float[]} vectors so the same element's embedding is
 * computed once per page and reused across heal attempts on that page.</p>
 *
 * <h3>Invalidation policy</h3>
 * <ul>
 *   <li>Full invalidation on every navigation event (URL change, refresh, back/forward).
 *       Wired via {@link #invalidate()} in {@code NavigationActions}.</li>
 *   <li>Partial invalidation on DOM mutation: {@link #markDomMutated()} sets a flag;
 *       the next {@link #get} call triggers a delta re-embed for changed elements only.
 *       (Full implementation active when Tier 3 model is embedded.)</li>
 * </ul>
 *
 * <p>Thread-safe: {@link ConcurrentHashMap} for the vector store; {@link AtomicBoolean}
 * for the mutation flag.</p>
 */
public class ElementVectorCache {

    private static final ElementVectorCache INSTANCE = new ElementVectorCache();

    private final ConcurrentHashMap<String, float[]> vectors = new ConcurrentHashMap<>();
    private final AtomicBoolean domMutated = new AtomicBoolean(false);

    private ElementVectorCache() {}

    public static ElementVectorCache getInstance() { return INSTANCE; }

    // ──────────────────────── Cache Operations ────────────────────────

    /**
     * Returns the cached vector for the given element key, or {@code null} if absent.
     * Callers should treat a {@code null} return as a cache miss and compute the vector.
     */
    public float[] get(String elementKey) {
        return vectors.get(elementKey);
    }

    /**
     * Stores a vector for an element key.
     *
     * @param elementKey A stable string key for the element (e.g., reconstructed locator string)
     * @param vector     The embedding vector produced by the ONNX model
     */
    public void put(String elementKey, float[] vector) {
        if (elementKey != null && vector != null) {
            vectors.put(elementKey, vector);
        }
    }

    /** Returns {@code true} when the DOM has been mutated since the last embed pass. */
    public boolean isDomMutated() { return domMutated.get(); }

    /**
     * Signals that the DOM has changed (called by the MutationObserver JS hook).
     * The next heal attempt will re-embed changed elements rather than using stale vectors.
     */
    public void markDomMutated() { domMutated.set(true); }

    /** Clears the mutation flag after delta re-embedding is complete. */
    public void clearMutationFlag() { domMutated.set(false); }

    /**
     * Fully invalidates the cache. Call on every navigation event
     * (URL change, page refresh, browser back/forward) so vectors from the previous
     * page are not matched against elements on the new page.
     */
    public void invalidate() {
        vectors.clear();
        domMutated.set(false);
    }

    /** Returns the number of cached element vectors. */
    public int size() { return vectors.size(); }

    /** Returns {@code true} when the cache is empty. */
    public boolean isEmpty() { return vectors.isEmpty(); }
}
