package es.codeurjc.backend.e2e.server;

import static io.restassured.RestAssured.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import es.codeurjc.backend.service.UserService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@Tag("e2e")
@DisplayName("Controllers - user not found System Test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserNotFoundE2ETest {

	@LocalServerPort
	private int port;

	@MockitoSpyBean
	private UserService userService;

	private String token;

	@BeforeEach
	void setUp() {
		RestAssured.port = port;
		RestAssured.baseURI = "http://localhost";
		RestAssured.basePath = "";

		// Login to get a valid token
		token = given()
				.contentType(ContentType.JSON)
				.body(Map.of("username", "martin", "password", "user"))
				.when()
				.post("/api/v1/auth/login")
				.then()
				.statusCode(200)
				.extract().cookie("AuthToken");

		// Mock the userService to return empty when searching for any username
		doReturn(Optional.empty()).when(userService).findByUsername(anyString());
	}

	@Test
	@DisplayName("PostRestController should return 404 when the token user does not exist")
	void postControllerTest() {
		given()
				.cookie("AuthToken", token)
				.contentType(ContentType.JSON)
				.body(Map.of("title", "Ghost", "topic", Map.of("id", 1)))
				.when()
				.post("/api/v1/posts")
				.then()
				.statusCode(404);
	}

	@Test
	@DisplayName("ImageRestController should return 404 when the token user does not exist")
	void imageControllerTest() {
		given()
				.cookie("AuthToken", token)
				.when()
				.delete("/api/v1/images/1")
				.then()
				.statusCode(404);
	}

	@Test
	@DisplayName("CommentRestController should return 404 when the token user does not exist")
	void commentControllerTest() {
		given()
				.cookie("AuthToken", token)
				.contentType(ContentType.JSON)
				.body(Map.of("content", "Ghost"))
				.when()
				.post("/api/v1/posts/1/comments")
				.then()
				.statusCode(404);
	}

	@Test
	@DisplayName("UserRestController should return 404 when the token user does not exist")
	void userControllerTest() {
		given()
				.cookie("AuthToken", token)
				.when()
				.get("/api/v1/users/2/details")
				.then()
				.statusCode(404);
	}

	@Test
	@DisplayName("LoginController /me should return 404 when the token user does not exist")
	void loginControllerMeTest() {
		given()
				.cookie("AuthToken", token)
				.when()
				.get("/api/v1/auth/me")
				.then()
				.statusCode(404);
	}
}