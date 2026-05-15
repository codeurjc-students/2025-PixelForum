package es.codeurjc.backend.e2e.server;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

@Tag("e2e")
@DisplayName("UserController System Test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerE2ETest {

    @LocalServerPort
    private int port;

    private String authToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = "/api/v1/users";

        // GIVEN
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "martin");
        loginData.put("password", "user");

        // WHEN
        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(loginData)
                .when()
                .post("http://localhost:" + port + "/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        authToken = loginResponse.getCookie("AuthToken");
    }

    @Test
    @DisplayName("Get all users system test")
    void getAllUsersSystemTest() {
        // WHEN & THEN
        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("size()", greaterThan(0));
    }

    @Test
    @DisplayName("Get user by id should succeed")
    void getUserByIdSystemTest() {
        // WHEN & THEN
        given()
                .when()
                .get("/1")
                .then()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("username", notNullValue());
    }

    @Test
    @DisplayName("Get own user details should succeed")
    void getUserDetailsOwnSystemTest() {
        // WHEN & THEN
        given()
                .cookie("AuthToken", authToken)
                .when()
                .get("/2/details")
                .then()
                .statusCode(200)
                .body("id", equalTo(2))
                .body("username", equalTo("martin"))
                .body("email", notNullValue());
    }

    @Test
    @DisplayName("Get other user details should fail")
    void getUserDetailsAccessDeniedSystemTest() {
        // WHEN & THEN
        given()
                .cookie("AuthToken", authToken)
                .when()
                .get("/1/details")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Create new user should succeed")
    void createUserSystemTest() {
        // GIVEN
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "newuser" + System.currentTimeMillis());
        userData.put("email", "newuser" + System.currentTimeMillis() + "@example.com");
        userData.put("password", "password123");
        userData.put("bio", "New user bio");

        // WHEN & THEN
        given()
                .contentType(ContentType.JSON)
                .body(userData)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("username", equalTo(userData.get("username")))
                .body("roles", hasItem("USER"));
    }

    @Test
    @DisplayName("Create user with duplicate username should fail")
    void createUserDuplicateUsernameSystemTest() {
        // GIVEN
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "martin");
        userData.put("email", "different@example.com");
        userData.put("password", "password123");
        userData.put("bio", "User bio");

        // WHEN & THEN
        given()
                .contentType(ContentType.JSON)
                .body(userData)
                .when()
                .post()
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Update own profile should succeed")
    void updateUserOwnProfileSystemTest() {
        // GIVEN
        Map<String, String> updateData = new HashMap<>();
        updateData.put("bio", "Updated bio at " + System.currentTimeMillis());

        // WHEN & THEN
        given()
                .cookie("AuthToken", authToken)
                .contentType(ContentType.JSON)
                .body(updateData)
                .when()
                .patch("/2")
                .then()
                .statusCode(200)
                .body("bio", equalTo(updateData.get("bio")));
    }

    @Test
    @DisplayName("Update other user profile should fail")
    void updateUserAccessDeniedSystemTest() {
        // GIVEN
        Map<String, String> updateData = new HashMap<>();
        updateData.put("bio", "Trying to update other user");

        // WHEN & THEN
        given()
                .cookie("AuthToken", authToken)
                .contentType(ContentType.JSON)
                .body(updateData)
                .when()
                .patch("/1")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Change own password should succeed")
    void changePasswordSystemTest() {
        // GIVEN: Create a new user to change password
        Map<String, String> userData = new HashMap<>();
        String uniqueUsername = "changepwd" + System.currentTimeMillis();
        userData.put("username", uniqueUsername);
        userData.put("email", "changepwd" + System.currentTimeMillis() + "@example.com");
        userData.put("password", "oldPassword123");
        userData.put("bio", "User for password change");

        Response createResponse = given()
                .contentType(ContentType.JSON)
                .body(userData)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract().response();

        Long userId = createResponse.jsonPath().getLong("id");

        // Login as newly created user
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", uniqueUsername);
        loginData.put("password", "oldPassword123");

        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(loginData)
                .when()
                .post("http://localhost:" + port + "/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        String userToken = loginResponse.getCookie("AuthToken");

        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("oldPassword", "oldPassword123");
        passwordData.put("newPassword", "newPassword123");

        // WHEN & THEN
        given()
                .cookie("AuthToken", userToken)
                .contentType(ContentType.JSON)
                .body(passwordData)
                .when()
                .patch("/" + userId + "/password")
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("Change password with wrong old password should fail")
    void changePasswordWrongOldPasswordSystemTest() {
        // GIVEN
        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("oldPassword", "wrongPassword");
        passwordData.put("newPassword", "newPassword123");

        // WHEN & THEN
        given()
                .cookie("AuthToken", authToken)
                .contentType(ContentType.JSON)
                .body(passwordData)
                .when()
                .patch("/2/password")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Delete own account should succeed")
    void deleteUserOwnAccountSystemTest() {
        // GIVEN: Create a new user to delete
        Map<String, String> userData = new HashMap<>();
        String uniqueUsername = "usertodeleteuser" + System.currentTimeMillis();
        userData.put("username", uniqueUsername);
        userData.put("email", "delete_" + System.currentTimeMillis() + "@example.com");
        userData.put("password", "password123");
        userData.put("bio", "User to delete");

        Response createResponse = given()
                .contentType(ContentType.JSON)
                .body(userData)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract().response();

        Long userId = createResponse.jsonPath().getLong("id");

        // Login as newly created user
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", uniqueUsername);
        loginData.put("password", "password123");

        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(loginData)
                .when()
                .post("http://localhost:" + port + "/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        String userToken = loginResponse.getCookie("AuthToken");

        // WHEN & THEN
        given()
                .cookie("AuthToken", userToken)
                .when()
                .delete("/" + userId)
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("Delete other user account should fail")
    void deleteUserAccessDeniedSystemTest() {
        // WHEN & THEN
        given()
                .cookie("AuthToken", authToken)
                .when()
                .delete("/1")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Set profile image should succeed")
    void setProfileImageSystemTest() {
        // GIVEN: Upload an image first
        byte[] imageData = new byte[] { 1, 2, 3, 4, 5 };

        Response uploadResponse = given()
                .cookie("AuthToken", authToken)
                .multiPart("files", "avatar.png", imageData, "image/png")
                .when()
                .post("http://localhost:" + port + "/api/v1/images")
                .then()
                .statusCode(200)
                .extract().response();

        Long imageId = uploadResponse.jsonPath().getLong("[0]");

        // WHEN & THEN
        given()
                .cookie("AuthToken", authToken)
                .queryParam("imageId", imageId)
                .when()
                .post("/2/avatar")
                .then()
                .statusCode(200)
                .body("avatar", equalTo(imageId.intValue()));
    }

    @Test
    @DisplayName("Remove profile image should succeed")
    void removeProfileImageSystemTest() {
        // GIVEN: Set an image first
        byte[] imageData = new byte[] { 1, 2, 3, 4, 5 };

        Response uploadResponse = given()
                .cookie("AuthToken", authToken)
                .multiPart("files", "avatar.png", imageData, "image/png")
                .when()
                .post("http://localhost:" + port + "/api/v1/images")
                .then()
                .statusCode(200)
                .extract().response();

        Long imageId = uploadResponse.jsonPath().getLong("[0]");

        given()
                .cookie("AuthToken", authToken)
                .queryParam("imageId", imageId)
                .when()
                .post("/2/avatar")
                .then()
                .statusCode(200);

        // WHEN & THEN
        given()
                .cookie("AuthToken", authToken)
                .when()
                .delete("/2/avatar")
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("Get user liked posts should succeed")
    void getLikedPostsSystemTest() {
        // WHEN & THEN
        given()
                .cookie("AuthToken", authToken)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/2/liked-posts")
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    @DisplayName("Get other user liked posts should fail")
    void getLikedPostsAccessDeniedSystemTest() {
        // WHEN & THEN
        given()
                .cookie("AuthToken", authToken)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/1/liked-posts")
                .then()
                .statusCode(403);
    }
}