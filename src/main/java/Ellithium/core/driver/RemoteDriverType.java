package Ellithium.core.driver;

public enum RemoteDriverType implements DriverType{
        REMOTE_Chrome("Remote Chrome"),
        REMOTE_Edge("Remote Edge"),
        REMOTE_Safari("Remote Safari"),
        REMOTE_FireFox("Remote Firefox");

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
         * @param name The browser name of the driver instance
         */
        RemoteDriverType(String name) {
                this.name = name;
        }
}
