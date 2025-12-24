package Ellithium.core.driver;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;

import java.net.URL;

public class DriverConfiguration {
    private DriverType driverType;
    private HeadlessMode headlessMode;

    public DriverConfiguration(DriverType driverType, HeadlessMode headlessMode, Capabilities capabilities, boolean isMobileCloud) {
        this.driverType = driverType;
        this.headlessMode = headlessMode;
        this.capabilities = (capabilities != null) ? capabilities : new MutableCapabilities();
        this.isMobileCloud = isMobileCloud;
    }

    private PageLoadStrategyMode pageLoadStrategy;
    private PrivateMode privateMode;
    private SandboxMode sandboxMode;
    private WebSecurityMode webSecurityMode;
    private URL remoteAddress;
    private Capabilities capabilities;
    private boolean isMobileCloud;

    public DriverConfiguration(DriverType driverType, HeadlessMode headlessMode, PageLoadStrategyMode pageLoadStrategy, PrivateMode privateMode, SandboxMode sandboxMode, WebSecurityMode webSecurityMode, Capabilities capabilities, boolean isMobileCloud) {
        this.driverType = driverType;
        this.headlessMode = headlessMode;
        this.pageLoadStrategy = pageLoadStrategy;
        this.privateMode = privateMode;
        this.sandboxMode = sandboxMode;
        this.webSecurityMode = webSecurityMode;
        this.capabilities = (capabilities != null) ? capabilities : new MutableCapabilities();
        this.isMobileCloud = isMobileCloud;
        this.remoteAddress=null;
    }
    public DriverType getDriverType() {
        return driverType;
    }

    public void setDriverType(DriverType driverType) {
        this.driverType = driverType;
    }

    public HeadlessMode getHeadlessMode() {
        return headlessMode;
    }

    public void setHeadlessMode(HeadlessMode headlessMode) {
        this.headlessMode = headlessMode;
    }

    public PageLoadStrategyMode getPageLoadStrategy() {
        return pageLoadStrategy;
    }

    public void setPageLoadStrategy(PageLoadStrategyMode pageLoadStrategy) {
        this.pageLoadStrategy = pageLoadStrategy;
    }

    public PrivateMode getPrivateMode() {
        return privateMode;
    }

    public void setPrivateMode(PrivateMode privateMode) {
        this.privateMode = privateMode;
    }

    public SandboxMode getSandboxMode() {
        return sandboxMode;
    }

    public void setSandboxMode(SandboxMode sandboxMode) {
        this.sandboxMode = sandboxMode;
    }

    public WebSecurityMode getWebSecurityMode() {
        return webSecurityMode;
    }

    public void setWebSecurityMode(WebSecurityMode webSecurityMode) {
        this.webSecurityMode = webSecurityMode;
    }

    public URL getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(URL remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    public boolean isMobileCloud() {
        return isMobileCloud;
    }

    public void setMobileCloud(boolean mobileCloud) {
        isMobileCloud = mobileCloud;
    }
}