package es.codeurjc.backend.e2e.client;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@Tag("selenium")
@DisplayName("Post UI System Test")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class PostUISystemTest {

    @LocalServerPort
    int port;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        port = 4200;
        String browser = System.getenv("BROWSER");
        if ("firefox".equalsIgnoreCase(browser)) {
            FirefoxOptions options = new FirefoxOptions();
            options.addArguments("--headless");
            options.setAcceptInsecureCerts(true);
            driver = new FirefoxDriver(options);
        } else if ("edge".equalsIgnoreCase(browser)) {
            EdgeOptions options = new EdgeOptions();
            options.addArguments("--headless");
            options.setAcceptInsecureCerts(true);
            driver = new EdgeDriver(options);
        } else {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--ignore-certificate-errors");
            options.setAcceptInsecureCerts(true);
            driver = new ChromeDriver(options);
        }
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void login(String username, String password) {
        driver.get("https://localhost:" + port + "/login");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-button")));

        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));
        WebElement loginButton = driver.findElement(By.id("login-button"));

        usernameField.sendKeys(username);
        passwordField.sendKeys(password);
        loginButton.click();

        wait.until(ExpectedConditions.urlContains("/posts"));
    }

    @Test
    @DisplayName("List posts page displays posts")
    void listPostsTest() {
        driver.get("https://localhost:" + port + "/posts");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("app-post")));
        List<WebElement> posts = driver.findElements(By.cssSelector("app-post"));
        assertTrue(posts.size() > 0, "There should be at least one post displayed");
    }

    @Test
    @DisplayName("Access post detail page")
    void postDetailTest() {
        driver.get("https://localhost:" + port + "/posts");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("app-post")));

        List<WebElement> posts = driver.findElements(By.cssSelector("app-post"));
        WebElement firstPost = posts.get(0);
        firstPost.click();

        wait.until(ExpectedConditions.urlMatches(".*/posts/\\d+$"));
        assertTrue(driver.getCurrentUrl().contains("/posts/"));
    }

    @Test
    @DisplayName("Create a new post successfully")
    void createPostTest() {
        login("admin", "admin0");
        driver.get("https://localhost:" + port + "/create-post");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));

        // Fill form
        driver.findElement(By.id("title")).sendKeys("Selenium Test Post");
        driver.findElement(By.id("content")).sendKeys("This post was created by Selenium E2E test.");
        driver.findElement(By.id("topic-search")).sendKeys("GTA V");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".topic-option")));
        driver.findElement(By.cssSelector(".topic-option")).click();

        driver.findElement(By.id("save-post-button")).click();

        // Wait for navigation to detail page
        wait.until(ExpectedConditions.urlMatches(".*/posts/\\d+$"));

        // Verify title in detail page
        WebElement postTitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("post-title")));
        assertEquals("Selenium Test Post", postTitle.getText(), "The post should be created");
    }

    @Test
    @DisplayName("Unauthorized user cannot access create post page")
    void unauthorizedCreatePostTest() {
        driver.get("https://localhost:" + port + "/create-post");

        wait.until(ExpectedConditions.urlContains("/login"));

        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    @DisplayName("Edit an existing post")
    void editPostTest() {
        login("admin", "admin0");

        // Navigate to last post
        driver.get("https://localhost:" + port + "/posts");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("app-post")));

        List<WebElement> posts = driver.findElements(By.cssSelector("app-post"));
        WebElement firstPost = posts.get(0);
        firstPost.click();

        wait.until(ExpectedConditions.urlMatches(".*/posts/\\d+$"));

        // Click edit button
        WebElement editButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("edit-btn")));
        editButton.click();

        wait.until(ExpectedConditions.urlMatches(".*/posts/\\d+/edit$"));

        WebElement titleInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));
        titleInput.clear();
        titleInput.sendKeys("Edited Selenium Post");

        driver.findElement(By.id("save-post-button")).click();

        wait.until(ExpectedConditions.urlMatches(".*/posts/\\d+$"));
        WebElement postTitle = driver.findElement(By.id("post-title"));
        assertEquals("Edited Selenium Post", postTitle.getText(), "The title should change to the edited one");
    }

    @Test
    @DisplayName("Edit an existing post without authorization should fail")
    void editPostUnauthorizedTest() {
        login("martin", "user");
        driver.get("https://localhost:" + port + "/posts");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("app-post")));

        // Get post
        List<WebElement> posts = driver.findElements(By.cssSelector("app-post"));
        WebElement targetPost = null;
        String originalContent = null;
        String postId = null;

        for (WebElement post : posts) {
            String author = post.findElement(By.id("post-author-username")).getText();
            if (!author.equals("martin")) {
                targetPost = post;
                originalContent = post.findElement(By.id("post-title")).getText();
                break;
            }
        }

        if (targetPost == null)
            fail("No post from another user found");

        targetPost.click();
        wait.until(ExpectedConditions.urlMatches(".*/posts/\\d+$"));

        String url = driver.getCurrentUrl();
        postId = url.substring(url.lastIndexOf("/") + 1);

        // Open edit page
        driver.get("https://localhost:" + port + "/posts/" + postId + "/edit");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("save-post-button")));

        // Try to edit
        WebElement titleInput = driver.findElement(By.id("title"));
        titleInput.clear();
        titleInput.sendKeys("Attempted Edit");
        driver.findElement(By.id("save-post-button")).click();

        // Wait for error page
        wait.until(ExpectedConditions.urlContains("/error"));
        assertTrue(driver.getCurrentUrl().contains("/error"));

        // Verify post content is unchanged
        driver.get("https://localhost:" + port + "/posts/" + postId);
        WebElement postTitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("post-title")));
        assertEquals(originalContent, postTitle.getText(), "The post title should not have changed");
    }

    @Test
    @DisplayName("Delete a post")
    void deletePostTest() {
        login("admin", "admin0");

        // Navigate to last post
        driver.get("https://localhost:" + port + "/posts");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("app-post")));

        List<WebElement> posts = driver.findElements(By.cssSelector("app-post"));
        WebElement firstPost = posts.get(0);
        firstPost.click();

        wait.until(ExpectedConditions.urlMatches(".*/posts/\\d+$"));
        // Save id
        String url = driver.getCurrentUrl();
        String postId = url.substring(url.lastIndexOf("/") + 1);

        // Click delete button
        WebElement deleteButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("delete-btn")));
        deleteButton.click();

        // Confirm dialog
        WebElement confirmButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("confirm-button")));
        confirmButton.click();

        // Back to posts list
        wait.until(ExpectedConditions.urlContains("/posts"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("app-post")));

        // Verify post is deleted
        driver.get("https://localhost:" + port + "/posts/" + postId);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("error-code")));
        WebElement errorCode = driver.findElement(By.id("error-code"));
        String code = errorCode.getText();
        assertEquals(code, "500", "The post should be deleted and not accessible anymore");
    }
}