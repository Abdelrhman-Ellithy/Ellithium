package Ellithium.com;

import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.TestStepStarted;
import io.cucumber.plugin.event.HookTestStep;  // Import HookTestStep to identify hooks
import io.qameta.allure.Allure;
import io.qameta.allure.model.StepResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public class AllureStepListener implements EventListener {

    private Map<Integer, String> stepUUIDMap = new HashMap<>();
    private int stepIndex = 0;
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestStepStarted.class, this::handleTestStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::handleTestStepFinished);
    }
    private void handleTestStepStarted(TestStepStarted event) {
        // Ensure we're only tracking steps in the test body, ignoring setup/teardown steps
        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep pickleStep = (PickleStepTestStep) event.getTestStep();
            String stepText = pickleStep.getStep().getText();

            // Generate a unique UUID for the step and store it
            String uuid = UUID.randomUUID().toString();
            stepUUIDMap.put(stepIndex, uuid);

            StepResult result = new StepResult().setName(stepText).setStatus(io.qameta.allure.model.Status.PASSED);
            Allure.getLifecycle().startStep(uuid, result);

            stepIndex++;
        }
    }
    private void handleTestStepFinished(TestStepFinished event) {
        // Convert Cucumber's status to Allure's status
        Status stepStatus = event.getResult().getStatus();
        io.qameta.allure.model.Status allureStatus;

        switch (stepStatus) {
            case FAILED:
                allureStatus = io.qameta.allure.model.Status.FAILED;
                break;
            case SKIPPED:
                allureStatus = io.qameta.allure.model.Status.SKIPPED;
                break;
            default:
                allureStatus = io.qameta.allure.model.Status.PASSED;
                break;
        }
        stepIndex--;
        if (event.getTestStep() instanceof PickleStepTestStep) {
            String uuid = stepUUIDMap.get(stepIndex);

            if (uuid != null) {
                Allure.getLifecycle().updateStep(uuid, stepResult -> stepResult.setStatus(allureStatus));
                Allure.getLifecycle().stopStep(uuid);
                stepUUIDMap.remove(stepIndex);
            }
        }
    }
}