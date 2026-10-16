package Ellithium.core.ai.healing;

import Ellithium.core.ai.models.ElementFingerprint;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaselineStoreIntegrationTest {

    private static final Path JSON_PATH = Paths.get("Test-Output" + File.separator + "healing-baselines.json");
    private static final Path DB_PATH   = Paths.get("Test-Output" + File.separator + "healing-baselines.db");

    @Test
    public void captureAndFlush_createsBothSqliteAndJsonDualExport() {
        WebDriver driver = mock(WebDriver.class);
        when(driver.getCurrentUrl()).thenReturn("https://example.com/login");
        when(driver.getTitle()).thenReturn("Login");

        String uniqueId = "loginBtn-" + System.nanoTime();
        WebElement element = mock(WebElement.class);
        when(element.getTagName()).thenReturn("button");
        when(element.getAttribute("id")).thenReturn(uniqueId);

        By locator = By.id(uniqueId);
        BaselineStore.capture(driver, locator, element);

        ElementFingerprint retrieved = BaselineStore.getBaseline(driver, locator);
        Assert.assertNotNull(retrieved, "Fingerprint should be present immediately in memory");
        Assert.assertEquals(retrieved.getId(), uniqueId);

        // Flush commits to both SQLite and JSON
        BaselineStore.flush();

        Assert.assertTrue(Files.exists(DB_PATH), "SQLite DB file must exist after flush");
        Assert.assertTrue(Files.exists(JSON_PATH), "JSON export file must exist after flush");

        // Verify SQLite has the record
        BaselineSqliteStorage sqlite = BaselineSqliteStorage.getDefault();
        Assert.assertFalse(sqlite.isEmpty(), "SQLite DB must not be empty after flush");
        Assert.assertTrue(sqlite.count() >= 1, "SQLite must have at least 1 record");
        String key = BaselineStore.pageKey(driver, locator.toString());
        Assert.assertNotNull(sqlite.get(key), "SQLite must contain the captured locator key: " + key);
    }
}
