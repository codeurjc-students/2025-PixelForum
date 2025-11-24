package es.codeurjc.backend.e2e.server;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.RestAssured;

@Tag("e2e")
@DisplayName("PostServer System Test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = es.codeurjc.backend.BackendApplication.class)
public class PostServerControllerTest {
    
    @LocalServerPort
    private int port;
    
    @BeforeEach
        public void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    public void getPostsSystemTest() {
        given()
        .when()
            .get("/api/posts/")
        .then()
            .statusCode(200)
            .body("content", not(empty()))
            .body("size()", greaterThan(0))
            .body("[0].id", notNullValue());  
    }

    @Test
    public void getPostByIdSystemTest() {
        long id = 1L;
        given()
        .when()
            .get("/api/posts/{id}", id)
        .then()
            .statusCode(200)
            .body("id", equalTo(Math.toIntExact(id)))
            .body("title", equalTo("GTA VI Massive leak"));
    }

}
