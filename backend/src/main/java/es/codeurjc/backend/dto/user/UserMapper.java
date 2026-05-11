package es.codeurjc.backend.dto.user;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDTO(User user);
	BasicUserDTO toBasicDTO(User user);

    List<UserDTO> toDTOs(Collection<User> users);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "likedComments", ignore = true)
    User toDomain(UserDTO userDTO);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "avatar", ignore = true)
	@Mapping(target = "likedPosts", ignore = true)
	@Mapping(target = "likedComments", ignore = true)
	@Mapping(target = "roles", ignore = true)
	User toDomain(CreateUserDTO userDTO);

    // Image -> Long
	default Long mapImage(Image image) {
		return image != null ? image.getId() : null;
	}

	// Long -> Image
	default Image mapImage(Long id) {
		if (id == null) return null;
		Image image = new Image();
		image.setId(id);
		return image;
	}

	// Post -> Long
	default Long mapPost(Post post) {
		return post != null ? post.getId() : null;
	}

	// Long -> Post
	default Post mapPost(Long id) {
		if (id == null) return null;
		Post post = new Post();
		post.setId(id);
		return post;
	}
}