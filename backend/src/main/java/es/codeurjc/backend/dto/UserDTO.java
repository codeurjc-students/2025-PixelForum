package es.codeurjc.backend.dto;

import java.util.List;

public record UserDTO (
    Long id,
    String username,
	String email,
    String password,
    List<Long> likedPosts,
    List<Long> likedComments,
    List<String> roles) {
}