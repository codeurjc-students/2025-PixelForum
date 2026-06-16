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
@DisplayName("CommentController System Test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CommentControllerE2ETest {

	@LocalServerPort
	private int port;

	private String authToken;
	private Long testPostId;

	@BeforeEach
	void setUp() {
		RestAssured.port = port;
		RestAssured.baseURI = "http://localhost";
		RestAssured.basePath = "";

		// Login as martin
		Map<String, String> loginData = new HashMap<>();
		loginData.put("username", "martin");
		loginData.put("password", "user");

		Response loginResponse = given()
				.contentType(ContentType.JSON)
				.body(loginData)
				.when()
				.post("http://localhost:" + port + "/api/v1/auth/login")
				.then()
				.statusCode(200)
				.extract().response();

		authToken = loginResponse.getCookie("AuthToken");

		Map<String, Object> topic = new HashMap<>();
		topic.put("id", 1);

		Map<String, Object> postBody = new HashMap<>();
		postBody.put("title", "Test post");
		postBody.put("content", "Created for comment tests");
		postBody.put("topic", topic);

		Response postResponse = given()
				.cookie("AuthToken", authToken)
				.contentType(ContentType.JSON)
				.body(postBody)
				.when()
				.post("http://localhost:" + port + "/api/v1/posts")
				.then()
				.statusCode(201)
				.extract()
				.response();

		testPostId = postResponse.jsonPath().getLong("id");
	}

	private String commentsPath() {
		return "/api/v1/posts/" + testPostId + "/comments";
	}

	private Long createComment(String authCookie, String content) {
		Map<String, String> body = new HashMap<>();
		body.put("content", content);

		return given()
				.cookie("AuthToken", authCookie)
				.contentType(ContentType.JSON)
				.body(body)
				.when()
				.post(commentsPath())
				.then()
				.statusCode(201)
				.extract().jsonPath().getLong("id");
	}

	// =============== GET comments ===============

	@Test
	@DisplayName("Get post comments should return a page of comments")
	void getPostCommentsSystemTest() {
		// WHEN & THEN
		given()
				.when()
				.get(commentsPath())
				.then()
				.statusCode(200)
				.body("content", notNullValue())
				.body("totalElements", greaterThanOrEqualTo(0));
	}

	@Test
	@DisplayName("Get comment by id should return the comment")
	void getCommentByIdSystemTest() {
		// GIVEN
		long commentId = createComment(authToken, "Comment for get test");

		// WHEN & THEN
		given()
				.cookie("AuthToken", authToken)
				.when()
				.get(commentsPath() + "/" + commentId)
				.then()
				.statusCode(200)
				.body("id", equalTo((int) commentId))
				.body("content", equalTo("Comment for get test"))
				.body("author.username", equalTo("martin"));
	}

	@Test
	@DisplayName("Get non-existing comment should return 404")
	void getCommentNotFoundSystemTest() {
		// WHEN & THEN
		given()
				.when()
				.get(commentsPath() + "/-1")
				.then()
				.statusCode(404);
	}

	// =============== POST (create) ===============

	@Test
	@DisplayName("Create comment should succeed with authentication")
	void createCommentSystemTest() {
		// GIVEN
		Map<String, String> body = new HashMap<>();
		body.put("content", "E2E test comment");

		// WHEN & THEN
		given()
				.cookie("AuthToken", authToken)
				.contentType(ContentType.JSON)
				.body(body)
				.when()
				.post(commentsPath())
				.then()
				.statusCode(201)
				.body("content", equalTo("E2E test comment"));
	}

	@Test
	@DisplayName("Create comment without authentication should fail")
	void createCommentWithoutAuthSystemTest() {
		// GIVEN
		Map<String, String> body = new HashMap<>();
		body.put("content", "Unauthorized comment");

		// WHEN & THEN
		given()
				.contentType(ContentType.JSON)
				.body(body)
				.when()
				.post(commentsPath())
				.then()
				.statusCode(401);
	}

	@Test
	@DisplayName("Create comment on non-existing post should return 404")
	void createCommentPostNotFoundSystemTest() {
		// GIVEN
		Map<String, String> body = new HashMap<>();
		body.put("content", "Orphan comment");

		// WHEN & THEN
		given()
				.cookie("AuthToken", authToken)
				.contentType(ContentType.JSON)
				.body(body)
				.when()
				.post("/api/v1/posts/-1/comments")
				.then()
				.statusCode(404);
	}

	// =============== PUT (update) ===============

	@Test
	@DisplayName("Update own comment should succeed")
	void updateCommentSystemTest() {
		// GIVEN
		Long commentId = createComment(authToken, "Original content");

		Map<String, String> updateBody = new HashMap<>();
		updateBody.put("content", "Updated content");

		// WHEN & THEN
		given()
				.cookie("AuthToken", authToken)
				.contentType(ContentType.JSON)
				.body(updateBody)
				.when()
				.put(commentsPath() + "/" + commentId)
				.then()
				.statusCode(200)
				.body("content", equalTo("Updated content"));
	}

	@Test
	@DisplayName("Update other user's comment should fail")
	void updateCommentAccessDeniedSystemTest() {
		// GIVEN
		Long commentId = createComment(authToken, "Martin's comment");

		// Login as robert
		Map<String, String> loginData = new HashMap<>();
		loginData.put("username", "robert");
		loginData.put("password", "user");

		String robertToken = given()
				.contentType(ContentType.JSON)
				.body(loginData)
				.when()
				.post("/api/v1/auth/login")
				.then()
				.statusCode(200)
				.extract().response().getCookie("AuthToken");

		Map<String, String> updateBody = new HashMap<>();
		updateBody.put("content", "Hijacked content");

		// WHEN & THEN
		given()
				.cookie("AuthToken", robertToken)
				.contentType(ContentType.JSON)
				.body(updateBody)
				.when()
				.put(commentsPath() + "/" + commentId)
				.then()
				.statusCode(403);
	}

	@Test
	@DisplayName("Update non-existing comment should return 404")
	void updateCommentNotFoundSystemTest() {
		// GIVEN
		Map<String, String> body = new HashMap<>();
		body.put("content", "Does not matter");

		// WHEN & THEN
		given()
				.cookie("AuthToken", authToken)
				.contentType(ContentType.JSON)
				.body(body)
				.when()
				.put(commentsPath() + "/-1")
				.then()
				.statusCode(404);
	}

	// =============== DELETE ===============

	@Test
	@DisplayName("Delete own comment should succeed")
	void deleteCommentSystemTest() {
		// GIVEN
		Long commentId = createComment(authToken, "Comment to delete");

		// WHEN & THEN
		given()
				.cookie("AuthToken", authToken)
				.when()
				.delete(commentsPath() + "/" + commentId)
				.then()
				.statusCode(204);

		// Verify it no longer exists
		given()
				.when()
				.get(commentsPath() + "/" + commentId)
				.then()
				.statusCode(404);
	}

	@Test
	@DisplayName("Delete other user's comment should fail")
	void deleteCommentAccessDeniedSystemTest() {
		// GIVEN
		Long commentId = createComment(authToken, "Protected comment");

		Map<String, String> loginData = new HashMap<>();
		loginData.put("username", "robert");
		loginData.put("password", "user");

		String robertToken = given()
				.contentType(ContentType.JSON)
				.body(loginData)
				.when()
				.post("/api/v1/auth/login")
				.then()
				.statusCode(200)
				.extract().response().getCookie("AuthToken");

		// WHEN & THEN
		given()
				.cookie("AuthToken", robertToken)
				.when()
				.delete(commentsPath() + "/" + commentId)
				.then()
				.statusCode(403);
	}

	@Test
	@DisplayName("Delete comment without authentication should fail")
	void deleteCommentWithoutAuthSystemTest() {
		// GIVEN
		Long commentId = createComment(authToken, "Comment without auth delete");

		// WHEN & THEN
		given()
				.when()
				.delete(commentsPath() + "/" + commentId)
				.then()
				.statusCode(401);
	}

	// =============== toggleLike ===============

	@Test
	@DisplayName("Toggle like should add like to comment")
	void toggleLikeAddSystemTest() {
		// GIVEN
		Long commentId = createComment(authToken, "Comment to like");

		// WHEN & THEN
		given()
				.cookie("AuthToken", authToken)
				.when()
				.post(commentsPath() + "/" + commentId + "/like")
				.then()
				.statusCode(200)
				.body("likes", equalTo(1));
	}

	@Test
	@DisplayName("Toggle like should remove like when already liked")
	void toggleLikeRemoveSystemTest() {
		// GIVEN
		Long commentId = createComment(authToken, "Comment to unlike");

		// First like
		given()
				.cookie("AuthToken", authToken)
				.when()
				.post(commentsPath() + "/" + commentId + "/like")
				.then()
				.statusCode(200);

		// WHEN & THEN - second toggle removes the like
		given()
				.cookie("AuthToken", authToken)
				.when()
				.post(commentsPath() + "/" + commentId + "/like")
				.then()
				.statusCode(200)
				.body("likes", equalTo(0));
	}

	@Test
	@DisplayName("Toggle like without authentication should fail")
	void toggleLikeWithoutAuthSystemTest() {
		// GIVEN
		Long commentId = createComment(authToken, "Comment for unauth like");

		// WHEN & THEN
		given()
				.when()
				.post(commentsPath() + "/" + commentId + "/like")
				.then()
				.statusCode(401);
	}
}