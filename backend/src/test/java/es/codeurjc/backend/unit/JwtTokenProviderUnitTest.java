package es.codeurjc.backend.unit;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import es.codeurjc.backend.model.User;
import es.codeurjc.backend.security.jwt.JwtTokenProvider;
import es.codeurjc.backend.security.jwt.TokenType;

import java.util.List;

@Tag("unit")
@DisplayName("JwtTokenProvider Unitary tests")
@ActiveProfiles("test")
class JwtTokenProviderUnitTest {

    private JwtTokenProvider jwtTokenProvider;
    private User user;

    @BeforeEach
    void init() {
        jwtTokenProvider = new JwtTokenProvider();
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("password");
        user.setRoles(List.of("USER", "ADMIN"));
    }

    @Test
    @DisplayName("Should generate valid Access Token")
    void generateAccessTokenTest() {
        // WHEN
        String token = jwtTokenProvider.generateAccessToken(user);

        // THEN
        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = jwtTokenProvider.validateToken(token);
        assertEquals("1", claims.getSubject());
        assertEquals("ACCESS", claims.get("type"));
    }

    @Test
    @DisplayName("Should generate valid Refresh Token")
    void generateRefreshTokenTest() {
        // WHEN
        String token = jwtTokenProvider.generateRefreshToken(user);

        // THEN
        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = jwtTokenProvider.validateToken(token);
        assertEquals("1", claims.getSubject());
        assertEquals("REFRESH", claims.get("type"));
    }

    @Test
    @DisplayName("Should validate token successfully")
    void validateTokenTest() {
        // GIVEN
        String token = jwtTokenProvider.generateAccessToken(user);

        // WHEN
        Claims claims = jwtTokenProvider.validateToken(token);

        // THEN
        assertNotNull(claims);
        assertEquals("1", claims.getSubject());
        assertNotNull(claims.get("roles"));
    }

    @Test
    @DisplayName("validateToken from Authorization header should return claims")
    void validateTokenFromHeaderTest() {
        // GIVEN
        String token = jwtTokenProvider.generateAccessToken(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        // WHEN
        Claims claims = jwtTokenProvider.validateToken(request, false);

        // THEN
        assertNotNull(claims);
        assertEquals("1", claims.getSubject());
    }

    @Test
    @DisplayName("validateToken should throw when Authorization header is missing")
    void validateTokenMissingHeaderTest() {
        // GIVEN
        MockHttpServletRequest request = new MockHttpServletRequest();

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.validateToken(request, false);
        });
    }

    @Test
    @DisplayName("validateToken should throw when Authorization header does not start with Bearer")
    void validateTokenInvalidBearerHeaderTest() {
        // GIVEN
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Invalid token");

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.validateToken(request, false);
        });
    }

    @Test
    @DisplayName("validateToken from cookies should return claims")
    void validateTokenFromCookiesTest() {
        // GIVEN
        String token = jwtTokenProvider.generateAccessToken(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(TokenType.ACCESS.cookieName, token));

        // WHEN
        Claims claims = jwtTokenProvider.validateToken(request, true);

        // THEN
        assertNotNull(claims);
        assertEquals("1", claims.getSubject());
    }

    @Test
    @DisplayName("validateToken from cookies should throw when no cookies exist")
    void validateTokenNoCookiesTest() {
        // GIVEN
        MockHttpServletRequest request = new MockHttpServletRequest();

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.validateToken(request, true);
        });
    }

    @Test
    @DisplayName("validateToken from cookies should throw when access token cookie does not exist")
    void validateTokenMissingAccessCookieTest() {
        // GIVEN
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("OTHER_COOKIE", "value"));

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.validateToken(request, true);
        });
    }

    @Test
    @DisplayName("validateToken from cookies should throw when cookie value is null")
    void validateTokenNullCookieValueTest() {
        // GIVEN
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(TokenType.ACCESS.cookieName, null));

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.validateToken(request, true);
        });
    }

    @Test
    @DisplayName("Should throw exception for invalid token")
    void invalidTokenTest() {
        // GIVEN
        String invalidToken = "invalid.token.here";

        // WHEN & THEN
        assertThrows(Exception.class, () -> {
            jwtTokenProvider.validateToken(invalidToken);
        });
    }

    @Test
    @DisplayName("Access Token should expire in 5 minutes")
    void accessTokenExpirationTest() {
        // GIVEN
        String token = jwtTokenProvider.generateAccessToken(user);
        Claims claims = jwtTokenProvider.validateToken(token);

        // WHEN
        long expirationTime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        long fifteenMinutesInMs = TokenType.ACCESS.duration.toMillis();

        // THEN
        assertTrue(Math.abs(expirationTime - fifteenMinutesInMs) < 1000,
                "Access token should expire in 5 minutes");
    }

    @Test
    @DisplayName("Refresh Token should expire in 7 days")
    void refreshTokenExpirationTest() {
        // GIVEN
        String token = jwtTokenProvider.generateRefreshToken(user);
        Claims claims = jwtTokenProvider.validateToken(token);

        // WHEN
        long expirationTime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        long sevenDaysInMs = TokenType.REFRESH.duration.toMillis();

        // THEN
        assertTrue(Math.abs(expirationTime - sevenDaysInMs) < 1000,
                "Refresh token should expire in 7 days");
    }

    @Test
    @DisplayName("Token should contain user roles")
    void tokenContainsRolesTest() {
        // GIVEN
        String token = jwtTokenProvider.generateAccessToken(user);

        // WHEN
        Claims claims = jwtTokenProvider.validateToken(token);

        // THEN
        assertNotNull(claims.get("roles"));
        assertTrue(claims.get("roles").toString().contains("USER"));
        assertTrue(claims.get("roles").toString().contains("ADMIN"));
    }

    @Test
    @DisplayName("isTokenType with Claims should return true for correct token type")
    void isTokenTypeWithClaimsTest() {
        // GIVEN
        String token = jwtTokenProvider.generateAccessToken(user);
        Claims claims = jwtTokenProvider.validateToken(token);

        // WHEN
        boolean result = jwtTokenProvider.isTokenType(claims, TokenType.ACCESS);

        // THEN
        assertTrue(result, "isTokenType should return true for correct Access token type");
    }

    @Test
    @DisplayName("isTokenType with token string should return false for wrong token type")
    void isTokenTypeWithTokenStringWrongTypeTest() {
        // GIVEN
        String token = jwtTokenProvider.generateRefreshToken(user);

        // WHEN
        boolean result = jwtTokenProvider.isTokenType(token, TokenType.ACCESS);

        // THEN
        assertFalse(result, "isTokenType should return false for wrong token type");
    }

    @Test
    @DisplayName("isTokenType with token string should return true for correct token type")
    void isTokenTypeWithTokenStringCorrectTypeTest() {
        // GIVEN
        String token = jwtTokenProvider.generateRefreshToken(user);

        // WHEN
        boolean result = jwtTokenProvider.isTokenType(token, TokenType.REFRESH);

        // THEN
        assertTrue(result);
    }

    @Test
    @DisplayName("isTokenType with claims should return false for incorrect token type")
    void isTokenTypeWithClaimsWrongTypeTest() {
        // GIVEN
        String token = jwtTokenProvider.generateAccessToken(user);
        Claims claims = jwtTokenProvider.validateToken(token);

        // WHEN
        boolean result = jwtTokenProvider.isTokenType(claims, TokenType.REFRESH);

        // THEN
        assertFalse(result);
    }
}