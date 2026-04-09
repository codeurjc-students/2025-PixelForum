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
@DisplayName("PostController System Test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PostControllerE2ETest {

	@LocalServerPort
	private int port;

	private String authToken;

	@BeforeEach
	void setUp() {
		RestAssured.port = port;
		RestAssured.baseURI = "http://localhost";
		RestAssured.basePath = "/api/v1/posts";

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
	@DisplayName("Get all posts system test")
	void getAllPostsSystemTest() {
		// WHEN & THEN
		given()
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("content", not(empty()))
				.body("size()", greaterThan(0));
	}

	@Test
	@DisplayName("Create post should succeed with authentication")
	void createPostSystemTest() {
		// GIVEN
		Map<String, Object> topic = new HashMap<>();
		topic.put("id", 1);

		Map<String, Object> postData = new HashMap<>();
		postData.put("title", "Test Post");
		postData.put("content", "Post created in E2E test");
		postData.put("topic", topic);

		// WHEN & THEN
		given()
				.cookie("AuthToken", authToken)
				.contentType(ContentType.JSON)
				.body(postData)
				.when()
				.post()
				.then()
				.statusCode(201)
				.body("title", equalTo("Test Post"))
				.body("id", notNullValue())
				.body("author.username", equalTo("martin"));
	}

	@Test
	@DisplayName("Get post by id should return the created post")
	void getPostByIdSystemTest() {
		// GIVEN
		Map<String, Object> topic = new HashMap<>();
		topic.put("id", 1);

		Map<String, Object> postData = new HashMap<>();
		postData.put("title", "Post for get");
		postData.put("topic", topic);

		Response createResponse = given()
				.cookie("AuthToken", authToken)
				.contentType(ContentType.JSON)
				.body(postData)
				.when()
				.post()
				.then()
				.statusCode(201)
				.extract().response();

		Long postId = createResponse.jsonPath().getLong("id");

		// WHEN & THEN
		given()
				.when()
				.get("/" + postId)
				.then()
				.statusCode(200)
				.body("id", equalTo(postId.intValue()))
				.body("title", equalTo("Post for get"));
	}

	@Test
	@DisplayName("Update own post should succeed")
	void updatePostSystemTest() {
		// GIVEN
		Map<String, Object> topic = new HashMap<>();
		topic.put("id", 1);

		Map<String, Object> postData = new HashMap<>();
		postData.put("title", "Original Title");
		postData.put("topic", topic);

		Response createResponse = given()
				.cookie("AuthToken", authToken)
				.contentType(ContentType.JSON)
				.body(postData)
				.when()
				.post()
				.then()
				.statusCode(201)
				.extract().response();

		Long postId = createResponse.jsonPath().getLong("id");

		Map<String, Object> updateData = new HashMap<>();
		updateData.put("title", "Updated Title");
		updateData.put("topic", topic);

		// WHEN & THEN
		given()
				.cookie("AuthToken", authToken)
				.contentType(ContentType.JSON)
				.body(updateData)
				.when()
				.put("/" + postId)
				.then()
				.statusCode(200)
				.body("title", equalTo("Updated Title"));
	}

	@Test
	@DisplayName("Delete own post should succeed")
	void deletePostSystemTest() {
		// GIVEN
		Map<String, Object> topic = new HashMap<>();
		topic.put("id", 1);

		Map<String, Object> postData = new HashMap<>();
		postData.put("title", "Post to delete");
		postData.put("topic", topic);

		Response createResponse = given()
				.cookie("AuthToken", authToken)
				.contentType(ContentType.JSON)
				.body(postData)
				.when()
				.post()
				.then()
				.statusCode(201)
				.extract().response();

		Long postId = createResponse.jsonPath().getLong("id");

		// WHEN & THEN
		given()
				.cookie("AuthToken", authToken)
				.when()
				.delete("/" + postId)
				.then()
				.statusCode(204);
	}

	@Test
	@DisplayName("Delete other user's post should fail")
	void deleteOtherUserPostSystemTest() {

		// STEP 1: Login as user
		Map<String, String> userLogin = new HashMap<>();
		userLogin.put("username", "robert");
		userLogin.put("password", "user");

		Response userLoginResponse = given()
				.contentType(ContentType.JSON)
				.body(userLogin)
				.when()
				.post("http://localhost:" + port + "/api/v1/auth/login")
				.then()
				.statusCode(200)
				.extract().response();

		String userToken = userLoginResponse.getCookie("AuthToken");

		// STEP 2: User creates a post
		Map<String, Object> topic = new HashMap<>();
		topic.put("id", 1);

		Map<String, Object> postData = new HashMap<>();
		postData.put("title", "User Post");
		postData.put("topic", topic);

		Response createResponse = given()
				.cookie("AuthToken", userToken)
				.contentType(ContentType.JSON)
				.body(postData)
				.when()
				.post()
				.then()
				.statusCode(201)
				.extract().response();

		Long postId = createResponse.jsonPath().getLong("id");

		// STEP 3: Other user tries to delete the post
		given()
				.cookie("AuthToken", authToken)
				.when()
				.delete("/" + postId)
				.then()
				.statusCode(403);
	}

	@Test
	@DisplayName("Create post without authentication should fail")
	void createPostWithoutAuthSystemTest() {
		// GIVEN
		Map<String, Object> topic = new HashMap<>();
		topic.put("id", 1);

		Map<String, Object> postData = new HashMap<>();
		postData.put("title", "Unauthorized post");
		postData.put("topic", topic);

		// WHEN & THEN
		given()
				.contentType(ContentType.JSON)
				.body(postData)
				.when()
				.post()
				.then()
				.statusCode(401);
	}

	@Test
	@DisplayName("Toggle like should add like to post")
	void toggleLikeAddSystemTest() {
		// GIVEN
		Map<String, Object> topic = new HashMap<>();
		topic.put("id", 1);

		Map<String, Object> postData = new HashMap<>();
		postData.put("title", "Like test");
		postData.put("topic", topic);

		Response createResponse = given()
				.cookie("AuthToken", authToken)
				.contentType(ContentType.JSON)
				.body(postData)
				.when()
				.post()
				.then()
				.statusCode(201)
				.extract().response();

		Long postId = createResponse.jsonPath().getLong("id");

		// WHEN & THEN
		given()
				.cookie("AuthToken", authToken)
				.when()
				.post("/" + postId + "/like")
				.then()
				.statusCode(200)
				.body("likes", equalTo(1));
	}

	@Test
	@DisplayName("Toggle like should remove like when already liked")
	void toggleLikeRemoveSystemTest() {
		// GIVEN
		Map<String, Object> topic = new HashMap<>();
		topic.put("id", 1);

		Map<String, Object> postData = new HashMap<>();
		postData.put("title", "Unlike test");
		postData.put("topic", topic);

		Response createResponse = given()
				.cookie("AuthToken", authToken)
				.contentType(ContentType.JSON)
				.body(postData)
				.when()
				.post()
				.then()
				.statusCode(201)
				.extract().response();

		Long postId = createResponse.jsonPath().getLong("id");

		// First like
		given()
				.cookie("AuthToken", authToken)
				.when()
				.post("/" + postId + "/like")
				.then()
				.statusCode(200);

		// WHEN & THEN - second like should remove the like
		given()
				.cookie("AuthToken", authToken)
				.when()
				.post("/" + postId + "/like")
				.then()
				.statusCode(200)
				.body("likes", equalTo(0));
	}

	@Test
	@DisplayName("Toggle like without authentication should fail")
	void toggleLikeWithoutAuthSystemTest() {
		// GIVEN
		Map<String, Object> topic = new HashMap<>();
		topic.put("id", 1);

		Map<String, Object> postData = new HashMap<>();
		postData.put("title", "No auth like");
		postData.put("topic", topic);

		Response createResponse = given()
				.cookie("AuthToken", authToken)
				.contentType(ContentType.JSON)
				.body(postData)
				.when()
				.post()
				.then()
				.statusCode(201)
				.extract().response();

		Long postId = createResponse.jsonPath().getLong("id");

		// WHEN & THEN
		given()
				.when()
				.post("/" + postId + "/like")
				.then()
				.statusCode(401);
	}
}