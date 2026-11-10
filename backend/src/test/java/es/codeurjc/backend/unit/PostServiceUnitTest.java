package es.codeurjc.backend.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashSet;
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
import es.codeurjc.backend.model.Comment;
import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.Topic;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.CommentRepository;
import es.codeurjc.backend.repository.ImageRepository;
import es.codeurjc.backend.repository.PostRepository;
import es.codeurjc.backend.repository.TopicRepository;
import es.codeurjc.backend.service.PostService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.test.context.ActiveProfiles;

@Tag("unit")
@DisplayName("PostService Unitary tests")
@ActiveProfiles("test")
class PostServiceUnitTest {

	private PostService postService;
	private PostMapper mapper;
	private PostRepository postRepository;
	private ImageRepository imageRepository;
	private CommentRepository commentRepository;
	private TopicRepository topicRepository;

	private Post post;
	private PostDTO postDTO;
	private User user;

	@BeforeEach
	void init() {
		mapper = mock(PostMapper.class);
		postRepository = mock(PostRepository.class);
		imageRepository = mock(ImageRepository.class);
		commentRepository = mock(CommentRepository.class);
		topicRepository = mock(TopicRepository.class);

		postService = new PostService(mapper, postRepository, imageRepository, commentRepository, topicRepository);

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
	@DisplayName("createPost should normalize title and content before saving")
	void createPostNormalizesTest() {
		// GIVEN
		when(postDTO.title()).thenReturn("  My    title \n ");
		when(postDTO.content()).thenReturn("  Hello   world  \r\n\r\n\r\n\r\n bye ");
		when(mapper.toDomain(postDTO)).thenReturn(post);
		when(postRepository.save(any(Post.class))).thenReturn(post);
		when(mapper.toDTOWithLike(post, user)).thenReturn(postDTO);

		// WHEN
		postService.createPost(postDTO, user);

		// THEN
		assertEquals("My title", post.getTitle());
		assertEquals("Hello world\n\nbye", post.getContent());
	}

	@Test
	@DisplayName("createPost should save null content when content is null or blank")
	void createPostWithoutContentTest() {
		// GIVEN
		when(postDTO.content()).thenReturn("   ");
		when(mapper.toDomain(postDTO)).thenReturn(post);
		when(postRepository.save(any(Post.class))).thenReturn(post);
		when(mapper.toDTOWithLike(post, user)).thenReturn(postDTO);

		// WHEN
		postService.createPost(postDTO, user);

		// THEN
		assertNull(post.getContent());
	}

	@Test
	@DisplayName("createPost should throw IllegalArgumentException when title is blank")
	void createPostBlankTitleTest() {
		// GIVEN
		when(postDTO.title()).thenReturn("   ");
		when(mapper.toDomain(postDTO)).thenReturn(post);

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> postService.createPost(postDTO, user));
		verify(postRepository, never()).save(any());
	}

