package AutoEllithiumSphere.properties;

import org.aeonbits.owner.Config.Sources;

@SuppressWarnings("unused")
@Sources({
        "system:properties",
        "file:src/main/resources/properties/config.properties",
        "file:src/main/resources/properties/default/log4j2.properties",
        "classpath:config.properties"
})
public interface CustomConfig extends FramepropertySetter {
    @Key("AutoEllithiumSphereVersion")
    @DefaultValue("1.0.0")
    String autoEllithiumSphereVersion();

    @Key("allureVersion")
    @DefaultValue("2.30.0")
    String allureVersion();
}
