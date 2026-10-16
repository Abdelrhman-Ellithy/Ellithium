package Ellithium.core.recording.internal;

import Ellithium.core.driver.*;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class VideoRecordingManagerLifecycleTest {

    @AfterMethod
    public void cleanup() {
        VideoRecordingManager.forceCleanupAll();
    }

    @Test
    public void testPrepareRecordingRegistersPending() {
        String testName = "testSample";
        String testIdentifier = "SampleClass.testSample";

        String recordingId = VideoRecordingManager.prepareRecording(testName, testIdentifier);
        // If driver is not active on this thread, recordingId is null and pending is set
        if (DriverFactory.getCurrentDriver() == null) {
            Assert.assertNull(recordingId);
            Assert.assertFalse(VideoRecordingManager.isRecordingActiveForTest(testIdentifier));
        }
    }

    @Test
    public void testForceCleanupClearsState() {
        VideoRecordingManager.forceCleanupAll();
        Assert.assertFalse(VideoRecordingManager.isRecordingActive());
    }

    @Test
    public void testAwaitPendingCompilationsWhenZero() {
        Assert.assertEquals(Ellithium.Utilities.interactions.ScreenRecorderActions.pendingCompilations(), 0);
        boolean completed = Ellithium.Utilities.interactions.ScreenRecorderActions.awaitPendingCompilations(
                1, java.util.concurrent.TimeUnit.SECONDS);
        Assert.assertTrue(completed);
    }

    @Test
    public void testConfigurationDefaults() {
        Assert.assertTrue(VideoRecordingManager.isRecordingEnabled());
        Assert.assertTrue(VideoRecordingManager.isAttachmentEnabled());
        Assert.assertFalse(VideoRecordingManager.isAttachmentOnFailureOnlyEnabled());
    }
}
