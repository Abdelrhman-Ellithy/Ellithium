package Ellithium.core.driver;

public enum WebSecurityMode {
    AllowUnsecure("Allow Unsecure Content"),
    SecureMode("Secure Mode");
            /**
        * Get the name of the web security mode
         * @return
         */
        public String getName() {
                return name;
        }
        private final String name ;

        /**
         * Constructor for WebSecurityMode.
         * @param name The name of the web security mode
         */
        WebSecurityMode(String name) {
                this.name = name;
        }
}
