package es.codeurjc.backend.dto.user;

public record ChangePasswordDTO(
    String oldPassword,
    String newPassword) {
}