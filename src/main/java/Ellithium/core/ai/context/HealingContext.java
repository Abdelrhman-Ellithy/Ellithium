package Ellithium.core.ai.context;

import Ellithium.core.ai.models.HealingRequest;
import org.openqa.selenium.By;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Java 25 ScopedValue context carrying immutable AI healing metadata, action classification,
 * and execution deadlines.
 *
 * <p>Propagates automatically to child virtual threads within StructuredTaskScope or structured
 * concurrency blocks without carrier-thread leaks or ThreadLocal overhead.
 */
public record HealingContext(
        String testIdentifier,
        String testName,
        By brokenLocator,
        String actionType,
        long deadlineMillis,
        Map<String, Object> metadata
) {

    public static final ScopedValue<HealingContext> CURRENT = ScopedValue.newInstance();

    public HealingContext {
        if (metadata == null) {
            metadata = Collections.emptyMap();
        } else {
            metadata = Collections.unmodifiableMap(metadata);
        }
    }

    /**
     * Factory creating a HealingContext from a {@link HealingRequest} and timeout duration in milliseconds.
     */
    public static HealingContext of(HealingRequest request, long timeoutMillis) {
        Objects.requireNonNull(request, "request must not be null");
        long deadline = timeoutMillis > 0 ? System.currentTimeMillis() + timeoutMillis : 0L;
        return new HealingContext(
                null,
                null,
                request.brokenLocator(),
                request.actionType(),
                deadline,
                Collections.emptyMap()
        );
    }

    /**
     * Factory creating a full HealingContext with test identification and metadata.
     */
    public static HealingContext of(String testIdentifier, String testName, By brokenLocator,
                                    String actionType, long timeoutMillis, Map<String, Object> metadata) {
        long deadline = timeoutMillis > 0 ? System.currentTimeMillis() + timeoutMillis : 0L;
        return new HealingContext(testIdentifier, testName, brokenLocator, actionType, deadline, metadata);
    }

    /**
     * Whether a HealingContext is currently bound to the executing thread or scope.
     */
    public static boolean isBound() {
        return CURRENT.isBound();
    }

    /**
     * Returns the currently bound HealingContext, or null if none is bound.
     */
    public static HealingContext current() {
        return CURRENT.isBound() ? CURRENT.get() : null;
    }

    /**
     * Returns an Optional containing the currently bound HealingContext.
     */
    public static Optional<HealingContext> currentOptional() {
        return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
    }

    /**
     * Checks if the currently bound context has passed its execution deadline.
     * Returns false if no context is bound or no deadline is set.
     */
    public static boolean isCurrentExpired() {
        HealingContext ctx = current();
        return ctx != null && ctx.isExpired();
    }

    /**
     * Returns the remaining milliseconds before the deadline for the current context.
     */
    public static long currentRemainingMillis() {
        HealingContext ctx = current();
        return ctx != null ? ctx.remainingMillis() : Long.MAX_VALUE;
    }

    /**
     * Whether this context has exceeded its deadline.
     */
    public boolean isExpired() {
        return deadlineMillis > 0 && System.currentTimeMillis() >= deadlineMillis;
    }

    /**
     * Milliseconds remaining until deadline.
     */
    public long remainingMillis() {
        return deadlineMillis > 0 ? Math.max(0, deadlineMillis - System.currentTimeMillis()) : Long.MAX_VALUE;
    }

    /**
     * Runs a task within this ScopedValue context.
     */
    public static void runWith(HealingContext context, Runnable task) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(task, "task must not be null");
        ScopedValue.where(CURRENT, context).run(task);
    }

    /**
     * Computes a result within this ScopedValue context.
     */
    public static <T> T getWith(HealingContext context, Supplier<T> task) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(task, "task must not be null");
        try {
            return ScopedValue.where(CURRENT, context).call(task::get);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calls a value-returning task that may throw a checked exception within this ScopedValue context.
     */
    public static <T> T callWith(HealingContext context, Callable<T> task) throws Exception {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(task, "task must not be null");
        return ScopedValue.where(CURRENT, context).call(task::call);
    }
}
