package APIs;
import Ellithium.Utilities.assertion.AssertionExecutor;
import Ellithium.core.API.Environment;
import Ellithium.core.base.NonBDDSetup;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class ContactListAPITests extends NonBDDSetup {
    private Environment env;
    private final String USER_EMAIL = "testtt@fake.com";

    @BeforeClass
    public void setUp() {
        RestAssured.baseURI = "https://thinking-tester-contact-list.herokuapp.com/";
        env = new Environment("ContactList");
        
        // Store test data in environment
        env.set("firstName", "Amy");
        env.set("lastName", "Smith");
        env.set("userEmail", USER_EMAIL);
    }

    @Test(priority = 1)
    public void loginUser() {
        String payload = """
                {
                    "email": "%s",
                    "password": "123456789"
                }
                """.formatted(env.get("userEmail"));

        Response response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/users/login");

        AssertionExecutor.soft soft = new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertTrue(response.time() < 4000, "Response time exceeded");
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");

        env.set("token", response.jsonPath().getString("token"));
        soft.assertNotNull(env.get("token"), "Token not found in response");

        soft.assertEquals(response.jsonPath().getString("user.firstName"), "said");
        soft.assertEquals(response.jsonPath().getString("user.lastName"), "ali");
        soft.assertEquals(response.jsonPath().getString("user.email"), env.get("userEmail"));
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
                """.formatted(env.get("firstName"), env.get("lastName"));

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + env.get("token"))
                .body(payload)
                .when()
                .post("/contacts");

        AssertionExecutor.soft soft = new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 201);
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");

        env.set("contactId", response.jsonPath().getString("_id"));
        soft.assertNotNull(env.get("contactId"), "Contact ID not generated");
        soft.assertEquals(response.jsonPath().getString("firstName"), env.get("firstName"));
        soft.assertEquals(response.jsonPath().getString("lastName"), env.get("lastName"));
        soft.assertAll();
    }

    @Test(priority = 3)
    public void getContactList() {
        Response response = given()
                .header("Authorization", "Bearer " + env.get("token"))
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
                .header("Authorization", "Bearer " + env.get("token"))
                .when()
                .get("/contacts/" + env.get("contactId"));

        AssertionExecutor.soft soft = new AssertionExecutor.soft();
        soft.assertEquals(response.getStatusCode(), 200);
        soft.assertTrue(response.time() < 1500, "Response time exceeded");
        soft.assertEquals(response.contentType(), "application/json; charset=utf-8");
        soft.assertEquals(response.jsonPath().getString("firstName"), env.get("firstName"));
        soft.assertEquals(response.jsonPath().getString("lastName"), env.get("lastName"));
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
                .header("Authorization", "Bearer " + env.get("token"))
                .body(payload)
                .when()
                .put("/contacts/" + env.get("contactId"));

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
                .header("Authorization", "Bearer " + env.get("token"))
                .when()
                .delete("/contacts/" + env.get("contactId"));

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
                """.formatted(env.get("userEmail"));

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
                .header("Authorization", "Bearer " + env.get("token"))
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