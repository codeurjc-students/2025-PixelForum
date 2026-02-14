package es.codeurjc.backend.integration;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import es.codeurjc.backend.security.jwt.AuthResponse;
import es.codeurjc.backend.security.jwt.LoginRequest;
import es.codeurjc.backend.security.jwt.TokenType;
import es.codeurjc.backend.security.jwt.UserLoginService;

@Tag("integration")
@DisplayName("UserLoginService Integration tests")
@ActiveProfiles("test")
@SpringBootTest
class UserLoginServiceIntegrationTest {

    @Autowired
    private UserLoginService userLoginService;

    @Test
    @DisplayName("Login should succeed with valid credentials and set cookies")
    void loginSuccessTest() {
        // GIVEN
        MockHttpServletResponse response = new MockHttpServletResponse();
        LoginRequest loginRequest = new LoginRequest("admin", "admin0");

        // WHEN
        ResponseEntity<AuthResponse> result = userLoginService.login(response, loginRequest);

        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(AuthResponse.Status.SUCCESS, result.getBody().getStatus());

        Cookie accessCookie = response.getCookie(TokenType.ACCESS.cookieName);
        Cookie refreshCookie = response.getCookie(TokenType.REFRESH.cookieName);

        assertNotNull(accessCookie, "Access cookie should not be null");
        assertNotNull(refreshCookie, "Refresh cookie should not be null");
        assertTrue(accessCookie.isHttpOnly(), "Access cookie should be HttpOnly");
        assertTrue(refreshCookie.isHttpOnly(), "Refresh cookie should be HttpOnly");
    }

    @Test
    @DisplayName("Login should fail with invalid credentials")
    void loginInvalidCredentialsTest() {
        // GIVEN
        MockHttpServletResponse response = new MockHttpServletResponse();
        LoginRequest loginRequest = new LoginRequest("user", "wrongpassword");

        // WHEN & THEN
        assertThrows(Exception.class, () -> userLoginService.login(response, loginRequest));
    }

    @Test
    @DisplayName("Refresh should succeed with valid refresh token")
    void refreshSuccessTest() {
        // GIVEN
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        LoginRequest loginRequest = new LoginRequest("admin", "admin0");
        userLoginService.login(loginResponse, loginRequest);

        Cookie refreshCookie = loginResponse.getCookie(TokenType.REFRESH.cookieName);
        assertNotNull(refreshCookie, "Refresh cookie should not be null");

        // WHEN
        MockHttpServletResponse refreshResponse = new MockHttpServletResponse();
        ResponseEntity<AuthResponse> refreshResult =
                userLoginService.refresh(refreshResponse, refreshCookie.getValue());

        // THEN
        assertEquals(HttpStatus.OK, refreshResult.getStatusCode());
        assertEquals(AuthResponse.Status.SUCCESS, refreshResult.getBody().getStatus());

        Cookie newAccessCookie = refreshResponse.getCookie(TokenType.ACCESS.cookieName);
        Cookie newRefreshCookie = refreshResponse.getCookie(TokenType.REFRESH.cookieName);

        assertNotNull(newAccessCookie, "New access cookie should not be null");
        assertNotNull(newRefreshCookie, "New refresh cookie should not be null");
        assertFalse(newAccessCookie.getValue().isEmpty(), "New access cookie should have value");
        assertFalse(newRefreshCookie.getValue().isEmpty(), "New refresh cookie should have value");
    }

    @Test
    @DisplayName("Refresh should fail with invalid refresh token")
    void refreshInvalidTokenTest() {
        // GIVEN
        MockHttpServletResponse response = new MockHttpServletResponse();
        String invalidToken = "invalid-refresh-token";

        // WHEN
        ResponseEntity<AuthResponse> result = userLoginService.refresh(response, invalidToken);

        // THEN
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(AuthResponse.Status.FAILURE, result.getBody().getStatus());
    }

    @Test
    @DisplayName("Logout should clear both cookies")
    void logoutTest() {
        // GIVEN
        MockHttpServletResponse response = new MockHttpServletResponse();

        // WHEN
        String result = userLoginService.logout(response);

        // THEN
        assertEquals("Logout successfully", result);

        Cookie accessCookie = response.getCookie(TokenType.ACCESS.cookieName);
        Cookie refreshCookie = response.getCookie(TokenType.REFRESH.cookieName);

        assertNotNull(accessCookie, "Access cookie should not be null");
        assertNotNull(refreshCookie, "Refresh cookie should not be null");
        assertEquals(0, accessCookie.getMaxAge(), "Access cookie should be deleted");
        assertEquals(0, refreshCookie.getMaxAge(), "Refresh cookie should be deleted");
    }
}