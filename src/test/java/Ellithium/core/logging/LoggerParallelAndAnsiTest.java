package Ellithium.core.logging;

import Ellithium.core.reporting.internal.Colors;
import org.apache.logging.log4j.ThreadContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.concurrent.*;

public class LoggerParallelAndAnsiTest {

    @AfterMethod
    public void cleanup() {
        ThreadContext.clearAll();
        Logger.clearCurrentExecutionLogs();
    }

    @Test
    public void testStripAnsiRemovesAllColorCodes() {
        String input = Colors.GREEN + "Encoded 1/1 frames at 3 FPS" + Colors.RESET;
        String clean = Logger.stripAnsi(input);
        Assert.assertEquals(clean, "Encoded 1/1 frames at 3 FPS");
        Assert.assertFalse(clean.contains("\u001B"));
        Assert.assertFalse(clean.contains("[92m"));
        Assert.assertFalse(clean.contains("[0m"));

        String input2 = Colors.BLUE + "Stopped video recording for: testFoo" + Colors.RESET;
        String clean2 = Logger.stripAnsi(input2);
        Assert.assertEquals(clean2, "Stopped video recording for: testFoo");
        Assert.assertFalse(clean2.contains("[94m"));
    }

    @Test
    public void testParallelLogIsolationBetweenTests() throws Exception {
        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                String testId = "testMethod_" + index;
                ThreadContext.put("testName", testId);
                try {
                    Logger.info("Message for " + testId + " line 1");
                    Logger.info(Colors.YELLOW + "Message for " + testId + " line 2" + Colors.RESET);

                    String logs = Logger.getLogsForTest(testId);
                    Assert.assertTrue(logs.contains("Message for " + testId + " line 1"));
                    Assert.assertTrue(logs.contains("Message for " + testId + " line 2"));
                    Assert.assertFalse(logs.contains("[93m"));

                    // Verify no logs from other tests leaked into this test's buffer
                    for (int j = 0; j < threads; j++) {
                        if (j != index) {
                            Assert.assertFalse(logs.contains("testMethod_" + j),
                                    "Log leakage: found testMethod_" + j + " in " + testId);
                        }
                    }
                } finally {
                    Logger.clearLogsForTest(testId);
                    ThreadContext.remove("testName");
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        Assert.assertTrue(finished, "Parallel logging test should finish in time");
    }

    @Test
    public void testLogsClearedAfterTestFinishes() {
        String testId = "testClearance";
        ThreadContext.put("testName", testId);

        Logger.info("Log line 1");
        String logs = Logger.getLogsForTest(testId);
        Assert.assertTrue(logs.contains("Log line 1"));

        Logger.clearLogsForTest(testId);
        ThreadContext.remove("testName");

        // Subsequent check should be empty or not contain old line
        String afterClear = Logger.getLogsForTest(testId);
        Assert.assertFalse(afterClear.contains("Log line 1"));
    }

    @Test
    public void testLoggerNameMatchesCurrentTestName() {
        String testId = "myAwesomeTest";
        ThreadContext.put("testName", testId);
        try {
            Logger.info("Testing dynamic logger name resolution");
            String logs = Logger.getLogsForTest(testId);
            Assert.assertTrue(logs.contains("Testing dynamic logger name resolution"));
            Assert.assertEquals(Logger.getCurrentTestName(), testId);
        } finally {
            Logger.clearLogsForTest(testId);
            ThreadContext.remove("testName");
        }
    }

    @Test
    public void testConfigurationMethodAttribution() {
        String configName = "FuelTypeTests.setup";
        ThreadContext.put("testName", configName);
        try {
            Logger.info("Entering setup in BeforeClass");
            String logs = Logger.getLogsForTest(configName);
            Assert.assertTrue(logs.contains("Entering setup in BeforeClass"));
            Assert.assertEquals(Logger.getCurrentTestName(), configName);
        } finally {
            Logger.clearLogsForTest(configName);
            ThreadContext.remove("testName");
        }
    }
}
