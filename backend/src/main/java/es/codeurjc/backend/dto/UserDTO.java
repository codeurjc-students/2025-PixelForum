package es.codeurjc.backend.dto;

import java.sql.Blob;
import java.util.List;

public record UserDTO (
    Long id,
    String username,
	String email,
    String password,
    Blob avatar,
    List<Long> likedPosts,
    List<Long> likedComments,
    List<String> roles) {
}