package Tests;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import io.restassured.RestAssured;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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
