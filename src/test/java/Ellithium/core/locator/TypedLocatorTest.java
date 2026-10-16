package Ellithium.core.locator;

import Ellithium.Utilities.helpers.Locators;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TypedLocatorTest {

    @Test
    public void testFactoryMethods_BasicTypes() {
        TypedLocator idLoc = TypedLocator.id("user-id", "User Identifier");
        assertEquals(idLoc.type(), LocatorType.ID);
        assertEquals(idLoc.value(), "user-id");
        assertEquals(idLoc.friendlyName(), "User Identifier");
        assertEquals(idLoc.toBy(), By.id("user-id"));
        assertEquals(idLoc.get(), By.id("user-id"));

        TypedLocator cssLoc = TypedLocator.css(".submit-btn");
        assertEquals(cssLoc.type(), LocatorType.CSS);
        assertEquals(cssLoc.value(), ".submit-btn");
        assertNull(cssLoc.friendlyName());
        assertEquals(cssLoc.toBy(), By.cssSelector(".submit-btn"));

        TypedLocator xpathLoc = TypedLocator.xpath("//button[@type='submit']");
        assertEquals(xpathLoc.type(), LocatorType.XPATH);
        assertEquals(xpathLoc.toBy(), By.xpath("//button[@type='submit']"));

        TypedLocator nameLoc = TypedLocator.name("username");
        assertEquals(nameLoc.type(), LocatorType.NAME);
        assertEquals(nameLoc.toBy(), By.name("username"));

        TypedLocator classLoc = TypedLocator.className("btn primary");
        assertEquals(classLoc.type(), LocatorType.CLASS_NAME);
        // Multi-class is safely transformed into dot-joined CSS selector
        assertEquals(classLoc.toBy(), By.cssSelector(".btn.primary"));

        TypedLocator tagLoc = TypedLocator.tagName("header");
        assertEquals(tagLoc.type(), LocatorType.TAG_NAME);
        assertEquals(tagLoc.toBy(), By.tagName("header"));

        TypedLocator linkLoc = TypedLocator.linkText("Sign In");
        assertEquals(linkLoc.type(), LocatorType.LINK_TEXT);
        assertEquals(linkLoc.toBy(), By.linkText("Sign In"));

        TypedLocator partialLinkLoc = TypedLocator.partialLinkText("Sign");
        assertEquals(partialLinkLoc.type(), LocatorType.PARTIAL_LINK_TEXT);
        assertEquals(partialLinkLoc.toBy(), By.partialLinkText("Sign"));
    }

    @Test
    public void testFactoryMethods_ShadowAndIframe() {
        TypedLocator shadowLoc = TypedLocator.shadow("user-card", "button.edit", "Edit Button");
        assertTrue(shadowLoc.isShadow());
        assertFalse(shadowLoc.isIframe());
        assertEquals(shadowLoc.type(), LocatorType.SHADOW);
        assertEquals(shadowLoc.hostSelector(), "user-card");
        assertEquals(shadowLoc.value(), "button.edit");
        assertEquals(shadowLoc.friendlyName(), "Edit Button");
        assertEquals(shadowLoc.toBy(), By.cssSelector("user-card >>> button.edit"));

        TypedLocator iframeLoc = TypedLocator.iframe("iframe#payment-frame", "input#card-number");
        assertTrue(iframeLoc.isIframe());
        assertFalse(iframeLoc.isShadow());
        assertEquals(iframeLoc.type(), LocatorType.IFRAME);
        assertEquals(iframeLoc.hostSelector(), "iframe#payment-frame");
        assertEquals(iframeLoc.value(), "input#card-number");
    }

    @Test
    public void testFromBy_Inference() {
        TypedLocator fromXpath = TypedLocator.fromBy(By.xpath("//div[@class='container']"));
        assertEquals(fromXpath.type(), LocatorType.XPATH);
        assertEquals(fromXpath.value(), "//div[@class='container']");

        TypedLocator fromCss = TypedLocator.fromBy(By.cssSelector("div.card"));
        assertEquals(fromCss.type(), LocatorType.CSS);
        assertEquals(fromCss.value(), "div.card");

        TypedLocator fromShadow = TypedLocator.fromBy(By.cssSelector("todo-app >>> li.item"));
        assertEquals(fromShadow.type(), LocatorType.SHADOW);
        assertEquals(fromShadow.hostSelector(), "todo-app");
        assertEquals(fromShadow.value(), "li.item");
        assertTrue(fromShadow.isShadow());

        TypedLocator fromId = TypedLocator.fromBy(By.id("login_field"));
        assertEquals(fromId.type(), LocatorType.ID);
        assertEquals(fromId.value(), "login_field");

        TypedLocator fromName = TypedLocator.fromBy(By.name("session_token"));
        assertEquals(fromName.type(), LocatorType.NAME);
        assertEquals(fromName.value(), "session_token");

        TypedLocator fromClass = TypedLocator.fromBy(By.className("menu-item"));
        assertEquals(fromClass.type(), LocatorType.CLASS_NAME);
        assertEquals(fromClass.value(), "menu-item");

        TypedLocator fromLink = TypedLocator.fromBy(By.linkText("Documentation"));
        assertEquals(fromLink.type(), LocatorType.LINK_TEXT);
        assertEquals(fromLink.value(), "Documentation");

        TypedLocator fromPartialLink = TypedLocator.fromBy(By.partialLinkText("Doc"));
        assertEquals(fromPartialLink.type(), LocatorType.PARTIAL_LINK_TEXT);
        assertEquals(fromPartialLink.value(), "Doc");

        TypedLocator fromTag = TypedLocator.fromBy(By.tagName("footer"));
        assertEquals(fromTag.type(), LocatorType.TAG_NAME);
        assertEquals(fromTag.value(), "footer");
    }

    @Test
    public void testFromStringAndLocatorsBridge() {
        TypedLocator parsedPrefix = Locators.typed("id=email-input");
        assertEquals(parsedPrefix.type(), LocatorType.ID);
        assertEquals(parsedPrefix.value(), "email-input");

        TypedLocator parsedXpath = Locators.typed("//button[text()='Submit']");
        assertEquals(parsedXpath.type(), LocatorType.XPATH);
        assertEquals(parsedXpath.value(), "//button[text()='Submit']");

        TypedLocator parsedFromBy = Locators.typed(By.cssSelector(".main-nav"));
        assertEquals(parsedFromBy.type(), LocatorType.CSS);
        assertEquals(parsedFromBy.value(), ".main-nav");
    }

    @Test
    public void testImmutabilityAndCopy() {
        TypedLocator original = TypedLocator.css(".badge");
        assertNull(original.friendlyName());

        TypedLocator withName = original.withFriendlyName("Status Badge");
        assertEquals(withName.friendlyName(), "Status Badge");
        assertEquals(withName.type(), original.type());
        assertEquals(withName.value(), original.value());
        assertNull(original.friendlyName(), "Original instance should remain immutable");

        TypedLocator withHost = original.withHostSelector("parent-host");
        assertEquals(withHost.hostSelector(), "parent-host");
        assertTrue(withHost.isShadow());
        assertNull(original.hostSelector(), "Original instance should remain immutable");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullTypeRejection() {
        new TypedLocator(null, "value", null, null, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullValueRejection() {
        new TypedLocator(LocatorType.CSS, null, null, null, null);
    }
}
