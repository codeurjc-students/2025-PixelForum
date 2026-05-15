package es.codeurjc.backend.dto.user;

import java.time.LocalDateTime;

public record BasicUserDTO (
    Long id,
    String username,
	LocalDateTime createdAt,
    String bio,
    Long avatar) {
}