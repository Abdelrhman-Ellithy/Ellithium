package Tests;

import Ellithium.core.base.NonBDDSetup;
import org.testng.annotations.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class DetailsTests extends NonBDDSetup {
    @Test
    public void TestVersion(){
        given().baseUri("http://movieland.runasp.net/")
                .when().get("/api/Movies/Details/50")
                .then()
                .assertThat()
                .statusCode(404)
                .contentType("application/json; charset=utf-8")
                .time(lessThanOrEqualTo(1000L));
    }
}