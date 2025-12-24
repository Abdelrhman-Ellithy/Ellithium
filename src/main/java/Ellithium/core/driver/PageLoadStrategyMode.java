package Ellithium.core.driver;

public enum PageLoadStrategyMode {

    Normal("Normal Page Load Strategy"),
    Eager("Eager Page Load Strategy");

            /**
        * Get the name of the page load strategy mode
         * @return the name of the page load strategy mode
         */
        public String getName() {
                return name;
        }
        private final String name ;

        /**
         * Constructor for PageLoadStrategyMode.
         * @param name The name of the page load strategy mode
         */
        PageLoadStrategyMode(String name) {
                this.name = name;
        }
}
