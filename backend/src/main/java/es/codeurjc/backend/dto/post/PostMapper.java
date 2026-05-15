package es.codeurjc.backend.dto.post;

import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.dto.user.UserMapper;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface PostMapper {

    @Mapping(target = "hasUserLiked", ignore = true)
    PostDTO toDTO(Post post);

    List<PostDTO> toDTOs(Collection<Post> posts);

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "usersThatLiked", ignore = true)
    Post toDomain(PostDTO postDTO);

    default PostDTO toDTOWithLike(Post post, User currentUser) {
        PostDTO dto = toDTO(post);
        Boolean hasLiked = false;
        if (currentUser != null) {
            hasLiked = post.getUsersThatLiked() != null &&
                    post.getUsersThatLiked().contains(currentUser);
        }
        return new PostDTO(
                dto.id(), dto.title(), dto.content(),
                dto.createdAt(), dto.updatedAt(),
                dto.author(), dto.topic(),
                dto.likes(),
                hasLiked,
                dto.images());
    }
}