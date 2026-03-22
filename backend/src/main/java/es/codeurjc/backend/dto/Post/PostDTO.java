package es.codeurjc.backend.dto.post;

import java.time.LocalDateTime;
import java.util.List;

import es.codeurjc.backend.dto.user.BasicUserDTO;
import es.codeurjc.backend.model.Topic;

public record PostDTO (
    Long id,
    String title,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    BasicUserDTO author,
    Topic topic,
    Integer likes,
    List<Long> usersThatLiked,
    List<Long> images) {
}