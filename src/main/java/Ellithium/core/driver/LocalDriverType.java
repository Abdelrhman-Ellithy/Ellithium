package Ellithium.core.driver;

public enum LocalDriverType implements DriverType {
        Chrome("Chrome"),
        Edge("Edge"),
        Safari("Safari"),
        FireFox("Firefox");
        
        /**
        * Get the name of the browser for the driver instance
         * @return
         */
        public String getName() {
                return name;
        }

        private final String name ;

        /**
         * Constructor for MobileDriverType.
         *
         * @param name The browser name of the driver instance
         */
        LocalDriverType(String name) {
                this.name = name;
        }
}
