package Ellithium.core.ai.codegen;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StorageManager {

    private StorageManager() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class CookieDto {
        String name, value, domain, path, sameSite;
        Long expiryEpoch;
        boolean secure, httpOnly;

        CookieDto() {}

        CookieDto(String name, String value, String domain, String path,
                  Long expiryEpoch, boolean secure, boolean httpOnly, String sameSite) {
            this.name = name; this.value = value; this.domain = domain; this.path = path;
            this.expiryEpoch = expiryEpoch; this.secure = secure; this.httpOnly = httpOnly; this.sameSite = sameSite;
        }
    }

    public static final class StorageState {
        List<CookieDto> cookies;
        Map<String, String> localStorage;

        StorageState() {}

        StorageState(List<CookieDto> cookies, Map<String, String> localStorage) {
            this.cookies = cookies;
            this.localStorage = localStorage;
        }
    }

    public static void save(WebDriver driver, String filePath) {
        try {
            List<CookieDto> cookies = new ArrayList<>();
            for (Cookie c : driver.manage().getCookies()) {
                cookies.add(new CookieDto(c.getName(), c.getValue(), c.getDomain(), c.getPath(),
                        c.getExpiry() != null ? c.getExpiry().getTime() : null,
                        c.isSecure(), c.isHttpOnly(), c.getSameSite()));
            }
            Map<String, String> ls = readLocalStorage(driver);
            Path target = Paths.get(filePath);
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            Path tmp = Files.createTempFile(
                    target.getParent() != null ? target.getParent() : Paths.get("."), "storage", ".tmp");
            try {
                Files.writeString(tmp, GSON.toJson(new StorageState(cookies, ls)));
                try {
                    Files.move(tmp, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                    Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }
            Reporter.log("StorageManager: saved " + cookies.size() + " cookies + " + ls.size()
                    + " localStorage entries to " + filePath, LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("StorageManager: failed to save storage: " + e.getMessage(), LogLevel.WARN);
        }
    }

    public static void load(WebDriver driver, String filePath) {
        try {
            Path src = Paths.get(filePath);
            if (!Files.exists(src)) {
                Reporter.log("StorageManager: storage file not found: " + filePath, LogLevel.WARN);
                return;
            }
            StorageState state = GSON.fromJson(Files.readString(src), StorageState.class);
            if (state == null) return;
            String cur = null;
            try { cur = driver.getCurrentUrl(); } catch (Exception ignored) {}
            if (cur == null || cur.isBlank() || cur.startsWith("about:") || cur.startsWith("data:")) {
                Reporter.log("StorageManager: driver is on a blank page (" + cur + ") — cookies and localStorage "
                        + "cannot be applied until you navigate to the target site first", LogLevel.WARN);
            }
            int cookies = 0;
            if (state.cookies != null) {
                for (CookieDto d : state.cookies) {
                    try {
                        Cookie.Builder b = new Cookie.Builder(d.name, d.value)
                                .path(d.path != null ? d.path : "/")
                                .isSecure(d.secure).isHttpOnly(d.httpOnly);
                        if (d.domain != null && !d.domain.isBlank()) b.domain(d.domain);
                        if (d.expiryEpoch != null) b.expiresOn(new Date(d.expiryEpoch));
                        if (d.sameSite != null && !d.sameSite.isBlank()) b.sameSite(d.sameSite);
                        driver.manage().addCookie(b.build());
                        cookies++;
                    } catch (Exception perCookie) {
                        Reporter.log("StorageManager: skipped cookie '" + d.name + "': " + perCookie.getMessage(), LogLevel.WARN);
                    }
                }
            }
            int ls = writeLocalStorage(driver, state.localStorage);
            try { driver.navigate().refresh(); } catch (Exception ignored) {}
            Reporter.log("StorageManager: loaded " + cookies + " cookies + " + ls
                    + " localStorage entries from " + filePath, LogLevel.INFO_GREEN);
        } catch (Exception e) {
            Reporter.log("StorageManager: failed to load storage: " + e.getMessage(), LogLevel.WARN);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> readLocalStorage(WebDriver driver) {
        if (!(driver instanceof JavascriptExecutor js)) return Map.of();
        try {
            Object res = js.executeScript(
                    "var o={}; for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i); o[k]=localStorage.getItem(k);} return o;");
            if (res instanceof Map<?, ?> m) {
                Map<String, String> out = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) out.put(e.getKey().toString(), e.getValue().toString());
                }
                return out;
            }
        } catch (Exception ignored) {}
        return Map.of();
    }

    private static int writeLocalStorage(WebDriver driver, Map<String, String> ls) {
        if (ls == null || ls.isEmpty() || !(driver instanceof JavascriptExecutor js)) return 0;
        try {
            js.executeScript("var d=arguments[0]; for(var k in d){ try{ localStorage.setItem(k, d[k]); }catch(e){} }", ls);
            return ls.size();
        } catch (Exception e) {
            return 0;
        }
    }
}
