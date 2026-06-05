package es.codeurjc.backend.dto.comment;

import java.time.LocalDateTime;

import es.codeurjc.backend.dto.user.BasicUserDTO;

public record CommentDTO (
    Long id,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    BasicUserDTO author,
    CommentDTO parentComment,
    Integer likes,
    Boolean hasUserLiked) {
}