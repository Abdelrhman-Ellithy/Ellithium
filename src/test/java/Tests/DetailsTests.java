package Tests;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import org.testng.annotations.Test;
import static io.restassured.RestAssured.given;
public class DetailsTests {
    @Test
    public void TestVersion(){
        String latest=given().
                baseUri("https://api.github.com").and().basePath("repos/Abdelrhman-Ellithy/Ellithium/releases/")
                .when().get("latest")
                .thenReturn().body().jsonPath().getString("name");
        Reporter.log(latest, LogLevel.INFO_GREEN);
    }
}
