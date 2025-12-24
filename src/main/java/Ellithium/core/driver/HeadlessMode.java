package Ellithium.core.driver;

public enum HeadlessMode {
    False("Headless Mode Disabled"),
    True("Headless Mode Enabled");
    private final String name ;

    /**
     * Get the name of the headless mode
     * @return
     */
    public String getName() {
        return name;
    }
    /**
     * Constructor for PrivateMode.
    * @param name The name of the private mode
    */
    HeadlessMode(String name) {
        this.name = name;
    }
}
