package Ellithium.core.ai.healing;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.*;

public class HealingContextBuilderTest {

    @Test
    public void parseByLocator_standardBy_parsesMethodAndValue() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        HealingContextBuilder.parseByLocator("By.id: username", ctx);

        assertEquals(ctx.byMethod, "id");
        assertEquals(ctx.byValue, "username");
    }

    @Test
    public void parseByLocator_xpathWithColons_parsesCorrectly() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        HealingContextBuilder.parseByLocator("By.xpath: //div[@class='header:main']", ctx);

        assertEquals(ctx.byMethod, "xpath");
        assertEquals(ctx.byValue, "//div[@class='header:main']");
    }

    @Test
    public void parseByLocator_appiumBy_parsesCorrectly() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        HealingContextBuilder.parseByLocator("AppiumBy.accessibilityId: submit_button", ctx);

        assertEquals(ctx.byMethod, "accessibilityId");
        assertEquals(ctx.byValue, "submit_button");
    }

    @Test
    public void parseByLocator_nullOrInvalid_handlesSafely() {
        HealingContextBuilder.HealingContext ctx = new HealingContextBuilder.HealingContext();
        HealingContextBuilder.parseByLocator(null, ctx);
        assertNull(ctx.byMethod);
        assertNull(ctx.byValue);

        HealingContextBuilder.parseByLocator("CustomLocatorStyle", ctx);
        assertNull(ctx.byMethod);
        assertNull(ctx.byValue);
    }

    @Test
    public void readCallSiteSource_validFile_returnsSurroundingSnippet() throws IOException {
        Path tempFile = Files.createTempFile("callSiteTest", ".java");
        try {
            List<String> content = List.of(
                    "public class TestClass {",
                    "    public void testMethod() {",
                    "        stepOne();",
                    "        driver.findElement(btn).click();",
                    "        stepThree();",
                    "    }",
                    "}"
            );
            Files.write(tempFile, content);

            // Call site is line 4
            String snippet = HealingContextBuilder.readCallSiteSource(tempFile.toString(), 4);
            assertNotNull(snippet);
            assertTrue(snippet.contains("driver.findElement(btn).click();"));
            assertTrue(snippet.contains("stepOne();"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void readCallSiteSource_invalidFileOrLine_returnsNull() {
        assertNull(HealingContextBuilder.readCallSiteSource("non_existent_file.java", 10));
        assertNull(HealingContextBuilder.readCallSiteSource(null, 5));
        assertNull(HealingContextBuilder.readCallSiteSource("dummy.java", -1));
    }

    @Test
    public void resolveFieldNameFromSource_staticFinalField_resolvesFieldName() throws IOException {
        Path tempFile = Files.createTempFile("LoginPage", ".java");
        try {
            List<String> content = List.of(
                    "package com.test;",
                    "import org.openqa.selenium.By;",
                    "public class LoginPage {",
                    "    private static final By loginBtn = By.id(\"submit\");",
                    "    public void clickLogin() {",
                    "        driver.findElement(loginBtn).click();",
                    "    }",
                    "}"
            );
            Files.write(tempFile, content);

            // Line 6 calls driver.findElement(loginBtn).click();
            String fieldName = HealingContextBuilder.resolveFieldNameFromSource(tempFile.toString(), 6);
            assertEquals(fieldName, "loginBtn");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void resolveFieldNameFromSource_inlineLocator_returnsNull() throws IOException {
        Path tempFile = Files.createTempFile("InlinePage", ".java");
        try {
            List<String> content = List.of(
                    "package com.test;",
                    "import org.openqa.selenium.By;",
                    "public class InlinePage {",
                    "    public void clickLogin() {",
                    "        driver.findElement(By.id(\"submit\")).click();",
                    "    }",
                    "}"
            );
            Files.write(tempFile, content);

            // Line 5 uses inline By.id("submit")
            String fieldName = HealingContextBuilder.resolveFieldNameFromSource(tempFile.toString(), 5);
            assertNull(fieldName, "Inline locator should not resolve to a field name");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void resolveFieldNameFromSource_fieldBelowMethod_resolvesFieldName() throws IOException {
        Path tempFile = Files.createTempFile("FieldBelowPage", ".java");
        try {
            List<String> content = List.of(
                    "package com.test;",
                    "import org.openqa.selenium.By;",
                    "public class FieldBelowPage {",
                    "    public void clickSearch() {",
                    "        element.click(searchBar);",
                    "    }",
                    "    protected By searchBar = By.name(\"q\");",
                    "}"
            );
            Files.write(tempFile, content);

            // Line 5 calls element.click(searchBar);
            String fieldName = HealingContextBuilder.resolveFieldNameFromSource(tempFile.toString(), 5);
            assertEquals(fieldName, "searchBar");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void resolveFieldNameFromSource_nullOrOutOfBounds_handlesSafely() {
        assertNull(HealingContextBuilder.resolveFieldNameFromSource(null, 1));
        assertNull(HealingContextBuilder.resolveFieldNameFromSource("non_existent.java", 1));
    }
}
