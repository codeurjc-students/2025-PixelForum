package es.codeurjc.backend.e2e.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@Tag("selenium")
@DisplayName("Selenium UI Test")
@ActiveProfiles("test")
class UISystemIT {
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
    void tearDown(){
        driver.quit();
    }

    @Test
    void mainPageSeleniumTest(){
        driver.get("https://localhost:" + port + "/");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("app-root")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("app-post")));

        WebElement postTitle = driver.findElement(By.id("post-title"));
        assertEquals("GTA VI Massive leak", postTitle.getText());
    }

}
