package es.codeurjc.backend.dto.comment;

import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import es.codeurjc.backend.dto.user.UserMapper;
import es.codeurjc.backend.model.Comment;
import es.codeurjc.backend.model.User;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface CommentMapper {

    @Mapping(target = "hasUserLiked", ignore = true)
    CommentDTO toDTO(Comment comment);

    List<CommentDTO> toDTOs(Collection<Comment> comments);

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "post", ignore = true)
    @Mapping(target = "usersThatLiked", ignore = true)
    Comment toDomain(CommentDTO commentDTO);

    default CommentDTO toDTOWithLike(Comment comment, User currentUser) {
        CommentDTO dto = toDTO(comment);
        Boolean hasLiked = false;
        if (currentUser != null) {
            hasLiked = comment.getUsersThatLiked() != null && comment.getUsersThatLiked().contains(currentUser);
        }
        return new CommentDTO(
                dto.id(), dto.content(),
                dto.createdAt(), dto.updatedAt(),
                dto.author(),
                dto.parentComment(),
                dto.likes(),
                hasLiked,
                dto.post());
    }
}