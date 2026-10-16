package Ellithium.core.ai.context;

import org.openqa.selenium.By;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;

public class HealingContextTest {

    @Test
    public void testCurrent_WhenUnbound_ReturnsNull() {
        assertFalse(HealingContext.isBound(), "Should not be bound initially");
        assertNull(HealingContext.current(), "Current context should be null when unbound");
        assertTrue(HealingContext.currentOptional().isEmpty());
        assertFalse(HealingContext.isCurrentExpired());
        assertEquals(HealingContext.currentRemainingMillis(), Long.MAX_VALUE);
    }

    @Test
    public void testRunWith_BindsContextAndCleansUp() {
        HealingContext ctx = HealingContext.of("test-id-1", "testLogin", By.id("btn"), "CLICK", 5000, Map.of("key", "val"));

        AtomicBoolean executed = new AtomicBoolean(false);
        HealingContext.runWith(ctx, () -> {
            executed.set(true);
            assertTrue(HealingContext.isBound());
            assertEquals(HealingContext.current(), ctx);
            assertEquals(HealingContext.current().testIdentifier(), "test-id-1");
            assertEquals(HealingContext.current().actionType(), "CLICK");
            assertEquals(HealingContext.current().metadata().get("key"), "val");
            assertFalse(HealingContext.isCurrentExpired());
            assertTrue(HealingContext.currentRemainingMillis() > 0);
        });

        assertTrue(executed.get());
        assertFalse(HealingContext.isBound(), "Should cleanly unbind after execution block");
        assertNull(HealingContext.current());
    }

    @Test
    public void testGetWith_ReturnsValueAndCleansUp() {
        HealingContext ctx = HealingContext.of("test-id-2", "testSearch", By.name("q"), "TYPE", 10000, Collections.emptyMap());

        String result = HealingContext.getWith(ctx, () -> {
            assertEquals(HealingContext.current(), ctx);
            return "healed-" + HealingContext.current().actionType();
        });

        assertEquals(result, "healed-TYPE");
        assertFalse(HealingContext.isBound());
    }

    @Test
    public void testCallWith_PropagatesCheckedException() {
        HealingContext ctx = HealingContext.of("test-id-3", "testFail", By.xpath("//div"), "READ", 2000, Collections.emptyMap());

        assertThrows(IOException.class, () -> HealingContext.callWith(ctx, () -> {
            throw new IOException("Simulated I/O failure");
        }));

        assertFalse(HealingContext.isBound());
    }

    @Test
    public void testDeadlineExpiration() {
        // Expired context (past deadline)
        HealingContext expiredCtx = new HealingContext("id", "name", By.id("x"), "CLICK", System.currentTimeMillis() - 500, Collections.emptyMap());
        assertTrue(expiredCtx.isExpired());
        assertEquals(expiredCtx.remainingMillis(), 0L);

        HealingContext.runWith(expiredCtx, () -> {
            assertTrue(HealingContext.isCurrentExpired());
            assertEquals(HealingContext.currentRemainingMillis(), 0L);
        });

        // Unbounded context (deadlineMillis = 0)
        HealingContext unbounded = new HealingContext("id", "name", By.id("x"), "CLICK", 0L, Collections.emptyMap());
        assertFalse(unbounded.isExpired());
        assertEquals(unbounded.remainingMillis(), Long.MAX_VALUE);
    }

    @Test
    public void testNestedScoping_RestoresOuterContext() {
        HealingContext outerCtx = HealingContext.of("outer-id", "outerTest", By.id("outer"), "CLICK", 8000, Map.of("level", "outer"));
        HealingContext innerCtx = HealingContext.of("inner-id", "innerTest", By.id("inner"), "TYPE", 4000, Map.of("level", "inner"));

        HealingContext.runWith(outerCtx, () -> {
            assertEquals(HealingContext.current(), outerCtx);
            assertEquals(HealingContext.current().testIdentifier(), "outer-id");

            HealingContext.runWith(innerCtx, () -> {
                assertEquals(HealingContext.current(), innerCtx);
                assertEquals(HealingContext.current().testIdentifier(), "inner-id");
            });

            // Outer context restored after inner scope completes
            assertEquals(HealingContext.current(), outerCtx);
            assertEquals(HealingContext.current().testIdentifier(), "outer-id");
        });

        assertFalse(HealingContext.isBound());
        assertNull(HealingContext.current());
    }

    @Test
    public void testMetadataImmutability() {
        Map<String, Object> mutableMap = new HashMap<>();
        mutableMap.put("a", 1);
        HealingContext ctx = HealingContext.of("id", "name", By.id("x"), "CLICK", 5000, mutableMap);

        assertThrows(UnsupportedOperationException.class, () -> ctx.metadata().put("b", 2));
    }
}
