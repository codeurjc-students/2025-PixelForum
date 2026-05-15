package es.codeurjc.backend.dto.user;

public record CreateUserDTO(
        String username,
        String email,
        String password,
        String bio) {
}