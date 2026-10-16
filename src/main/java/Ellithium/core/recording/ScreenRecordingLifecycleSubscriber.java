package Ellithium.core.recording;

import Ellithium.core.execution.events.TestLifecycleEvent;
import Ellithium.core.execution.events.TestLifecycleSubscriber;
import Ellithium.core.logging.Logger;
import Ellithium.core.recording.internal.VideoRecordingManager;

/**
 * Reactive subscriber that manages screen recording and initiates video compilation
 * based on test lifecycle events, fully decoupled from test runner listeners.
 */
public class ScreenRecordingLifecycleSubscriber implements TestLifecycleSubscriber {

    @Override
    public void onTestStarted(TestLifecycleEvent.TestStarted event) {
        if (event == null || event.testIdentifier() == null) return;
        VideoRecordingManager.prepareRecording(event.testName(), event.testIdentifier());
    }

    @Override
    public void onTestPassed(TestLifecycleEvent.TestPassed event) {
        if (event == null || event.testIdentifier() == null) return;
        stopRecording(event.testIdentifier(), "PASSED");
    }

    @Override
    public void onTestFailed(TestLifecycleEvent.TestFailed event) {
        if (event == null || event.testIdentifier() == null) return;
        stopRecording(event.testIdentifier(), "FAILED");
    }

    @Override
    public void onTestSkipped(TestLifecycleEvent.TestSkipped event) {
        if (event == null || event.testIdentifier() == null) return;
        stopRecording(event.testIdentifier(), "SKIPPED");
    }

    @Override
    public void onSuiteFinished(TestLifecycleEvent.SuiteFinished event) {
        cleanupAll();
    }

    @Override
    public void onExecutionFinished(TestLifecycleEvent.ExecutionFinished event) {
        cleanupAll();
    }

    private void stopRecording(String testIdentifier, String status) {
        try {
            VideoRecordingManager.stopRecordingForTest(testIdentifier, status);
        } catch (Throwable t) {
            Logger.warn("ScreenRecordingLifecycleSubscriber: error stopping recording for [" + testIdentifier + "]: " + t.getMessage());
        }
    }

    private void cleanupAll() {
        try {
            VideoRecordingManager.forceCleanupAll();
        } catch (Throwable t) {
            Logger.warn("ScreenRecordingLifecycleSubscriber: error during force cleanup: " + t.getMessage());
        }
    }
}
