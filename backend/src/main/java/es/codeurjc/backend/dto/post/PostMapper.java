package es.codeurjc.backend.dto.post;

import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.Image;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "images", source = "images", qualifiedByName = "imagesToIds")
    PostDTO toDTO(Post post);

    List<PostDTO> toDTOs(Collection<Post> posts);

    @Mapping(target = "author.password", ignore = true)
    @Mapping(target = "author.avatar", ignore = true)
    @Mapping(target = "author.likedPosts", ignore = true)
    @Mapping(target = "author.likedComments", ignore = true)
    @Mapping(target = "author.roles", ignore = true)
    @Mapping(target = "images", source = "images", qualifiedByName = "idsToImages")
    Post toDomain(PostDTO postDTO);

    @Named("imagesToIds")
    default List<Long> imagesToIds(List<Image> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return images.stream()
                .map(Image::getId)
                .toList();
    }

    @Named("idsToImages")
    default List<Image> idsToImages(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(id -> {
                    Image image = new Image();
                    image.setId(id);
                    return image;
                })
                .toList();
    }
}