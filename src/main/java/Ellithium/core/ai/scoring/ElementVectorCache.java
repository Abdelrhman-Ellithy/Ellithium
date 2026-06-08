package Ellithium.core.ai.scoring;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
 *   <li>DOM mutation: {@link #markDomMutated()} sets a flag; while set, {@link #get} returns
 *       {@code null} (cache miss) so callers re-embed against the mutated DOM rather than serving a
 *       stale vector. {@link #clearMutationFlag()} re-enables hits after a re-embed pass. A
 *       MutationObserver JS hook may call {@link #markDomMutated()} on SPAs that re-render without
 *       navigating; until that hook is wired, the flag simply stays clear (navigation-only policy).</li>
 * </ul>
 *
 * <p>Thread-safe: {@link ConcurrentHashMap} for the vector store; {@link AtomicBoolean}
 * for the mutation flag.</p>
 */
public class ElementVectorCache {

    private static final ThreadLocal<ElementVectorCache> THREAD_LOCAL =
            ThreadLocal.withInitial(ElementVectorCache::new);

    private static final int MAX_ENTRIES = 2_000;

    private final Map<String, float[]> vectors = Collections.synchronizedMap(
            new LinkedHashMap<>(1024, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                    return size() > MAX_ENTRIES;
                }
            });
    private final AtomicBoolean domMutated = new AtomicBoolean(false);

    private ElementVectorCache() {}

    public static ElementVectorCache getInstance() { return THREAD_LOCAL.get(); }

    // ──────────────────────── Cache Operations ────────────────────────

    /**
     * Returns the cached vector for the given element key, or {@code null} if absent.
     * Callers should treat a {@code null} return as a cache miss and compute the vector.
     */
    public float[] get(String elementKey) {
        if (elementKey == null || elementKey.isEmpty()) return null; // keyless element — never cached
        if (domMutated.get()) return null;                           // stale after a DOM mutation — re-embed
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
    boolean isDomMutated() { return domMutated.get(); }

    /**
     * Signals that the DOM has changed (called by the MutationObserver JS hook).
     * The next heal attempt will re-embed changed elements rather than using stale vectors.
     */
    void markDomMutated() { domMutated.set(true); }

    /** Clears the mutation flag after delta re-embedding is complete. */
    void clearMutationFlag() { domMutated.set(false); }

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
    int size() { return vectors.size(); }

    /** Returns {@code true} when the cache is empty. */
    boolean isEmpty() { return vectors.isEmpty(); }
}
