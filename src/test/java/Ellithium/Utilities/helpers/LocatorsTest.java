package Ellithium.Utilities.helpers;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * {@link Locators#className} must never let a multi-class value reach {@code By.className} — that
 * constructor throws synchronously on whitespace, before Ellithium's healing cascade could ever see
 * the locator to fix it.
 */
public class LocatorsTest {

    @Test
    public void singleClass_delegatesToByClassNameUnchanged() {
        Assert.assertEquals(Locators.className("btn"), By.className("btn"));
    }

    @Test
    public void multiClass_becomesDotJoinedCssSelector() {
        Assert.assertEquals(Locators.className("btn primary"), By.cssSelector(".btn.primary"));
    }

    @Test
    public void multiClass_withExtraWhitespace_isNormalized() {
        Assert.assertEquals(Locators.className("  btn   primary  "), By.cssSelector(".btn.primary"));
    }

    @Test
    public void threeClasses_allJoined() {
        Assert.assertEquals(Locators.className("a b c"), By.cssSelector(".a.b.c"));
    }

    @Test
    public void neverThrowsForWhitespaceContainingValue() {
        // The whole point: this must not throw, unlike raw By.className("btn primary").
        By result = Locators.className("btn primary");
        Assert.assertNotNull(result);
    }

    @Test
    public void tailwindVariantColon_isEscapedNotLeftAsPseudoClassSyntax() {
        // Tailwind utility classes routinely contain ':' (hover:, focus:, md:) — unescaped, CSS
        // parses ":bg-red-500" as an (invalid) pseudo-class, not a literal class-name character.
        By result = Locators.className("flex hover:bg-red-500");
        Assert.assertEquals(result, By.cssSelector(".flex.hover\\:bg-red-500"));
    }

    @Test
    public void tailwindArbitraryValueBrackets_areEscaped() {
        // Tailwind arbitrary values use literal brackets, e.g. w-[100px] — unescaped brackets are
        // CSS attribute-selector syntax, not part of a class name.
        By result = Locators.className("w-[100px] flex");
        Assert.assertEquals(result, By.cssSelector(".w-\\[100px\\].flex"));
    }
}
