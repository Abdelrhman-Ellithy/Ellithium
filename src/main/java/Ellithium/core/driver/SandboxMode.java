package Ellithium.core.driver;

public enum SandboxMode {
    NoSandboxMode ("No Sandbox Mode"),
    Sandbox("Sandbox Mode Enabled");
    /**
     * Get the name of the sandbox mode
     * @return
     */
    public String getName() {
        return name;
    }
    private final String name ;

    /**
     * Constructor for SandboxMode.
     * @param name The name of the sandbox mode
     */
    SandboxMode(String name) {
        this.name = name;
    }
}
