package AutoEllithiumSphere.properties;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.ConfigFactory;

@SuppressWarnings("unused")
@Sources({
        "system:properties",
        "file:src/main/resources/properties/allure.properties",
        "file:src/main/resources/properties/default/allure.properties",
        "classpath:allure.properties"
})
public interface Allure extends FramepropertySetter<Allure> {

    @Config.Key("allure.results.directory")
    @Config.DefaultValue("Test-Output/Reports/Allure/allure-results")
    String definedPath();

    @Config.Key("allure.open.afterExecution")
    @Config.DefaultValue("true")
    boolean openAfterExecution();

    default SetProperty set() {
        return new SetProperty();
    }

    class SetProperty implements FramepropertySetter.SetProperty {

        private void updateSystemProperty(String key, String value) {
            System.setProperty(key, value);
        }

        public SetProperty openAfterExecution(boolean value) {
            updateSystemProperty("allure.open.afterExecution", String.valueOf(value));
            return this;
        }

        public SetProperty setResultsDirectory(String path) {
            updateSystemProperty("allure.results.directory", path);
            return this;
        }
    }
}