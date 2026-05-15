package es.codeurjc.backend.e2e.client;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
@DisplayName("User UI System Test")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class UserUISystemTest {

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

    // =============== View Profile ===============

    @Test
    @DisplayName("View own profile should display user information")
    void viewOwnProfileTest() {
        // GIVEN
        login("martin", "user");

        // WHEN
        driver.get("https://localhost:" + port + "/users/2");

        // THEN
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".username")));
        WebElement usernameElement = driver.findElement(By.cssSelector(".username"));
        assertEquals("martin", usernameElement.getText(), "Username should be martin");

        // Verify edit button is visible for own profile
        WebElement editButton = driver.findElement(By.cssSelector(".edit-btn"));
        assertTrue(editButton.isDisplayed());
    }

    @Test
    @DisplayName("View other user profile should not show edit buttons")
    void viewOtherUserProfileTest() {
        // GIVEN
        login("martin", "user");

        // WHEN
        driver.get("https://localhost:" + port + "/users/1");

        // THEN
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".username")));

        // Edit and Delete buttons should not be visible for other users
        assertTrue(driver.findElements(By.cssSelector(".edit-btn")).isEmpty() ||
                !driver.findElement(By.cssSelector(".edit-btn")).isDisplayed());
    }

    // =============== Tabs Navigation ===============

    @Test
    @DisplayName("Admin should see Likes tab")
    void adminViewLikedPostsTabTest() {
        // GIVEN
        login("admin", "admin0");

        // WHEN
        driver.get("https://localhost:" + port + "/users/2");

        // THEN - Likes tab should be visible
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".tabs-header")));
        WebElement likesTabs = driver.findElements(By.cssSelector(".tab-btn")).stream()
                .filter(tab -> tab.getText().contains("Likes")).findFirst().orElse(null);

        assertNotNull(likesTabs, "Likes tab should be visible for own profile");
    }

    // =============== Registration (Create User) ===============

    @Test
    @DisplayName("Register new user should succeed")
    void registerNewUserTest() {
        // GIVEN
        driver.get("https://localhost:" + port + "/register");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));

        String uniqueUsername = "testuser" + System.currentTimeMillis();
        String uniqueEmail = "test" + System.currentTimeMillis() + "@example.com";

        // WHEN
        WebElement usernameInput = driver.findElement(By.id("username"));
        WebElement emailInput = driver.findElement(By.id("email"));
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement confirmPasswordInput = driver.findElement(By.id("confirmPassword"));
        WebElement bioInput = driver.findElement(By.id("bio"));

        usernameInput.sendKeys(uniqueUsername);
        emailInput.sendKeys(uniqueEmail);
        passwordInput.sendKeys("password123");
        confirmPasswordInput.sendKeys("password123");
        bioInput.sendKeys("New user bio from Selenium");

        WebElement registerButton = driver.findElement(By.id("register-button"));
        registerButton.click();

        // THEN
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"), "Should redirect to login after successful registration");
    }

    @Test
    @DisplayName("Register with existing username should show error")
    void registerDuplicateUsernameTest() {
        // GIVEN
        driver.get("https://localhost:" + port + "/register");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));

        // WHEN
        driver.findElement(By.id("username")).sendKeys("martin");
        driver.findElement(By.id("email")).sendKeys("unique" + System.currentTimeMillis() + "@example.com");
        driver.findElement(By.id("password")).sendKeys("password123");
        driver.findElement(By.id("confirmPassword")).sendKeys("password123");

        WebElement registerButton = driver.findElement(By.id("register-button"));
        registerButton.click();

        // THEN
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".server-error")));
        WebElement errorMessage = driver.findElement(By.cssSelector(".server-error"));
        assertTrue(errorMessage.isDisplayed(), "Error message should be displayed");
    }

    // =============== Edit Profile ===============

    @Test
    @DisplayName("Edit profile bio should update successfully")
    void editProfileBioTest() {
        // GIVEN
        login("martin", "user");
        driver.get("https://localhost:" + port + "/users/2");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".edit-btn")));

        // WHEN
        WebElement editButton = driver.findElement(By.cssSelector(".edit-btn"));
        editButton.click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("mat-input-1")));

        // Wait for the form to load and find the bio field
        WebElement bioInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("textarea[formControlName='bio']")));

        bioInput.clear();
        bioInput.sendKeys("Updated bio from Selenium test");

        // Find and click submit button
        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[type='submit'].submit-button")));
        submitButton.click();

        // THEN - Verify redirect back to profile
        wait.until(ExpectedConditions.urlContains("/users/2"));
        assertTrue(driver.getCurrentUrl().contains("/users/2"));
    }

    // =============== Profile Picture ===============

    @Test
    @DisplayName("Upload profile picture should succeed")
    void uploadProfilePictureTest() {
        // GIVEN
        login("martin", "user");
        driver.get("https://localhost:" + port + "/users/2");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".profile-picture")));

        // WHEN - Click on profile picture to upload
        WebElement profilePictureDiv = driver.findElement(By.cssSelector(".profile-picture"));
        profilePictureDiv.click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("avatarFileInput")));

        WebElement fileInput = driver.findElement(By.id("avatarFileInput"));
        fileInput.sendKeys(System.getProperty("user.dir") + "/src/test/resources/test.png");

        // THEN - Avatar should be loaded
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".profile-picture img")));
        WebElement avatar = driver.findElement(By.cssSelector(".profile-picture img"));
        assertTrue(avatar.isDisplayed());
    }

    // =============== Change Password ===============

    @Test
    @DisplayName("Change password should succeed")
    void changePasswordTest() {
        // GIVEN
        login("daniel", "user");
        driver.get("https://localhost:" + port + "/users/4");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".edit-btn")));

        // WHEN
        WebElement editButton = driver.findElement(By.cssSelector(".edit-btn"));
        editButton.click();

        // Wait for tabs and click on password tab
        WebElement passwordTab = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("password-tab")));
        passwordTab.click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[formControlName='oldPassword']")));

        // Fill password fields
        driver.findElement(By.cssSelector("input[formControlName='oldPassword']")).sendKeys("user");

        String newPassword = "newPassword" + System.currentTimeMillis();
        driver.findElement(By.cssSelector("input[formControlName='newPassword']")).sendKeys(newPassword);
        driver.findElement(By.cssSelector("input[formControlName='confirmPassword']")).sendKeys(newPassword);

        // Submit form
        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".submit-button")));
        submitButton.click();

        // THEN
        wait.until(ExpectedConditions.urlContains("/users/4"));
        assertTrue(driver.getCurrentUrl().contains("/users/4"),
                "Should redirect to user profile after successful password change");
    }

    // =============== Delete Account ===============

    @Test
    @DisplayName("Delete account with confirmation should succeed")
    void deleteAccountTest() {
        // GIVEN: Create and register a new user to delete
        driver.get("https://localhost:" + port + "/register");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));

        String uniqueUsername = "deletetest" + System.currentTimeMillis();
        String uniqueEmail = "delete" + System.currentTimeMillis() + "@example.com";

        driver.findElement(By.id("username")).sendKeys(uniqueUsername);
        driver.findElement(By.id("email")).sendKeys(uniqueEmail);
        driver.findElement(By.id("password")).sendKeys("password123");
        driver.findElement(By.id("confirmPassword")).sendKeys("password123");
        driver.findElement(By.id("bio")).sendKeys("User to delete");

        WebElement registerButton = driver.findElement(By.id("register-button"));
        registerButton.click();

        wait.until(ExpectedConditions.urlContains("/login"));
        login(uniqueUsername, "password123");

        // Navigate to profile edit page
        WebElement profile = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".avatar")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", profile);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".delete-btn")));

        // WHEN
        WebElement deleteButton = driver.findElement(By.cssSelector(".delete-btn"));
        deleteButton.click();

        // Confirm deletion
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("confirm-button")));
        WebElement confirmButton = driver.findElement(By.id("confirm-button"));
        confirmButton.click();

        // THEN
        wait.until(ExpectedConditions.urlContains("/posts"));
        assertTrue(driver.getCurrentUrl().contains("/posts"), "Should redirect to posts after account deletion");
    }
}