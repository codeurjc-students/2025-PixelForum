package es.codeurjc.backend.controller.auth;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.codeurjc.backend.dto.user.UserDTO;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.security.jwt.AuthResponse;
import es.codeurjc.backend.security.jwt.AuthResponse.Status;
import es.codeurjc.backend.security.jwt.LoginRequest;
import es.codeurjc.backend.security.jwt.UserLoginService;
import es.codeurjc.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    private final UserLoginService userLoginService;
    private final UserService userService;

    public LoginController(UserLoginService userLoginService, UserService userService) {
        this.userLoginService = userLoginService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        return userLoginService.login(response, loginRequest);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @CookieValue(name = "RefreshToken", required = false) String refreshToken, HttpServletResponse response) {
        return userLoginService.refresh(response, refreshToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpServletResponse response) {
        return ResponseEntity.ok(new AuthResponse(Status.SUCCESS, userLoginService.logout(response)));
    }

    @GetMapping("/me")
    public UserDTO me(Principal principal) {
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return userService.getUserDetails(currentUser.getId(), currentUser);
    }

}