package es.codeurjc.backend.dto.comment;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;

import es.codeurjc.backend.model.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    CommentDTO toDTO(Comment comment);

    List<CommentDTO> toDTOs(Collection<Comment> comments);

    Comment toDomain(CommentDTO commentDTO);

}