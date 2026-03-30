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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Tag("e2e")
@DisplayName("ImageController System Test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ImageControllerE2ETest {

    @LocalServerPort
    private int port;

    private String authToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = "/api/v1/images";

        // LOGIN
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "martin");
        loginData.put("password", "user");

        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(loginData)
                .post("http://localhost:" + port + "/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        authToken = loginResponse.getCookie("AuthToken");
    }

    // ------------------- UPLOAD -------------------

    @Test
    @DisplayName("Upload image should succeed")
    void uploadImageSystemTest() throws IOException {
        InputStream is = getClass().getResourceAsStream("/test.png");
        byte[] bytes = is.readAllBytes();

        given()
                .cookie("AuthToken", authToken)
                .contentType("multipart/form-data")
                .multiPart("files", "test.png", bytes, "image/png")
                .when()
                .post()
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    @DisplayName("Upload empty files should fail")
    void uploadEmptySystemTest() {
        given()
                .cookie("AuthToken", authToken)
                .when()
                .post()
                .then()
                .statusCode(500);
    }

    // ------------------- GET -------------------

    @Test
    @DisplayName("Get image should return binary data")
    void getImageSystemTest() throws IOException {
        // STEP 1: upload image
        InputStream is = getClass().getResourceAsStream("/test.png");
        byte[] bytes = is.readAllBytes();

        Response uploadResponse = given()
                .cookie("AuthToken", authToken)
                .multiPart("files", "test.png", bytes, "image/png")
                .post();

        Long imageId = uploadResponse.jsonPath().getLong("[0]");

        // STEP 2: get image
        given()
                .when()
                .get("/" + imageId)
                .then()
                .statusCode(200)
                .contentType(anyOf(
                        containsString("image/png"),
                        containsString("image/jpeg")));
    }

    @Test
    @DisplayName("Get non-existing image should fail")
    void getImageNotFoundSystemTest() {
        given()
                .when()
                .get("/99999")
                .then()
                .statusCode(500);
    }

    // ------------------- DELETE -------------------

    @Test
    @DisplayName("Delete own image should succeed")
    void deleteImageSystemTest() throws IOException {
        // STEP 1: upload image
        InputStream is = getClass().getResourceAsStream("/test.png");
        byte[] bytes = is.readAllBytes();

        Response uploadResponse = given()
                .cookie("AuthToken", authToken)
                .multiPart("files", "test.png", bytes, "image/png")
                .post();

        Long imageId = uploadResponse.jsonPath().getLong("[0]");

        // STEP 2: delete image
        given()
                .cookie("AuthToken", authToken)
                .when()
                .delete("/" + imageId)
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("Delete image of another user should fail")
    void deleteImageOtherUserSystemTest() throws IOException {
        // STEP 1: login as other user
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "robert");
        loginData.put("password", "user");

        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(loginData)
                .post("http://localhost:" + port + "/api/v1/auth/login");

        String otherToken = loginResponse.getCookie("AuthToken");

        // STEP 2: robert uploads image
        InputStream is = getClass().getResourceAsStream("/test.png");
        byte[] bytes = is.readAllBytes();

        Response uploadResponse = given()
                .cookie("AuthToken", otherToken)
                .multiPart("files", "test.png", bytes, "image/png")
                .post();

        Long imageId = uploadResponse.jsonPath().getLong("[0]");

        // STEP 3: martin tries to delete
        given()
                .cookie("AuthToken", authToken)
                .when()
                .delete("/" + imageId)
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Delete image without auth should fail")
    void deleteImageWithoutAuthSystemTest() {
        given()
                .when()
                .delete("/1")
                .then()
                .statusCode(401);
    }
}