package es.codeurjc.backend.dto;

import java.sql.Blob;
import java.time.LocalDateTime;
import java.util.List;

import es.codeurjc.backend.model.Topic;
import es.codeurjc.backend.model.User;

public record PostDTO (
    Long id,
    String title,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    User author,
    List<Topic> topics,
    int likes,
    List<Long> usersThatLiked,
    List<Blob> images) {
}