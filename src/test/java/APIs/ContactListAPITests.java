package APIs;
import Ellithium.Utilities.assertion.AssertionExecutor;
import Ellithium.core.base.NonBDDSetup;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class ContactListAPITests extends NonBDDSetup {
    private String token;
    private String contactId;
    private final String FIRST_NAME = "Amy";
    private final String LAST_NAME = "Smith";
    private final String USER_EMAIL = "testtt@fake.com";

    @BeforeClass
    public void setUp() {
        RestAssured.baseURI = "https://thinking-tester-contact-list.herokuapp.com/";
    }

    @Test(priority = 1)
    public void loginUser() {
        String payload = """
                {
                    "email": "%s",
                    "password": "123456789"
                }
                """.formatted(USER_EMAIL);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/users/login");

        AssertionExecutor.soft soft = new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertTrue(response.time() < 3000, "Response time exceeded");
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");

        token = response.jsonPath().getString("token");
        soft.assertNotNull(token, "Token not found in response");

        soft.assertEquals(response.jsonPath().getString("user.firstName"), "said");
        soft.assertEquals(response.jsonPath().getString("user.lastName"), "ali");
        soft.assertEquals(response.jsonPath().getString("user.email"), USER_EMAIL);
        soft.assertAll();
    }

    @Test(priority = 2)
    public void addContact() {
        String payload = """
                {
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "asmith@thinkingtester.com"
                }
                """.formatted(FIRST_NAME, LAST_NAME);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(payload)
                .when()
                .post("/contacts");

        AssertionExecutor.soft soft = new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 201);
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");

        contactId = response.jsonPath().getString("_id");
        soft.assertNotNull(contactId, "Contact ID not generated");
        soft.assertEquals(response.jsonPath().getString("firstName"), FIRST_NAME);
        soft.assertEquals(response.jsonPath().getString("lastName"), LAST_NAME);
        soft.assertAll();
    }

    @Test(priority = 3)
    public void getContactList() {
        Response response = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/contacts");

        AssertionExecutor.soft soft = new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertEquals(response.jsonPath().getString("[0].firstName"), "Abdelrahman");
        soft.assertEquals(response.jsonPath().getString("[0].lastName"), "Ellithy");
        soft.assertAll();
    }

    @Test(priority = 4)
    public void getSingleContact() {
        Response response = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/contacts/" + contactId);

        AssertionExecutor.soft soft = new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertEquals(response.jsonPath().getString("firstName"), FIRST_NAME);
        soft.assertEquals(response.jsonPath().getString("lastName"), LAST_NAME);
        soft.assertAll();
    }

    @Test(priority = 5)
    public void updateContact() {
        String payload = """
                {
                    "firstName": "Asaid",
                    "lastName": "ghaly",
                    "email": "asmith@thinkingtester.com",
                    "location": {
                        "city": "Boston, MA",
                        "country": "USA"
                    }
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(payload)
                .when()
                .put("/contacts/" + contactId);

        AssertionExecutor.soft soft = new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertEquals(response.jsonPath().getString("firstName"), "Asaid");
        soft.assertEquals(response.jsonPath().getString("lastName"), "ghaly");
        soft.assertAll();
    }

    @Test(priority = 6)
    public void deleteContact() {
        Response response = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/contacts/" + contactId);

        AssertionExecutor.soft soft = new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertEquals(response.contentType(), "text/html; charset=utf-8");
        soft.assertAll();
    }

    // Negative Tests
    @Test(priority = 7)
    public void loginWithInvalidCredentials() {
        String payload = """
                {
                    "email": "%s",
                    "password": "12345678"
                }
                """.formatted(USER_EMAIL);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/users/login");

        AssertionExecutor.soft soft = new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 401);
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertAll();
    }

    @Test(priority = 8)
    public void addContactWithLongLastName() {
        String payload = """
                {
                    "firstName": "Amy",
                    "lastName": "mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm",
                    "email": "asmith@thinkingtester.com"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(payload)
                .when()
                .post("/contacts");

        AssertionExecutor.soft soft = new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 400);
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertTrue(response.getBody().asString().contains("is longer than the maximum allowed length (20)"));
        soft.assertAll();
    }

    @Test(priority = 9)
    public void getContactListUnauthorized() {
        Response response = given()
                .header("Authorization", "Bearer abcdf")
                .when()
                .get("/contacts");

        AssertionExecutor.soft soft = new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 401);
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertTrue(response.getBody().asString().contains("Please authenticate"));
        soft.assertAll();
    }
}