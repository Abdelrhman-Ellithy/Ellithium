package Ellithium.core.ai.codegen;

import org.mockito.ArgumentCaptor;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

public class StorageManagerTest {

    @Test
    public void save_writesJsonFileContainingCookieNameAndValue() throws Exception {
        Path tmp = Files.createTempFile("sm-save", ".json");
        tmp.toFile().deleteOnExit();

        WebDriver mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        JavascriptExecutor mockJs = (JavascriptExecutor) mockDriver;
        WebDriver.Options mockOptions = mock(WebDriver.Options.class);
        Cookie cookie = new Cookie("session", "tok-abc");

        when(mockDriver.manage()).thenReturn(mockOptions);
        when(mockOptions.getCookies()).thenReturn(Set.of(cookie));
        when(mockJs.executeScript(anyString())).thenReturn(Map.of());

        StorageManager.save(mockDriver, tmp.toString());

        String content = Files.readString(tmp);
        Assert.assertTrue(content.contains("tok-abc"), "Saved JSON must contain cookie value");
        Assert.assertTrue(content.contains("session"), "Saved JSON must contain cookie name");
    }

    @Test
    public void save_succeedsWithNonJsDriver() throws Exception {
        Path tmp = Files.createTempFile("sm-save-nojs", ".json");
        tmp.toFile().deleteOnExit();

        WebDriver mockDriver = mock(WebDriver.class);
        WebDriver.Options mockOptions = mock(WebDriver.Options.class);

        when(mockDriver.manage()).thenReturn(mockOptions);
        when(mockOptions.getCookies()).thenReturn(Set.of());

        StorageManager.save(mockDriver, tmp.toString());

        String content = Files.readString(tmp);
        Assert.assertTrue(content.contains("cookies"), "Saved JSON must include cookies field");
    }

    @Test
    public void load_doesNotThrow_whenFileNotFound() {
        WebDriver mockDriver = mock(WebDriver.class);
        StorageManager.load(mockDriver, "/no/such/path/storage.json");
    }

    @Test
    public void load_doesNotThrow_whenJsonIsMalformed() throws Exception {
        Path tmp = Files.createTempFile("sm-malformed", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, "{ this is not valid JSON }}}");
        WebDriver mockDriver = mock(WebDriver.class);
        StorageManager.load(mockDriver, tmp.toString());
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void load_appliesCookieFromJsonFile() throws Exception {
        Path tmp = Files.createTempFile("sm-load", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp,
                "{\"cookies\":[{\"name\":\"token\",\"value\":\"secret99\",\"domain\":null," +
                "\"path\":\"/\",\"expiryEpoch\":null,\"secure\":false,\"httpOnly\":false,\"sameSite\":null}]," +
                "\"localStorage\":{}}");

        WebDriver mockDriver = mock(WebDriver.class);
        WebDriver.Options mockOptions = mock(WebDriver.Options.class);
        WebDriver.Navigation mockNav = mock(WebDriver.Navigation.class);

        when(mockDriver.manage()).thenReturn(mockOptions);
        when(mockDriver.getCurrentUrl()).thenReturn("https://example.com");
        when(mockDriver.navigate()).thenReturn(mockNav);

        StorageManager.load(mockDriver, tmp.toString());

        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(mockOptions).addCookie(captor.capture());
        Assert.assertEquals(captor.getValue().getName(), "token");
        Assert.assertEquals(captor.getValue().getValue(), "secret99");
    }

    @Test
    public void load_skipsAddCookie_whenCookieListEmpty() throws Exception {
        Path tmp = Files.createTempFile("sm-load-empty", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, "{\"cookies\":[],\"localStorage\":{}}");

        WebDriver mockDriver = mock(WebDriver.class);
        WebDriver.Options mockOptions = mock(WebDriver.Options.class);
        WebDriver.Navigation mockNav = mock(WebDriver.Navigation.class);

        when(mockDriver.manage()).thenReturn(mockOptions);
        when(mockDriver.getCurrentUrl()).thenReturn("https://example.com");
        when(mockDriver.navigate()).thenReturn(mockNav);

        StorageManager.load(mockDriver, tmp.toString());

        verifyNoInteractions(mockOptions);
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void save_thenLoad_roundTrips_cookieValue() throws Exception {
        Path tmp = Files.createTempFile("sm-roundtrip", ".json");
        tmp.toFile().deleteOnExit();

        WebDriver saveDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        JavascriptExecutor saveJs = (JavascriptExecutor) saveDriver;
        WebDriver.Options saveOptions = mock(WebDriver.Options.class);
        Cookie original = new Cookie("rt", "round-trip-val");

        when(saveDriver.manage()).thenReturn(saveOptions);
        when(saveOptions.getCookies()).thenReturn(Set.of(original));
        when(saveJs.executeScript(anyString())).thenReturn(Map.of());

        StorageManager.save(saveDriver, tmp.toString());

        WebDriver loadDriver = mock(WebDriver.class);
        WebDriver.Options loadOptions = mock(WebDriver.Options.class);
        WebDriver.Navigation loadNav = mock(WebDriver.Navigation.class);

        when(loadDriver.manage()).thenReturn(loadOptions);
        when(loadDriver.getCurrentUrl()).thenReturn("https://example.com");
        when(loadDriver.navigate()).thenReturn(loadNav);

        StorageManager.load(loadDriver, tmp.toString());

        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(loadOptions).addCookie(captor.capture());
        Assert.assertEquals(captor.getValue().getName(), "rt");
        Assert.assertEquals(captor.getValue().getValue(), "round-trip-val");
    }
}
