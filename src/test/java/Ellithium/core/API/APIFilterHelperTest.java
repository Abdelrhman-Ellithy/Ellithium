package Ellithium.core.API;

import io.restassured.RestAssured;
import org.testng.Assert;
import org.testng.annotations.Test;

public class APIFilterHelperTest {

    @Test
    public void testApplyFilter_registersFilters() {
        APIFilterHelper.applyFilter();
        Assert.assertFalse(RestAssured.filters().isEmpty(), "RestAssured filters should not be empty after applyFilter()");
    }
}
