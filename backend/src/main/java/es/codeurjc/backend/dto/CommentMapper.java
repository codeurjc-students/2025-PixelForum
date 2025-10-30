package es.codeurjc.backend.dto;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;

import es.codeurjc.backend.model.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    CommentDTO toDTO(Comment Comment);

    List<CommentDTO> toDTOs(Collection<Comment> Comments);

    Comment toDomain(CommentDTO CommentDTO);

}