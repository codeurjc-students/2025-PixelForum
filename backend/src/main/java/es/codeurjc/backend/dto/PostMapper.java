package es.codeurjc.backend.dto;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;

import es.codeurjc.backend.model.Post;

@Mapper(componentModel = "spring")
public interface PostMapper {

    PostDTO toDTO(Post post);

    List<PostDTO> toDTOs(Collection<Post> posts);

    Post toDomain(PostDTO postDTO);
    
}