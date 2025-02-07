package Tests;

import Ellithium.Utilities.assertion.AssertionExecutor;
import Ellithium.core.base.NonBDDSetup;
import Ellithium.core.reporting.Reporter;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class BookingAPITests extends NonBDDSetup {
    private String token;
    private int lastBookingID;

    @BeforeClass
    public void setUp() {
        RestAssured.baseURI = "https://restful-booker.herokuapp.com/";
    }
    @Test(priority = 1)
    public void createToken() {
        String payload =
                """
                        { 
                        \"username\": \"admin\", 
                        \"password\": \"password123\"
                   }
                """;
        Response response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/auth");
        AssertionExecutor.soft soft=new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        token = response.jsonPath().getString("token");
        soft.assertNotNull(token, "Token is null");
        soft.assertAll();
    }
    @Test(priority = 2)
    public void createBooking() {
        String payload = """
                {
                    \"firstname\": \"John\",
                    \"lastname\": \"Doe\",
                    \"totalprice\": 111,
                    \"depositpaid\": true,
                    \"bookingdates\": {
                        \"checkin\": \"2018-01-01\",
                        \"checkout\": \"2019-01-01\"
                    },
                    \"additionalneeds\": \"Breakfast\"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/booking"); //
        AssertionExecutor.soft soft=new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertTrue(response.asString().contains("bookingid"), "Booking ID not present in response");
        lastBookingID = response.jsonPath().getInt("bookingid");
        soft.assertNotSame(lastBookingID, 0, "Invalid Booking ID");
    }

    @Test(priority = 3)
    public void getAllBookings() {
        Response response = given()
                .when()
                .get("/booking");

        AssertionExecutor.soft soft=new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertTrue(response.asString().contains(String.valueOf(lastBookingID)), "Last Booking ID not found");
    }

    @Test(priority = 4)
    public void getSingleBooking() {
        Response response = given()
                .when()
                .get("/booking/" + lastBookingID);
        AssertionExecutor.soft soft=new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertTrue(response.time() < 1500, "Response time exceeded");

        String firstName = response.jsonPath().getString("firstname");
        String lastName = response.jsonPath().getString("lastname");

        soft.assertEquals(firstName, "John");
        soft.assertEquals(lastName, "Doe");
    }

    @Test(priority = 5)
    public void updateBooking() {
        String payload = """
                {
                    \"firstname\": \"Jane\",
                    \"lastname\": \"Smith\",
                    \"totalprice\": 111,
                    \"depositpaid\": true,
                    \"bookingdates\": {
                        \"checkin\": \"2018-01-01\",
                        \"checkout\": \"2019-01-01\"
                    },
                    \"additionalneeds\": \"Breakfast\"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Cookie", "token="+ token)
                .body(payload)
                .when()
                .put("/booking/" + lastBookingID);

        AssertionExecutor.soft soft=new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertTrue(response.time() < 1500, "Response time exceeded");

        String firstName = response.jsonPath().getString("firstname");
        String lastName = response.jsonPath().getString("lastname");

        soft.assertEquals(firstName, "Jane");
        soft.assertEquals(lastName, "Smith");
    }
    @Test(priority = 6)
    public void deleteCreatedBooking() {
        Response response = given()
                .when()
                .delete("/booking/" + lastBookingID);
        AssertionExecutor.soft soft=new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        Reporter.setTestCaseName("Delete the Created Booking");
    }
}
