package es.codeurjc.backend.dto.User;

public record BasicUserDTO (
    Long id,
    String username,
	String email) {
}