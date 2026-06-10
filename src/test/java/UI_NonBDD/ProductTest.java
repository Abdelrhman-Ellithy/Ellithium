package UI_NonBDD;

import org.testng.annotations.Test;
import Pages.ProductPage;

public class ProductTest extends Base.BaseTests {

    @Test
    public void testProducts() {
        ProductPage productPage = new ProductPage(driver);
        productPage.setProducts();
    }

}
