package es.codeurjc.backend.e2e.client;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@Tag("selenium")
@DisplayName("Authentication UI System Test")
@ActiveProfiles("test")
class AuthUISystemIT {
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

    @Test
    @DisplayName("Complete login flow should work correctly")
    void loginFlowSeleniumTest() {
        // Navigate to login page
        driver.get("https://localhost:" + port + "/login");
        
        // Wait for page load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-button")));
        
        // Fill login form
        WebElement usernameInput = driver.findElement(By.id("username"));
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement loginButton = driver.findElement(By.id("login-button"));
        
        usernameInput.sendKeys("admin");
        passwordInput.sendKeys("admin0");
        loginButton.click();
        
        // Verify redirect to posts
        wait.until(ExpectedConditions.urlContains("/posts"));
        assertTrue(driver.getCurrentUrl().contains("/posts"));
        
        // Verify cookies exist
        Cookie authToken = driver.manage().getCookieNamed("AuthToken");
        Cookie refreshToken = driver.manage().getCookieNamed("RefreshToken");
        
        assertNotNull(authToken, "AuthToken cookie should exist");
        assertNotNull(refreshToken, "RefreshToken cookie should exist");
        assertTrue(authToken.isHttpOnly(), "AuthToken should be HttpOnly");
        assertTrue(refreshToken.isHttpOnly(), "RefreshToken should be HttpOnly");
    }

    @Test
    @DisplayName("Login with invalid credentials should show error and not set cookies")
    void loginInvalidSeleniumTest() {
        // Navigate to login
        driver.get("https://localhost:" + port + "/login");
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-button")));
        
        // Fill with invalid credentials
        WebElement usernameInput = driver.findElement(By.id("username"));
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement loginButton = driver.findElement(By.id("login-button"));
        
        usernameInput.sendKeys("invalid");
        passwordInput.sendKeys("wrong");
        loginButton.click();
        
        // Verify error message appears
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("error-message")));
        WebElement errorMessage = driver.findElement(By.className("error-message"));
        assertTrue(errorMessage.isDisplayed());
        
        // Verify no cookies
        assertNull(driver.manage().getCookieNamed("AuthToken"));
        assertNull(driver.manage().getCookieNamed("RefreshToken"));
    }

    @Test
    @DisplayName("Access protected route without auth should redirect to login")
    void accessProtectedWithoutAuthTest() {
        // Try to access protected route directly
        driver.get("https://localhost:" + port + "/profile");
        
        // Verify redirect to login
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    @DisplayName("Access protected route with auth should work correctly")
    void accessProtectedWithAuthTest() {
        // Login first
        driver.get("https://localhost:" + port + "/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-button")));
        
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).sendKeys("admin0");
        driver.findElement(By.id("login-button")).click();
        
        wait.until(ExpectedConditions.urlContains("/posts"));
        
        // Navigate to protected route
        driver.get("https://localhost:" + port + "/profile");
        
        // Should access profile page
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("app-profile")));
        assertTrue(driver.getCurrentUrl().contains("/profile"));
        
        // Verify content loaded
        WebElement profileContent = driver.findElement(By.cssSelector("app-profile"));
        assertTrue(profileContent.isDisplayed());
    }

    @Test
    @DisplayName("Logout should clear cookies")
    void logoutSeleniumTest() {
        // Login first
        driver.get("https://localhost:" + port + "/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-button")));
        
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).sendKeys("admin0");
        driver.findElement(By.id("login-button")).click();
        
        wait.until(ExpectedConditions.urlContains("/posts"));
        
        // Verify cookies exist
        assertNotNull(driver.manage().getCookieNamed("AuthToken"));
        
        // Click logout
        WebElement logoutButton = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("logout-button"))
        );

        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].click();", logoutButton
        );
        
        // Verify redirect to login/posts
        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/login"),
            ExpectedConditions.urlContains("/posts")
        ));
        
        // Verify cookies removed
        assertNull(driver.manage().getCookieNamed("AuthToken"));
        assertNull(driver.manage().getCookieNamed("RefreshToken"));
    }

    @Test
    @DisplayName("Session persistence on reload system test")
    void sessionPersistenceTest() {
        // Login
        driver.get("https://localhost:" + port + "/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-button")));
        
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).sendKeys("admin0");
        driver.findElement(By.id("login-button")).click();
        
        wait.until(ExpectedConditions.urlContains("/posts"));
        
        // Reload page
        driver.navigate().refresh();
        
        // Should stay authenticated
        wait.until(ExpectedConditions.urlContains("/posts"));     
        assertNotNull(driver.manage().getCookieNamed("AuthToken"));
    }
}