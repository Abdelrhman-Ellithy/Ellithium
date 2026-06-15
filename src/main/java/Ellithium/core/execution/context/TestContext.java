package Ellithium.core.execution.context;

public final class TestContext {

    public static final ScopedValue<TestContextData> CURRENT = ScopedValue.newInstance();

    private TestContext() {
    }

    public static String testId() {
        return CURRENT.isBound() ? CURRENT.get().testId() : null;
    }

    public static TestContextData current() {
        return CURRENT.isBound() ? CURRENT.get() : null;
    }
}
