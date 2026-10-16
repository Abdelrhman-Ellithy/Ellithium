package Ellithium.core.execution.listener;

import io.cucumber.plugin.event.*;
import org.apache.logging.log4j.ThreadContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CucumberListenerTest {

    private CucumberListener listener;

    @BeforeMethod
    public void setup() {
        listener = new CucumberListener();
        ThreadContext.clearAll();
    }

    @AfterMethod
    public void cleanup() {
        ThreadContext.clearAll();
    }

    @Test
    public void setEventPublisher_registersAllEventHandlers() {
        EventPublisher mockPublisher = mock(EventPublisher.class);
        listener.setEventPublisher(mockPublisher);

        verify(mockPublisher, atLeastOnce()).registerHandlerFor(eq(TestCaseStarted.class), any());
        verify(mockPublisher, atLeastOnce()).registerHandlerFor(eq(TestStepStarted.class), any());
        verify(mockPublisher, atLeastOnce()).registerHandlerFor(eq(TestStepFinished.class), any());
        verify(mockPublisher, atLeastOnce()).registerHandlerFor(eq(TestCaseFinished.class), any());
        verify(mockPublisher, atLeastOnce()).registerHandlerFor(eq(TestRunFinished.class), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLifecycle_startedAndFinishedEvents_manageThreadContext() {
        EventPublisher mockPublisher = mock(EventPublisher.class);
        List<io.cucumber.plugin.event.EventHandler<?>> handlers = new ArrayList<>();
        doAnswer(invocation -> {
            handlers.add(invocation.getArgument(1));
            return null;
        }).when(mockPublisher).registerHandlerFor(any(), any());

        listener.setEventPublisher(mockPublisher);

        TestCase mockTestCase = mock(TestCase.class);
        when(mockTestCase.getName()).thenReturn("User logs in successfully");
        when(mockTestCase.getUri()).thenReturn(URI.create("classpath:features/login.feature"));
        when(mockTestCase.getLine()).thenReturn(10);
        when(mockTestCase.getTestSteps()).thenReturn(Collections.emptyList());

        TestCaseStarted startEvent = new TestCaseStarted(Instant.now(), mockTestCase);
        
        // Handler index 0 is CucumberListener::testStartedHandler
        ((EventHandler<TestCaseStarted>) handlers.get(0)).receive(startEvent);

        Assert.assertEquals(ThreadContext.get("testName"), "User logs in successfully");
        Assert.assertNotNull(ThreadContext.get("testIdentifier"));

        // Handler index 3 is CucumberListener::testFinishedHandler
        Result mockResult = new Result(Status.PASSED, Duration.ofMillis(120), null);
        TestCaseFinished finishEvent = new TestCaseFinished(Instant.now(), mockTestCase, mockResult);
        ((EventHandler<TestCaseFinished>) handlers.get(3)).receive(finishEvent);

        Assert.assertEquals(ThreadContext.get("testName"), "Ellithium");
        Assert.assertNull(ThreadContext.get("testIdentifier"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunFinishedHandler_cleansUpWithoutThrowing() {
        EventPublisher mockPublisher = mock(EventPublisher.class);
        List<io.cucumber.plugin.event.EventHandler<?>> handlers = new ArrayList<>();
        doAnswer(invocation -> {
            handlers.add(invocation.getArgument(1));
            return null;
        }).when(mockPublisher).registerHandlerFor(any(), any());

        listener.setEventPublisher(mockPublisher);

        TestRunFinished runFinished = new TestRunFinished(Instant.now(), new Result(Status.PASSED, Duration.ofSeconds(1), null));

        // Handler index 4 is CucumberListener::testRunFinishedHandler
        ((EventHandler<TestRunFinished>) handlers.get(4)).receive(runFinished);
    }
}