	@Test
	@DisplayName("createPost should throw IllegalArgumentException when title exceeds 300 characters")
	void createPostTitleTooLongTest() {
		// GIVEN
		when(postDTO.title()).thenReturn("a".repeat(301));
		when(mapper.toDomain(postDTO)).thenReturn(post);

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> postService.createPost(postDTO, user));
		verify(postRepository, never()).save(any());
	}

	@Test
	@DisplayName("createPost should throw IllegalArgumentException when content exceeds 5000 characters")
	void createPostContentTooLongTest() {
		// GIVEN
		when(postDTO.content()).thenReturn("a".repeat(5001));
		when(mapper.toDomain(postDTO)).thenReturn(post);

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> postService.createPost(postDTO, user));
		verify(postRepository, never()).save(any());
	}

	@Test
	@DisplayName("createPost should save post when images is null")
	void createPostNullImagesTest() {
		// GIVEN
		when(postDTO.images()).thenReturn(null);
		when(mapper.toDomain(postDTO)).thenReturn(post);
		when(postRepository.save(any(Post.class))).thenReturn(post);
		when(mapper.toDTOWithLike(post, user)).thenReturn(postDTO);

		// WHEN
		PostDTO result = postService.createPost(postDTO, user);

		// THEN
		assertEquals(postDTO, result);
		verify(imageRepository, never()).findAllById(any());
	}

	@Test
	@DisplayName("createPost should throw when topic does not exist")
	void createPostTopicNotFoundTest() {
		// GIVEN
		Topic topic = new Topic();
		topic.setId(2L);

		when(postDTO.topic()).thenReturn(topic);
		when(mapper.toDomain(postDTO)).thenReturn(post);
		when(topicRepository.findById(2L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> postService.createPost(postDTO, user));
		verify(postRepository, never()).save(any());
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
		when(topicRepository.findById(2L)).thenReturn(Optional.of(topic));
		when(postRepository.save(post)).thenReturn(post);

		when(postDTO.topic()).thenReturn(topic);

		// WHEN
		postService.updatePost(1L, postDTO, user);

		// THEN
		verify(postRepository).save(post);
	}

	@Test
	@DisplayName("updatePost should detect content change")
	void updatePostDetectContentChangeTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postDTO.title()).thenReturn(post.getTitle());
		when(postDTO.content()).thenReturn("NEW CONTENT");
		when(postDTO.topic()).thenReturn(null);

		when(postRepository.save(post)).thenReturn(post);

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
	@DisplayName("updatePost should allow admin to add foreign images to post")
	void updatePostAdminAddsForeignImageTest() {
		// GIVEN
		User admin = new User();
		admin.setId(10L);
		admin.setRoles(List.of("ADMIN"));

		User imageOwner = new User();
		imageOwner.setId(99L);

		Image img = new Image();
		img.setId(3L);
		img.setOwner(imageOwner);

		post.setImages(new ArrayList<>());

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postDTO.title()).thenReturn("New title");
		when(postDTO.images()).thenReturn(List.of(3L));
		when(imageRepository.findAllById(List.of(3L))).thenReturn(List.of(img));
		when(postRepository.save(any())).thenReturn(post);
		when(mapper.toDTOWithLike(any(), any())).thenReturn(postDTO);

		// WHEN & THEN
		assertDoesNotThrow(() -> postService.updatePost(1L, postDTO, admin));
	}

	@Test
	@DisplayName("updatePost should allow removing image that belongs to same post")
	void updatePostRemoveImageSamePostTest() {
		// GIVEN
		Image img = new Image();
		img.setId(1L);
		img.setOwner(user);
		img.setPost(post);

		post.setImages(new ArrayList<>(List.of(img)));

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postDTO.title()).thenReturn("New title");
		when(postDTO.images()).thenReturn(List.of());
		when(imageRepository.findAllById(List.of(1L))).thenReturn(List.of(img));
		when(imageRepository.findAllById(List.of())).thenReturn(List.of());
		when(postRepository.save(post)).thenReturn(post);
		when(mapper.toDTOWithLike(any(), any())).thenReturn(postDTO);

		// WHEN & THEN
		assertDoesNotThrow(() -> postService.updatePost(1L, postDTO, user));
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
		when(topicRepository.findById(2L)).thenReturn(Optional.of(newTopic));
		when(postDTO.topic()).thenReturn(newTopic);
		when(postRepository.save(post)).thenReturn(post);

		// WHEN
		postService.updatePost(1L, postDTO, user);

		// THEN
		assertEquals(newTopic, post.getTopic());
	}

	@Test
	@DisplayName("updatePost should keep existing images and only add/remove the differences")
	void updatePostKeepsExistingImagesTest() {
		// GIVEN
		Image removed = new Image();
		removed.setId(1L);
		removed.setOwner(user);

		Image kept = new Image();
		kept.setId(2L);
		kept.setOwner(user);
		kept.setPost(post);

		Image added = new Image();
		added.setId(3L);
		added.setOwner(user);

		post.setImages(new ArrayList<>(List.of(removed, kept)));

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postDTO.images()).thenReturn(List.of(2L, 3L));
		when(imageRepository.findAllById(List.of(1L))).thenReturn(List.of(removed));
		when(imageRepository.findAllById(List.of(3L))).thenReturn(List.of(added));
		when(postRepository.save(post)).thenReturn(post);

		// WHEN
		postService.updatePost(1L, postDTO, user);

		// THEN
		assertEquals(2, post.getImages().size());
		assertTrue(post.getImages().contains(kept));
		assertTrue(post.getImages().contains(added));
		assertFalse(post.getImages().contains(removed));
		verify(imageRepository).deleteAllById(List.of(1L));
		verify(imageRepository, never()).deleteAllById(List.of(2L));
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
	@DisplayName("updatePost should remove images when empty list provided")
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
	@DisplayName("updatePost should remove images when null provided")
	void updatePostRemoveImagesNullDtoTest() {
		// GIVEN
		Image img = new Image();
		img.setId(1L);
		img.setOwner(user);

		post.setImages(new ArrayList<>(List.of(img)));

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postDTO.images()).thenReturn(null);
		when(imageRepository.findAllById(List.of(1L))).thenReturn(List.of(img));
		when(postRepository.save(post)).thenReturn(post);
		when(mapper.toDTOWithLike(post, user)).thenReturn(postDTO);

		// WHEN
		PostDTO result = postService.updatePost(1L, postDTO, user);

		// THEN
		assertEquals(postDTO, result);
		assertTrue(post.getImages().isEmpty());
		verify(imageRepository).deleteAllById(List.of(1L));
		verify(postRepository).save(post);
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
	@DisplayName("updatePost should normalize title and content before saving")
	void updatePostNormalizesTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postRepository.save(post)).thenReturn(post);
		when(postDTO.title()).thenReturn("  New    title ");
		when(postDTO.content()).thenReturn("  New \r\n\r\n\r\n\r\n content  ");

		// WHEN
		postService.updatePost(1L, postDTO, user);

		// THEN
		assertEquals("New title", post.getTitle());
		assertEquals("New\n\ncontent", post.getContent());
	}

	@Test
	@DisplayName("updatePost should not save when title and content only differ by whitespace")
	void updatePostOnlyWhitespaceChangesTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(mapper.toDTOWithLike(post, user)).thenReturn(postDTO);
		when(postDTO.title()).thenReturn("  Title ");
		when(postDTO.content()).thenReturn("  Content  ");

		// WHEN
		postService.updatePost(1L, postDTO, user);

		// THEN
		verify(postRepository, never()).save(any());
	}

	@Test
	@DisplayName("updatePost should clear content when blank content is provided")
	void updatePostClearContentTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postRepository.save(post)).thenReturn(post);
		when(postDTO.content()).thenReturn("");

		// WHEN
		postService.updatePost(1L, postDTO, user);

		// THEN
		assertNull(post.getContent());
		verify(postRepository).save(post);
	}

	@Test
	@DisplayName("updatePost should validate before touching images")
	void updatePostInvalidDoesNotTouchImagesTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postDTO.title()).thenReturn("a".repeat(301));
		when(postDTO.images()).thenReturn(List.of(5L));

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> postService.updatePost(1L, postDTO, user));
		verify(imageRepository, never()).findAllById(any());
		verify(imageRepository, never()).deleteAllById(any());
		verify(postRepository, never()).save(any());
	}

	@Test
	@DisplayName("deletePost should succed and remove likes from comments and post")
	void deletePostShouldRemoveLikesTest() {
		// GIVEN
		Comment comment = new Comment();
		comment.setId(10L);

		post.setComments(List.of(comment));
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));

		// WHEN
		postService.deletePost(1L, user);

		// THEN
		verify(commentRepository).deleteLikesByCommentIds(List.of(10L));
		verify(postRepository).deleteLikesByPostIds(List.of(1L));
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
		post.setUsersThatLiked(new HashSet<>());
		user.setLikedPosts(new HashSet<>());

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
		post.setUsersThatLiked(new HashSet<>(List.of(user)));
		user.setLikedPosts(new HashSet<>(List.of(post)));

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