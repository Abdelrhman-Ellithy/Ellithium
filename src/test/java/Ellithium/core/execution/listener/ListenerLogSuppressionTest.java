package Ellithium.core.execution.listener;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class ListenerLogSuppressionTest {

    @AfterMethod
    public void resetSuppression() {
        while (ListenerLogSuppression.isSuppressed()) {
            ListenerLogSuppression.resume();
        }
    }

    @Test
    public void initialState_isNotSuppressed() {
        Assert.assertFalse(ListenerLogSuppression.isSuppressed());
    }

    @Test
    public void singleSuppress_thenSuppressed() {
        ListenerLogSuppression.suppress();
        Assert.assertTrue(ListenerLogSuppression.isSuppressed());
    }

    @Test
    public void suppressThenResume_notSuppressed() {
        ListenerLogSuppression.suppress();
        ListenerLogSuppression.resume();
        Assert.assertFalse(ListenerLogSuppression.isSuppressed());
    }

    @Test
    public void reentrantSuppress_requiresMatchingResumes() {
        ListenerLogSuppression.suppress();
        ListenerLogSuppression.suppress();
        ListenerLogSuppression.suppress();

        ListenerLogSuppression.resume();
        Assert.assertTrue(ListenerLogSuppression.isSuppressed());

        ListenerLogSuppression.resume();
        Assert.assertTrue(ListenerLogSuppression.isSuppressed());

        ListenerLogSuppression.resume();
        Assert.assertFalse(ListenerLogSuppression.isSuppressed());
    }

    @Test
    public void resume_withoutSuppress_doesNotGoNegative() {
        ListenerLogSuppression.resume();
        Assert.assertFalse(ListenerLogSuppression.isSuppressed());
        // A subsequent suppress+resume should still work cleanly
        ListenerLogSuppression.suppress();
        Assert.assertTrue(ListenerLogSuppression.isSuppressed());
        ListenerLogSuppression.resume();
        Assert.assertFalse(ListenerLogSuppression.isSuppressed());
    }

    @Test
    public void suppressionIsThreadLocal_otherThreadSeesClean() throws InterruptedException {
        ListenerLogSuppression.suppress();
        Assert.assertTrue(ListenerLogSuppression.isSuppressed());

        AtomicBoolean otherSuppressed = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(1);

        Thread t = new Thread(() -> {
            otherSuppressed.set(ListenerLogSuppression.isSuppressed());
            latch.countDown();
        });
        t.start();
        latch.await();

        Assert.assertFalse(otherSuppressed.get(), "Other thread must start unsuppressed");
    }

    @Test
    public void eachThreadManagesOwnDepth() throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean threadResult = new AtomicBoolean(false);

        Thread t = new Thread(() -> {
            ListenerLogSuppression.suppress();
            ListenerLogSuppression.suppress();
            ListenerLogSuppression.resume();
            threadResult.set(ListenerLogSuppression.isSuppressed());
            ready.countDown();
            try { done.await(); } catch (InterruptedException ignored) {}
        });
        t.start();
        ready.await();

        Assert.assertTrue(threadResult.get(), "Thread's own depth=1 must still be suppressed");
        Assert.assertFalse(ListenerLogSuppression.isSuppressed(), "Main thread must be unaffected");

        done.countDown();
        t.join();
    }

    @Test
    public void tryCatchPattern_alwaysResumes() {
        ListenerLogSuppression.suppress();
        try {
            Assert.assertTrue(ListenerLogSuppression.isSuppressed());
            throw new RuntimeException("simulated");
        } catch (RuntimeException ignored) {
        } finally {
            ListenerLogSuppression.resume();
        }
        Assert.assertFalse(ListenerLogSuppression.isSuppressed());
    }
}
