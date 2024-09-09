package AutoEllithiumSphere.properties;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.HotReload;
import org.aeonbits.owner.Config.HotReloadType;
import org.aeonbits.owner.Config.LoadPolicy;

@HotReload(type = HotReloadType.ASYNC)
@LoadPolicy(Config.LoadType.MERGE)
public interface FramepropertySetter<T> extends Config {
    SetProperty set();
    interface SetProperty {
    }
}