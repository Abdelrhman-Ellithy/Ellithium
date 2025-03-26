//package UI_NonBDD;
//import net.lightbody.bmp.BrowserMobProxy;
//import net.lightbody.bmp.BrowserMobProxyServer;
//import net.lightbody.bmp.client.ClientUtil;
//import net.lightbody.bmp.core.har.Har;
//import org.openqa.selenium.Proxy;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.firefox.FirefoxDriver;
//import org.openqa.selenium.firefox.FirefoxOptions;
//import org.openqa.selenium.firefox.FirefoxProfile;
//import org.testng.annotations.Test;
//
//public class TestDevTools {
//    public void test(){
//        BrowserMobProxy proxy = new BrowserMobProxyServer();
//        proxy.start(0);
//
//        // Get the Selenium proxy object
//        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
//
//        // Configure Firefox profile to use the proxy and accept untrusted certificates
//        FirefoxProfile profile = new FirefoxProfile();
//        profile.setAcceptUntrustedCertificates(true);
//        profile.setAssumeUntrustedCertificateIssuer(false);
//
//        // Set up Firefox options with the proxy settings
//        FirefoxOptions options = new FirefoxOptions();
//        options.setProfile(profile);
//        options.setCapability("proxy", seleniumProxy);
//
//        // Create Firefox driver instance
//        WebDriver driver = new FirefoxDriver(options);
//
//        // Start capturing the network traffic
//        proxy.newHar("testSite");
//
//        // Navigate to a target URL
//        driver.get("https://www.example.com");
//
//        try {
//            Thread.sleep(5000);
//        }catch (Exception hamada){
//        }
//
//        // Retrieve the HAR data (HTTP Archive)
//        Har har = proxy.getHar();
//        // Optionally, write the HAR to a file for further analysis
//        har.writeTo(new File("network_traffic.har"));
//
//        // Clean up
//        driver.quit();
//        proxy.stop();
//    }
//}
