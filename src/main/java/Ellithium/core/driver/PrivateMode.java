package Ellithium.core.driver;

public enum PrivateMode {
    True("Private/Incognito Mode Enabled"),
    False("Private/Incognito Mode Disabled");

        /**
        * Get the name of the private mode 
         * @return
         */
        public String getName() {
                return name;
        }
        private final String name ;

        /**
         * Constructor for PrivateMode.
         * @param name The name of the private mode
         */
        PrivateMode(String name) {
                this.name = name;
        }
}
