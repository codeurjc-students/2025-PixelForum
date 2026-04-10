package es.codeurjc.backend.dto.user;

import java.time.LocalDateTime;
import java.util.List;

public record UserDTO (
    Long id,
    String username,
	String email,
    LocalDateTime createdAt,
    String bio,
    Long avatar,
    List<Long> likedPosts,
    List<String> roles) {
}