package APIs;

import Ellithium.Utilities.assertion.AssertionExecutor;
import Ellithium.core.API.Environment;
import Ellithium.core.reporting.Reporter;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static io.restassured.RestAssured.given;

public class BookingAPITests extends BaseAssertion{
    private Environment env;

    @BeforeClass
    public void setUp() {
        RestAssured.baseURI = "https://restful-booker.herokuapp.com/";
        env = new Environment("RestfulBooker");
    }

    @Test(priority = 1)
    public void createToken() {
        String payload = """
                { 
                    "username": "admin", 
                    "password": "password123"
                }
                """;
        Response response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/auth");

        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertTrue(response.time() < 3000, "Response time exceeded");

        env.set("token", response.jsonPath().getString("token"));
        soft.assertNotNull(env.get("token"), "Token is null");
        soft.assertAll();
    }

    @Test(priority = 2)
    public void createBooking() {
        String payload = """
                {
                    "firstname": "John",
                    "lastname": "Doe",
                    "totalprice": 111,
                    "depositpaid": true,
                    "bookingdates": {
                        "checkin": "2018-01-01",
                        "checkout": "2019-01-01"
                    },
                    "additionalneeds": "Breakfast"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/booking"); //
        
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertTrue(response.asString().contains("bookingid"), "Booking ID not present in response");
        env.set("bookingId", response.jsonPath().getInt("bookingid"));
        soft.assertNotEquals(env.getAsInteger("bookingId"), 0, "Invalid Booking ID");
        soft.assertAll();
    }

    @Test(priority = 3)
    public void getAllBookings() {
        Response response = given()
                .when()
                .get("/booking");
        
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertTrue(response.asString().contains(String.valueOf(env.getAsInteger("bookingId"))), "Booking ID not found");
        soft.assertAll();
    }

    @Test(priority = 4)
    public void getSingleBooking() {
        Response response = given()
                .when()
                .get("/booking/" + env.getAsInteger("bookingId"));
        
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertTrue(response.time() < 1500, "Response time exceeded");

        String firstName = response.jsonPath().getString("firstname");
        String lastName = response.jsonPath().getString("lastname");

        soft.assertEquals(firstName, "John");
        soft.assertEquals(lastName, "Doe");
        soft.assertAll();
    }

    @Test(priority = 5)
    public void updateBooking() {
        String payload = """
                {
                    "firstname": "Jane",
                    "lastname": "Smith",
                    "totalprice": 111,
                    "depositpaid": true,
                    "bookingdates": {
                        "checkin": "2018-01-01",
                        "checkout": "2019-01-01"
                    },
                    "additionalneeds": "Breakfast"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Cookie", "token="+ env.get("token"))
                .body(payload)
                .when()
                .put("/booking/" + env.getAsInteger("bookingId"));

        
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertTrue(response.time() < 1500, "Response time exceeded");

        String firstName = response.jsonPath().getString("firstname");
        String lastName = response.jsonPath().getString("lastname");

        soft.assertEquals(firstName, "Jane");
        soft.assertEquals(lastName, "Smith");
        soft.assertAll();
    }
    @Test(priority = 6)
    public void deleteCreatedBooking() {
        Response response = given()
                .header("Cookie", "token="+ env.get("token"))
                .when()
                .delete("/booking/" + env.getAsInteger("bookingId"));
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        Reporter.setTestCaseName("Delete the Created Booking");
        soft.assertAll();
    }
}
