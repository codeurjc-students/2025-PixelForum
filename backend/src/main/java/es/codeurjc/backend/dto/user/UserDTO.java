package es.codeurjc.backend.dto.user;

import java.util.List;

import es.codeurjc.backend.model.Image;

public record UserDTO (
    Long id,
    String username,
	String email,
    String password,
    List<Long> likedPosts,
    List<Long> likedComments,
    List<String> roles,
    Image avatar) {
}