package es.codeurjc.backend.e2e.server;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
@DisplayName("AuthController System Test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LoginControllerE2ETest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = "/api/v1/auth";
    }

    @Test
    @DisplayName("Login should succeed with valid credentials and return cookies")
    void loginSystemTest() {
        // GIVEN
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "admin");
        loginData.put("password", "admin0");

        // WHEN & THEN
        given()
            .contentType(ContentType.JSON)
            .body(loginData)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .body("status", equalTo("SUCCESS"))
            .cookie("AuthToken")
            .cookie("RefreshToken");
    }

    @Test
    @DisplayName("Login should fail with invalid credentials")
    void loginInvalidSystemTest() {
        // GIVEN
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "invalid");
        loginData.put("password", "wrong");

        // WHEN & THEN
        given()
            .contentType(ContentType.JSON)
            .body(loginData)
        .when()
            .post("/login")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("Complete auth flow should work correctly")
    void completeAuthFlowSystemTest() {
        // STEP 1: Login
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "admin");
        loginData.put("password", "admin0");

        Response loginResponse = 
            given()
                .contentType(ContentType.JSON)
                .body(loginData)
            .when()
                .post("/login")
            .then()
                .statusCode(200)
                .extract().response();

        String authToken = loginResponse.getCookie("AuthToken");
        String refreshToken = loginResponse.getCookie("RefreshToken");

        // STEP 2: Get user protected info
        given()
            .cookie("AuthToken", authToken)
        .when()
            .get("/me")
        .then()
            .statusCode(200)
            .body("username", equalTo("admin"));

        // STEP 3: Refresh token
        given()
            .cookie("RefreshToken", refreshToken)
        .when()
            .post("/refresh")
        .then()
            .statusCode(200)
            .body("status", equalTo("SUCCESS"))
            .cookie("AuthToken");

        // STEP 4: Logout
        given()
            .cookie("AuthToken", authToken)
        .when()
            .post("/logout")
        .then()
            .statusCode(200)
            .body("status", equalTo("SUCCESS"))
            .cookie("AuthToken", "")
            .cookie("RefreshToken", "");
    }

    @Test
    @DisplayName("Protected endpoint without auth system test")
    void protectedWithoutAuthSystemTest() {
        given()
        .when()
            .get("http://localhost:" + port + "/api/v1/auth/me")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("Refresh with valid token should return new access token")
    void refreshValidTokenSystemTest() {
        // STEP 1: Login
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "admin");
        loginData.put("password", "admin0");

        Response loginResponse = 
            given()
                .contentType(ContentType.JSON)
                .body(loginData)
            .when()
                .post("/login")
            .then()
                .statusCode(200)
                .extract().response();

        String refreshToken = loginResponse.getCookie("RefreshToken");

        // STEP 2: post refresh
        Response refreshResponse =
            given()
                .cookie("RefreshToken", refreshToken)
            .when()
                .post("/refresh")
            .then()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"))
                .cookie("AuthToken")
                .cookie("RefreshToken")
                .extract().response();

        String newAuthToken = refreshResponse.getCookie("AuthToken");
        assertNotNull(newAuthToken, "New AuthToken should not be null");
    }

    @Test
    @DisplayName("Refresh with invalid token should fail")
    void refreshInvalidTokenSystemTest() {
        given()
            .cookie("RefreshToken", "invalid-token")
        .when()
            .post("/refresh")
        .then()
            .statusCode(401)
            .body("status", equalTo("FAILURE"));
    }
}