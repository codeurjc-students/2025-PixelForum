package es.codeurjc.backend.dto.user;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.codeurjc.backend.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDTO(User user);

    List<UserDTO> toDTOs(Collection<User> users);

    @Mapping(target = "likedPosts", ignore = true)
    User toDomain(UserDTO userDTO);

}