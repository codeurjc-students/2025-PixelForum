package es.codeurjc.backend.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import es.codeurjc.backend.dto.post.PostDTO;
import es.codeurjc.backend.dto.post.PostMapper;
import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.Topic;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.ImageRepository;
import es.codeurjc.backend.repository.PostRepository;
import es.codeurjc.backend.service.PostService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.test.context.ActiveProfiles;

@Tag("unit")
@DisplayName("PostService Unitary tests")
@ActiveProfiles("test")
class PostServiceUnitTest {

	private PostService postService;
	private PostRepository postRepository;
	private ImageRepository imageRepository;
	private PostMapper mapper;

	private Post post;
	private PostDTO postDTO;
	private User user;

	@BeforeEach
	void init() {
		postRepository = mock(PostRepository.class);
		imageRepository = mock(ImageRepository.class);
		mapper = mock(PostMapper.class);

		postService = new PostService(mapper, postRepository, imageRepository);

		user = new User();
		user.setId(1L);
		user.setRoles(List.of("USER"));

		post = new Post();
		post.setId(1L);
		post.setTitle("Title");
		post.setContent("Content");
		post.setImages(new ArrayList<>());
		post.setAuthor(user);

		postDTO = mock(PostDTO.class);

		when(postDTO.title()).thenReturn("Title");
		when(postDTO.content()).thenReturn("Content");
		when(postDTO.images()).thenReturn(List.of());
		when(postDTO.topic()).thenReturn(null);
	}

	@Test
	@DisplayName("getPost should return PostDTO when post exists")
	void getPostSuccessTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(mapper.toDTOWithLike(post, user)).thenReturn(postDTO);

		// WHEN
		PostDTO result = postService.getPost(1L, user);

