package Ellithium.Utilities.interactions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Set-heal derivation tests. Every assertion is robust whether the local ONNX model is loaded
 * (embedding cosine) or not (deCamelCase token-Jaccard): the expected winning class always shares
 * a token with the intent and the losing classes never do, so both ranking paths agree.
 *
 * <p>Note: element-list mocks are always built into locals BEFORE stubbing {@code findElements},
 * because building a mock calls {@code when(...)} and Mockito forbids that inside a {@code thenReturn}.
 */
public class MultiElementHealTest {

    private WebDriver driver;
    private ElementActions<WebDriver> actions;

    @BeforeMethod
    public void setup() {
        driver = mock(WebDriver.class);
        actions = new ElementActions<>(driver);
    }

    private static List<WebElement> elementsOfTag(String tag, int n) {
        List<WebElement> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            WebElement el = mock(WebElement.class);
            when(el.getTagName()).thenReturn(tag);
            list.add(el);
        }
        return list;
    }

    private WebElement anchorWith(String tag, String classAttr) {
        WebElement a = mock(WebElement.class);
        when(a.getTagName()).thenReturn(tag);
        when(a.getDomAttribute("class")).thenReturn(classAttr);
        return a;
    }

    // ──────────────────────── Core over-grab fix ────────────────────────

    @Test
    public void pricesRenamed_healsToSemanticClass_notBroadUtilityClass() {
        List<WebElement> productPrice = elementsOfTag("div", 10);
        List<WebElement> colMd3 = elementsOfTag("div", 60);
        WebElement anchor = anchorWith("div", "product-price col-md-3");
        when(driver.findElements(By.cssSelector("div.product-price"))).thenReturn(productPrice);
        when(driver.findElements(By.cssSelector("div.col-md-3"))).thenReturn(colMd3);

        By set = actions.deriveSetSelector(anchor, By.cssSelector(".price"));
        assertEquals(set, By.cssSelector("div.product-price"));
        assertEquals(driver.findElements(set).size(), 10);
    }

    @Test
    public void semanticWins_evenAtExtremeCountRatio() {
        List<WebElement> priceRow = elementsOfTag("div", 3);
        List<WebElement> wrapper = elementsOfTag("div", 500);
        WebElement anchor = anchorWith("div", "price-row wrapper");
        when(driver.findElements(By.cssSelector("div.price-row"))).thenReturn(priceRow);
        when(driver.findElements(By.cssSelector("div.wrapper"))).thenReturn(wrapper);

        assertEquals(actions.deriveSetSelector(anchor, By.cssSelector(".price")), By.cssSelector("div.price-row"));
    }

    @Test
    public void intentFromIdLocator_picksSemanticClass() {
        List<WebElement> priceValue = elementsOfTag("div", 5);
        List<WebElement> grid = elementsOfTag("div", 30);
        WebElement anchor = anchorWith("div", "price-value grid");
        when(driver.findElements(By.cssSelector("div.price-value"))).thenReturn(priceValue);
        when(driver.findElements(By.cssSelector("div.grid"))).thenReturn(grid);

        assertEquals(actions.deriveSetSelector(anchor, By.id("price")), By.cssSelector("div.price-value"));
    }

    @Test
    public void intentFromXpathClassPredicate_picksSemanticClass() {
        List<WebElement> priceItem = elementsOfTag("li", 8);
        List<WebElement> listRow = elementsOfTag("li", 25);
        WebElement anchor = anchorWith("li", "price-item list-row");
        when(driver.findElements(By.cssSelector("li.price-item"))).thenReturn(priceItem);
        when(driver.findElements(By.cssSelector("li.list-row"))).thenReturn(listRow);

        By original = By.xpath("//li[contains(@class,'price')]");
        assertEquals(actions.deriveSetSelector(anchor, original), By.cssSelector("li.price-item"));
    }

    // ──────────────────────── Structural / robustness edges ────────────────────────

    @Test
    public void uppercaseTag_isLowercasedInSelector() {
        List<WebElement> priceTag = elementsOfTag("DIV", 5);
        List<WebElement> container = elementsOfTag("DIV", 80);
        WebElement anchor = anchorWith("DIV", "price-tag container");
        when(driver.findElements(By.cssSelector("div.price-tag"))).thenReturn(priceTag);
        when(driver.findElements(By.cssSelector("div.container"))).thenReturn(container);

        assertEquals(actions.deriveSetSelector(anchor, By.cssSelector(".price")), By.cssSelector("div.price-tag"));
    }

    @Test
    public void whitespacePaddedAndCollapsedClassAttribute_isParsed() {
        List<WebElement> productPrice = elementsOfTag("div", 6);
        List<WebElement> container = elementsOfTag("div", 90);
        WebElement anchor = anchorWith("div", "  product-price    container  ");
        when(driver.findElements(By.cssSelector("div.product-price"))).thenReturn(productPrice);
        when(driver.findElements(By.cssSelector("div.container"))).thenReturn(container);

        assertEquals(actions.deriveSetSelector(anchor, By.cssSelector(".price")), By.cssSelector("div.product-price"));
    }

    @Test
    public void skipsCandidateClassWhoseLookupThrows() {
        List<WebElement> priceCell = elementsOfTag("div", 4);
        WebElement anchor = anchorWith("div", "broken price-cell");
        when(driver.findElements(By.cssSelector("div.broken"))).thenThrow(new WebDriverException("boom"));
        when(driver.findElements(By.cssSelector("div.price-cell"))).thenReturn(priceCell);

        assertEquals(actions.deriveSetSelector(anchor, By.cssSelector(".price")), By.cssSelector("div.price-cell"));
    }

    @Test
    public void ignoresUnsafeClassTokens_neverQueriesThem() {
        List<WebElement> rows = elementsOfTag("tr", 4);
        WebElement anchor = anchorWith("tr", "3invalid data-row has:colon");
        when(driver.findElements(By.cssSelector("tr.data-row"))).thenReturn(rows);

        assertEquals(actions.deriveSetSelector(anchor, By.cssSelector(".row")), By.cssSelector("tr.data-row"));
        verify(driver, never()).findElements(By.cssSelector("tr.3invalid"));
        verify(driver, never()).findElements(By.cssSelector("tr.has:colon"));
    }

    @Test
    public void singleRepeatingClass_amongSingletons_isReturnedWithoutRanking() {
        List<WebElement> price = elementsOfTag("div", 6);
        List<WebElement> a = elementsOfTag("div", 1);
        List<WebElement> b = elementsOfTag("div", 1);
        WebElement anchor = anchorWith("div", "price a b");
        when(driver.findElements(By.cssSelector("div.price"))).thenReturn(price);
        when(driver.findElements(By.cssSelector("div.a"))).thenReturn(a);
        when(driver.findElements(By.cssSelector("div.b"))).thenReturn(b);

        assertEquals(actions.deriveSetSelector(anchor, By.cssSelector(".whatever")), By.cssSelector("div.price"));
    }

    @Test
    public void multipleRepeatingButNoSemanticOverlap_stillHealsToARepeatingSet() {
        List<WebElement> col = elementsOfTag("div", 10);
        List<WebElement> row = elementsOfTag("div", 20);
        WebElement anchor = anchorWith("div", "col row");
        when(driver.findElements(By.cssSelector("div.col"))).thenReturn(col);
        when(driver.findElements(By.cssSelector("div.row"))).thenReturn(row);

        By set = actions.deriveSetSelector(anchor, By.cssSelector(".zzz"));
        assertNotNull(set);
        assertTrue(set.equals(By.cssSelector("div.col")) || set.equals(By.cssSelector("div.row")),
                "expected a repeating set selector, got " + set);
    }

    // ──────────────────────── Null / degrade edges ────────────────────────

    @Test
    public void returnsNull_whenNoClassMatchesMoreThanOne() {
        List<WebElement> one = elementsOfTag("button", 1);
        WebElement anchor = anchorWith("button", "submit");
        when(driver.findElements(By.cssSelector("button.submit"))).thenReturn(one);

        assertNull(actions.deriveSetSelector(anchor, By.id("submit")));
    }

    @Test
    public void returnsNull_whenAnchorClassIsNull() {
        assertNull(actions.deriveSetSelector(anchorWith("div", null), By.cssSelector(".price")));
    }

    @Test
    public void returnsNull_whenAnchorClassIsBlank() {
        assertNull(actions.deriveSetSelector(anchorWith("div", "   "), By.cssSelector(".price")));
    }

    @Test
    public void returnsNull_whenAnchorTagIsNull() {
        WebElement a = mock(WebElement.class);
        when(a.getTagName()).thenReturn(null);
        when(a.getDomAttribute("class")).thenReturn("price");
        assertNull(actions.deriveSetSelector(a, By.cssSelector(".price")));
    }

    @Test
    public void returnsNull_whenEveryCandidateLookupThrows() {
        WebElement anchor = anchorWith("div", "price wrapper");
        when(driver.findElements(any(By.class))).thenThrow(new WebDriverException("session gone"));
        assertNull(actions.deriveSetSelector(anchor, By.cssSelector(".price")));
    }

    @Test
    public void returnsNull_whenAnchorTagNameThrows() {
        WebElement a = mock(WebElement.class);
        when(a.getTagName()).thenThrow(new WebDriverException("stale"));
        assertNull(actions.deriveSetSelector(a, By.cssSelector(".price")));
    }
}
