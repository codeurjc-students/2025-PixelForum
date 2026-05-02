package es.codeurjc.backend.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import io.jsonwebtoken.Claims;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.UserRepository;
import es.codeurjc.backend.security.jwt.*;

import java.util.List;
import java.util.Optional;

@Tag("unit")
@DisplayName("UserLoginService Unitary tests")
@ActiveProfiles("test")
class UserLoginServiceUnitTest {

    private UserLoginService userLoginService;
    private AuthenticationManager authenticationManager;
    private JwtTokenProvider jwtTokenProvider;
    private UserRepository userRepository;
    private HttpServletResponse response;
    private User user;

    @BeforeEach
    void init() {
        authenticationManager = mock(AuthenticationManager.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        response = mock(HttpServletResponse.class);
        userRepository = mock(UserRepository.class);

        userLoginService = new UserLoginService(
                authenticationManager,
                jwtTokenProvider,
                userRepository);

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("password");
        user.setRoles(List.of("USER"));
    }

    @Test
    @DisplayName("Login should return SUCCESS with valid credentials")
    void loginSuccessTest() {
        // GIVEN
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");

        // WHEN
        ResponseEntity<AuthResponse> result = userLoginService.login(response, loginRequest);

        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(AuthResponse.Status.SUCCESS, result.getBody().getStatus());

        verify(response, times(2)).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("Login should add HttpOnly cookies")
    void loginCookiesHttpOnlyTest() {
        // GIVEN
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");

        // WHEN
        userLoginService.login(response, loginRequest);

        // THEN
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());

        List<Cookie> cookies = cookieCaptor.getAllValues();
        cookies.forEach(cookie -> {
            assertTrue(cookie.isHttpOnly(), "Cookie should be HttpOnly");
        });
    }

    @Test
    @DisplayName("Login should throw exception with invalid credentials")
    void loginInvalidCredentialsTest() {
        // GIVEN
        LoginRequest loginRequest = new LoginRequest("invalid", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // WHEN & THEN
        assertThrows(BadCredentialsException.class, () -> {
            userLoginService.login(response, loginRequest);
        });

        verify(response, never()).addCookie(any());
    }

    @Test
    @DisplayName("Refresh should return new Access Token")
    void refreshTokenSuccessTest() {
        // GIVEN
        String refreshToken = "valid-refresh-token";
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");

        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(claims);
        when(jwtTokenProvider.isTokenType(claims, TokenType.REFRESH)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("new-refresh-token");

        // WHEN
        ResponseEntity<AuthResponse> result = userLoginService.refresh(response, refreshToken);

        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(AuthResponse.Status.SUCCESS, result.getBody().getStatus());
        verify(response, atLeastOnce()).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("Refresh should fail with invalid token")
    void refreshInvalidTokenTest() {
        // GIVEN
        String invalidToken = "invalid-token";
        when(jwtTokenProvider.validateToken(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));

        // WHEN
        ResponseEntity<AuthResponse> result = userLoginService.refresh(response, invalidToken);

        // THEN
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(AuthResponse.Status.FAILURE, result.getBody().getStatus());
    }

    @Test
    @DisplayName("Logout should clear cookies")
    void logoutClearsCookiesTest() {
        // WHEN
        String result = userLoginService.logout(response);

        // THEN
        assertEquals("Logout successfully", result);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());

        List<Cookie> cookies = cookieCaptor.getAllValues();
        cookies.forEach(cookie -> {
            assertEquals(0, cookie.getMaxAge(), "Cookie should be deleted (maxAge=0)");
        });
    }
}