		// THEN
		assertEquals(postDTO, result);
		verify(postRepository).findById(1L);
		verify(mapper).toDTOWithLike(post, user);
	}

	@Test
	@DisplayName("getPost should throw exception when post not found")
	void getPostNotFoundTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> {
			postService.getPost(1L, user);
		});
	}

	@Test
	@DisplayName("searchAndFilterPosts should return mapped page")
	void searchPostsTest() {
		// GIVEN
		Page<Post> page = new PageImpl<>(List.of(post));

		when(postRepository.findByFilters(null, null, null, Pageable.unpaged()))
				.thenReturn(page);

		// WHEN
		Page<PostDTO> result = postService.searchAndFilterPosts(null, null, null, Pageable.unpaged(), user);

		// THEN
		assertEquals(1, result.getContent().size());
		verify(postRepository).findByFilters(null, null, null, Pageable.unpaged());
	}

	@Test
	@DisplayName("exist should return true when post exists")
	void existTrueTest() {
		// GIVEN
		when(postRepository.existsById(1L)).thenReturn(true);

		// WHEN
		boolean result = postService.exist(1L);

		// THEN
		assertTrue(result);
	}

	@Test
	@DisplayName("exist should return false when post does not exist")
	void existFalseTest() {
		// GIVEN
		when(postRepository.existsById(1L)).thenReturn(false);

		// WHEN
		boolean result = postService.exist(1L);

		// THEN
		assertFalse(result);
	}

	@Test
	@DisplayName("findAll should return all posts")
	void findAllTest() {
		// GIVEN
		when(postRepository.findAll()).thenReturn(List.of(post));

		// WHEN
		List<Post> result = postService.findAll();

		// THEN
		assertEquals(1, result.size());
		verify(postRepository).findAll();
	}

	@Test
	@DisplayName("save should persist post")
	void saveTest() {
		// GIVEN
		when(postRepository.save(post)).thenReturn(post);

		// WHEN
		Post result = postService.save(post);

		// THEN
		assertEquals(post, result);
		verify(postRepository).save(post);
	}

	@Test
	@DisplayName("createPost should save new post and return DTO")
	void createPostSuccessTest() {
		// GIVEN
		when(mapper.toDomain(postDTO)).thenReturn(post);
		when(postRepository.save(any(Post.class))).thenReturn(post);
		when(mapper.toDTOWithLike(post, user)).thenReturn(postDTO);

		// WHEN
		PostDTO result = postService.createPost(postDTO, user);

		// THEN
		assertEquals(postDTO, result);
		verify(postRepository).save(any(Post.class));
		verify(mapper).toDTOWithLike(post, user);
	}

	@Test
	@DisplayName("createPost should initialize default fields")
	void createPostInitializeFieldsTest() {
		// GIVEN
		when(mapper.toDomain(postDTO)).thenReturn(post);
		when(postRepository.save(any(Post.class))).thenReturn(post);
		when(mapper.toDTOWithLike(post, user)).thenReturn(postDTO);

		// WHEN
		postService.createPost(postDTO, user);

		// THEN
		assertNotNull(post.getCreatedAt());
		assertNotNull(post.getUpdatedAt());
		assertEquals(0, post.getLikes());
		assertEquals(user, post.getAuthor());
	}

	@Test
	@DisplayName("createPost should assign images correctly")
	void createPostWithImagesTest() {
		// GIVEN
		Image img = new Image();
		img.setId(1L);
		img.setOwner(user);

		when(postDTO.images()).thenReturn(List.of(1L));
		when(mapper.toDomain(postDTO)).thenReturn(post);
		when(imageRepository.findAllById(List.of(1L))).thenReturn(List.of(img));
		when(postRepository.save(any(Post.class))).thenReturn(post);
		when(mapper.toDTOWithLike(post, user)).thenReturn(postDTO);

		// WHEN
		PostDTO result = postService.createPost(postDTO, user);

		// THEN
		assertEquals(postDTO, result);
		assertEquals(1, post.getImages().size());
		verify(imageRepository).findAllById(List.of(1L));
	}

	@Test
	@DisplayName("createPost should throw when images not found")
	void createPostImagesNotFoundTest() {
		// GIVEN
		when(postDTO.images()).thenReturn(List.of(1L, 2L));
		when(mapper.toDomain(postDTO)).thenReturn(post);
		when(imageRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of());

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> {
			postService.createPost(postDTO, user);
		});
	}

	@Test
	@DisplayName("createPost should fail if image not owned by user")
	void createPostWrongOwnerTest() {
		// GIVEN
		User other = new User();
		other.setId(99L);

		Image img = new Image();
		img.setId(1L);
		img.setOwner(other);

		when(postDTO.images()).thenReturn(List.of(1L));
		when(mapper.toDomain(postDTO)).thenReturn(post);
		when(imageRepository.findAllById(List.of(1L))).thenReturn(List.of(img));

		// WHEN & THEN
		assertThrows(AccessDeniedException.class, () -> {
			postService.createPost(postDTO, user);
		});
	}

	@Test
	@DisplayName("createPost should fail when image already belongs to another post")
	void createPostImageAlreadyAssignedTest() {
		// GIVEN
		Post otherPost = new Post();
		otherPost.setId(99L);

		Image img = new Image();
		img.setId(1L);
		img.setOwner(user);
		img.setPost(otherPost);

		when(postDTO.images()).thenReturn(List.of(1L));
		when(mapper.toDomain(postDTO)).thenReturn(post);
		when(imageRepository.findAllById(List.of(1L))).thenReturn(List.of(img));

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> {
			postService.createPost(postDTO, user);
		});
	}

	@Test
	@DisplayName("createPost should allow admin to use foreign images")
	void createPostAdminCanUseForeignImagesTest() {
		// GIVEN
		User admin = new User();
		admin.setId(10L);
		admin.setRoles(List.of("ADMIN"));

		User owner = new User();
		owner.setId(99L);

		Image img = new Image();
		img.setId(1L);
		img.setOwner(owner);

		when(postDTO.images()).thenReturn(List.of(1L));
		when(mapper.toDomain(postDTO)).thenReturn(post);
		when(imageRepository.findAllById(List.of(1L))).thenReturn(List.of(img));
		when(postRepository.save(any())).thenReturn(post);
		when(mapper.toDTOWithLike(post, admin)).thenReturn(postDTO);

		// WHEN & THEN
		assertDoesNotThrow(() -> {
			postService.createPost(postDTO, admin);
		});
	}

	@Test
	@DisplayName("updatePost should update post when author edits")
	void updatePostSuccessTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postRepository.save(post)).thenReturn(post);
		when(mapper.toDTOWithLike(post, user)).thenReturn(postDTO);

		when(postDTO.title()).thenReturn("New title");
		when(postDTO.content()).thenReturn("New content");

		// WHEN
		PostDTO result = postService.updatePost(1L, postDTO, user);

		// THEN
		assertEquals(postDTO, result);
		verify(postRepository).save(post);
	}

	@Test
	@DisplayName("updatePost should return same post when no changes")
	void updatePostNoChangesTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(mapper.toDTOWithLike(post, user)).thenReturn(postDTO);

		// WHEN
		PostDTO result = postService.updatePost(1L, postDTO, user);

		// THEN
		assertEquals(postDTO, result);
		verify(postRepository, never()).save(any());
	}

	@Test
	@DisplayName("updatePost should fail when image belongs to another post")
	void updatePostImageAlreadyAssignedTest() {
		// GIVEN
		Post otherPost = new Post();
		otherPost.setId(99L);

		Image img = new Image();
		img.setId(5L);
		img.setOwner(user);
		img.setPost(otherPost);

		post.setImages(new ArrayList<>());

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postDTO.images()).thenReturn(List.of(5L));
		when(imageRepository.findAllById(List.of(5L))).thenReturn(List.of(img));

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> {
			postService.updatePost(1L, postDTO, user);
		});
	}

	@Test
	@DisplayName("updatePost should assign owner to added images")
	void updatePostAssignOwnerToImagesTest() {
		// GIVEN
		Image img = new Image();
		img.setId(2L);
		img.setOwner(user);

		post.setImages(new ArrayList<>());

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postDTO.images()).thenReturn(List.of(2L));
		when(imageRepository.findAllById(List.of(2L))).thenReturn(List.of(img));
		when(postRepository.save(post)).thenReturn(post);

		// WHEN
		postService.updatePost(1L, postDTO, user);

		// THEN
		assertEquals(user, img.getOwner());
		assertEquals(post, img.getPost());
	}

	@Test
	@DisplayName("updatePost should detect topic changes")
	void updatePostTopicChangedTest() {
		// GIVEN
		Topic topic = new Topic();
		topic.setId(2L);

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postRepository.save(post)).thenReturn(post);

		when(postDTO.topic()).thenReturn(topic);

		// WHEN
		postService.updatePost(1L, postDTO, user);

		// THEN
		verify(postRepository).save(post);
	}

	@Test
	@DisplayName("updatePost should throw AccessDeniedException when not author")
	void updatePostAccessDeniedTest() {
		// GIVEN
		User otherUser = new User();
		otherUser.setId(2L);
		otherUser.setRoles(List.of("USER"));

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));

		// WHEN & THEN
		assertThrows(AccessDeniedException.class, () -> {
			postService.updatePost(1L, postDTO, otherUser);
		});
	}

	@Test
	@DisplayName("updatePost should throw when post not found")
	void updatePostNotFoundTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> {
			postService.updatePost(1L, postDTO, user);
		});
	}

	@Test
	@DisplayName("updatePost should update topic")
	void updatePostTopicUpdateTest() {
		// GIVEN
		Topic oldTopic = new Topic();
		oldTopic.setId(1L);
		Topic newTopic = new Topic();
		newTopic.setId(2L);

		post.setTopic(oldTopic);

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postDTO.topic()).thenReturn(newTopic);
		when(postRepository.save(post)).thenReturn(post);

		// WHEN
		postService.updatePost(1L, postDTO, user);

		// THEN
		assertEquals(newTopic, post.getTopic());
	}

	@Test
	@DisplayName("updatePost should add new images")
	void updatePostAddImagesTest() {
		// GIVEN
		Image img = new Image();
		img.setId(2L);
		img.setOwner(user);

		post.setImages(new ArrayList<>());

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postDTO.images()).thenReturn(List.of(2L));
		when(imageRepository.findAllById(List.of(2L))).thenReturn(List.of(img));
		when(postRepository.save(post)).thenReturn(post);

		// WHEN
		postService.updatePost(1L, postDTO, user);

		// THEN
		assertEquals(1, post.getImages().size());
	}

	@Test
	@DisplayName("updatePost should remove images")
	void updatePostRemoveImagesTest() {
		// GIVEN
		Image img = new Image();
		img.setId(1L);
		img.setOwner(user);

		post.setImages(new ArrayList<>(List.of(img)));

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postDTO.images()).thenReturn(List.of());
		when(imageRepository.findAllById(List.of(1L))).thenReturn(List.of(img));
		when(postRepository.save(post)).thenReturn(post);

		// WHEN
		postService.updatePost(1L, postDTO, user);

		// THEN
		assertTrue(post.getImages().isEmpty());
		verify(imageRepository).deleteAllById(List.of(1L));
	}

	@Test
	@DisplayName("updatePost should throw when adding non-existing image")
	void updatePostImageNotFoundTest() {
		// GIVEN
		post.setImages(new ArrayList<>());

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postDTO.images()).thenReturn(List.of(5L));
		when(imageRepository.findAllById(List.of(5L))).thenReturn(List.of());

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> {
			postService.updatePost(1L, postDTO, user);
		});
	}

	@Test
	@DisplayName("deletePost should delete when author")
	void deletePostSuccessTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));

		// WHEN
		postService.deletePost(1L, user);

		// THEN
		verify(postRepository).delete(post);
	}

	@Test
	@DisplayName("deletePost should remove post from liked posts")
	void deletePostShouldRemoveLikesTest() {
		// GIVEN
		User liker = new User();
		liker.setLikedPosts(new ArrayList<>(List.of(post)));
		post.setUsersThatLiked(new ArrayList<>(List.of(liker)));

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));

		// WHEN
		postService.deletePost(1L, user);

		// THEN
		assertFalse(liker.getLikedPosts().contains(post));
		verify(postRepository).delete(post);
	}

	@Test
	@DisplayName("deletePost should allow admin")
	void deletePostAdminTest() {
		// GIVEN
		User admin = new User();
		admin.setId(2L);
		admin.setRoles(List.of("ADMIN"));

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));

		// WHEN
		postService.deletePost(1L, admin);

		// THEN
		verify(postRepository).delete(post);
	}

	@Test
	@DisplayName("deletePost should throw AccessDeniedException when not author")
	void deletePostAccessDeniedTest() {
		// GIVEN
		User otherUser = new User();
		otherUser.setId(3L);
		otherUser.setRoles(List.of("USER"));

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));

		// WHEN & THEN
		assertThrows(AccessDeniedException.class, () -> {
			postService.deletePost(1L, otherUser);
		});
	}

	@Test
	@DisplayName("deletePost should throw exception when post not found")
	void deletePostNotFoundTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> {
			postService.deletePost(1L, user);
		});
	}

	@Test
	@DisplayName("toggleLike should add like when user has not liked the post")
	void toggleLikeAddLikeTest() {
		// GIVEN
		post.setUsersThatLiked(new ArrayList<>());
		user.setLikedPosts(new ArrayList<>());

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(mapper.toDTOWithLike(post, user)).thenReturn(postDTO);

		// WHEN
		PostDTO result = postService.toggleLike(1L, user);

		// THEN
		assertEquals(postDTO, result);
		assertTrue(user.getLikedPosts().contains(post));
		assertTrue(post.getUsersThatLiked().contains(user));
		assertEquals(1, post.getLikes());
		verify(postRepository).findById(1L);
	}

	@Test
	@DisplayName("toggleLike should remove like when user has already liked the post")
	void toggleLikeRemoveLikeTest() {
		// GIVEN
		post.setUsersThatLiked(new ArrayList<>(List.of(user)));
		user.setLikedPosts(new ArrayList<>(List.of(post)));

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(mapper.toDTOWithLike(post, user)).thenReturn(postDTO);

		// WHEN
		PostDTO result = postService.toggleLike(1L, user);

		// THEN
		assertEquals(postDTO, result);
		assertFalse(user.getLikedPosts().contains(post));
		assertFalse(post.getUsersThatLiked().contains(user));
		assertEquals(0, post.getLikes());
		verify(postRepository).findById(1L);
	}

	@Test
	@DisplayName("toggleLike should throw exception when post not found")
	void toggleLikePostNotFoundTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> {
			postService.toggleLike(1L, user);
		});
	}
}