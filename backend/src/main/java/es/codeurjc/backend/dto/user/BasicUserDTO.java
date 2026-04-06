package es.codeurjc.backend.dto.user;

public record BasicUserDTO (
    Long id,
    String username,
	String email) {
}