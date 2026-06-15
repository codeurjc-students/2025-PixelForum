package es.codeurjc.backend.e2e.client;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

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
@DisplayName("Comment UI System Test")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class CommentUISystemTest {

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
		driver.findElement(By.id("username")).sendKeys(username);
		driver.findElement(By.id("password")).sendKeys(password);
		driver.findElement(By.id("login-button")).click();
		wait.until(ExpectedConditions.urlContains("/posts"));
	}

	private void goToFirstPost() {
		driver.get("https://localhost:" + port + "/posts");
		wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("app-post")));
		driver.findElement(By.cssSelector("app-post")).click();
		wait.until(ExpectedConditions.urlMatches(".*/posts/\\d+$"));
	}

	private void postComment(String content) {
		WebElement textarea = wait
				.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".comment-textarea")));
		textarea.clear();
		textarea.sendKeys(content);

		driver.findElement(By.cssSelector(".submit-btn")).click();

		// Wait until the new comment card appears in the feed
		wait.until(ExpectedConditions.presenceOfElementLocated(By
				.xpath("//app-comment//p[contains(@class,'comment-content') and contains(text(),'" + content + "')]")));
	}

	private WebElement findCommentCard(String content) {
		return driver.findElement(By.xpath(
				"//app-comment[.//p[contains(@class,'comment-content') and contains(text(),'" + content + "')]]"));
	}

	private int getCommentLikes(String content) {
		WebElement count = findCommentCard(content).findElement(By.cssSelector(".like-btn .count"));
		return Integer.parseInt(count.getText().trim());
	}

	// =============== Read ===============

	@Test
	@DisplayName("Post detail page should display comment section")
	void commentSectionVisibleTest() {
		// GIVEN - no login needed
		goToFirstPost();

		// THEN
		WebElement commentSection = wait
				.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("app-comment-list")));
		assertTrue(commentSection.isDisplayed(), "Comment section should be visible on post detail page");
	}

	@Test
	@DisplayName("Unauthenticated user should see login notice instead of comment form")
	void commentFormHiddenWhenNotLoggedInTest() {
		// GIVEN - no login
		goToFirstPost();

		// THEN - login-notice is shown, submit button is absent
		wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".login-notice")));
		assertTrue(driver.findElements(By.cssSelector(".submit-btn")).isEmpty(),
				"Submit button should not be visible for unauthenticated users");
	}

	// =============== Create ===============

	@Test
	@DisplayName("Authenticated user can post a comment")
	void createCommentTest() {
		// GIVEN
		login("martin", "user");
		goToFirstPost();

		// WHEN
		String content = "Selenium comment " + System.currentTimeMillis();
		postComment(content);

		// THEN
		assertTrue(findCommentCard(content).isDisplayed(), "The posted comment should appear in the list");
	}

	// =============== Edit ===============

	@Test
	@DisplayName("Author can edit their own comment")
	void editCommentTest() {
		// GIVEN
		login("martin", "user");
		goToFirstPost();
		String original = "Edit target " + System.currentTimeMillis();
		postComment(original);

		// WHEN - click the edit button
		WebElement editBtn = findCommentCard(original).findElement(By.xpath(".//button[contains(@title,'Edit')]"));
		editBtn.click();

		// The edit-textarea replaces the comment-content paragraph
		String edited = "Edited content " + System.currentTimeMillis();
		WebElement editArea = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".edit-textarea")));
		editArea.clear();
		editArea.sendKeys(edited);

		driver.findElement(By.cssSelector(".btn-save")).click();

		// THEN
		wait.until(ExpectedConditions.presenceOfElementLocated(By
				.xpath("//app-comment//p[contains(@class,'comment-content') and contains(text(),'" + edited + "')]")));
		assertTrue(findCommentCard(edited).isDisplayed(), "Comment should show the updated content");
	}

	// =============== Delete ===============

	@Test
	@DisplayName("Author can delete their own comment")
	void deleteCommentTest() {
		// GIVEN
		login("martin", "user");
		goToFirstPost();
		String content = "Delete me " + System.currentTimeMillis();
		postComment(content);

		// WHEN - click the delete button (bin icon) inside the card
		WebElement deleteBtn = findCommentCard(content).findElement(By.xpath(".//button[contains(@title,'Delete')]"));
		deleteBtn.click();

		// Confirm in the dialog
		WebElement confirmBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("confirm-button")));
		confirmBtn.click();

		// THEN
		wait.until(ExpectedConditions.invisibilityOfElementLocated(By
				.xpath("//app-comment//p[contains(@class,'comment-content') and contains(text(),'" + content + "')]")));
		assertTrue(driver
				.findElements(By.xpath(
						"//app-comment//p[contains(@class,'comment-content') and contains(text(),'" + content + "')]"))
				.isEmpty(), "Deleted comment should no longer appear");
	}

	// =============== Like ===============

	@Test
	@DisplayName("User can like and unlike a comment")
	void toggleCommentLikeTest() {
		// GIVEN - create the comment as martin and save the exact post URL
		login("martin", "user");
		goToFirstPost();
		String postUrl = driver.getCurrentUrl();
		String content = "Like this " + System.currentTimeMillis();
		postComment(content);

		// WHEN - Like it as robert
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("logout-button")));
		driver.manage().deleteAllCookies();
		driver.navigate().refresh();
		login("robert", "user");
		driver.get(postUrl);
		wait.until(ExpectedConditions.presenceOfElementLocated(By
				.xpath("//app-comment//p[contains(@class,'comment-content') and contains(text(),'" + content + "')]")));

		int likesBefore = getCommentLikes(content);

		WebElement likeBtn = findCommentCard(content).findElement(By.cssSelector(".like-btn"));
		likeBtn.click();

		// THEN Like added
		wait.until(d -> getCommentLikes(content) == likesBefore + 1);
		assertEquals(likesBefore + 1, getCommentLikes(content), "Likes should increase by 1");

		// Unlike
		likeBtn = findCommentCard(content).findElement(By.cssSelector(".like-btn"));
		likeBtn.click();
		wait.until(d -> getCommentLikes(content) == likesBefore);
		assertEquals(likesBefore, getCommentLikes(content), "Likes should return to original value");
	}
}