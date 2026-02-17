package es.codeurjc.backend.dto.Post;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.codeurjc.backend.model.Post;

@Mapper(componentModel = "spring")
public interface PostMapper {

    PostDTO toDTO(Post post);

    List<PostDTO> toDTOs(Collection<Post> posts);
    
    @Mapping(target = "author.password", ignore = true)
    @Mapping(target = "author.avatar", ignore = true)
    @Mapping(target = "author.likedPosts", ignore = true)
    @Mapping(target = "author.likedComments", ignore = true)
    @Mapping(target = "author.roles", ignore = true)
    Post toDomain(PostDTO postDTO);
    
}