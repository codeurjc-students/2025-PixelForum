package es.codeurjc.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import es.codeurjc.backend.model.Comment;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;

public record CommentDTO (
    Long id,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    User author,
    Post post,
    Comment commentId,
    int likes,
    List<Long> usersThatLiked) {
}