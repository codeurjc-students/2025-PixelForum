package es.codeurjc.backend.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.Topic;
import es.codeurjc.backend.model.User;
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
	private PostMapper mapper;

	private Post post;
	private PostDTO postDTO;
	private User user;

	@BeforeEach
	void init() {

		postRepository = mock(PostRepository.class);
		mapper = mock(PostMapper.class);

		postService = new PostService(mapper, postRepository);

		user = new User();
		user.setId(1L);
		user.setRoles(List.of("USER"));

		post = new Post();
		post.setId(1L);
		post.setTitle("Title");
		post.setContent("Content");
		post.setImages(List.of("img1"));
		post.setAuthor(user);

		postDTO = mock(PostDTO.class);

		when(postDTO.title()).thenReturn("Title");
		when(postDTO.content()).thenReturn("Content");
		when(postDTO.images()).thenReturn(List.of("img1"));
		when(postDTO.topic()).thenReturn(null);
	}

	@Test
	@DisplayName("getPost should return PostDTO when post exists")
	void getPostSuccessTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(mapper.toDTO(post)).thenReturn(postDTO);

		// WHEN
		PostDTO result = postService.getPost(1L);

		// THEN
		assertEquals(postDTO, result);
		verify(postRepository).findById(1L);
		verify(mapper).toDTO(post);
	}

	@Test
	@DisplayName("getPost should throw exception when post not found")
	void getPostNotFoundTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> {
			postService.getPost(1L);
		});
	}

	@Test
	@DisplayName("searchAndFilterPosts should return mapped page")
	void searchPostsTest() {
		// GIVEN
		Page<Post> page = new PageImpl<>(List.of(post));

		when(postRepository.findByFilters(null, null, null, Pageable.unpaged()))
				.thenReturn(page);

		when(mapper.toDTO(post)).thenReturn(postDTO);

		// WHEN
		Page<PostDTO> result = postService.searchAndFilterPosts(null, null, null, Pageable.unpaged());

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
		when(mapper.toDTO(post)).thenReturn(postDTO);

		// WHEN
		PostDTO result = postService.createPost(postDTO, user);

		// THEN
		assertEquals(postDTO, result);
		verify(postRepository).save(any(Post.class));
		verify(mapper).toDTO(post);
	}

	@Test
	@DisplayName("updatePost should update post when author edits")
	void updatePostSuccessTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postRepository.save(post)).thenReturn(post);
		when(mapper.toDTO(post)).thenReturn(postDTO);

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
		when(mapper.toDTO(post)).thenReturn(postDTO);

		// WHEN
		PostDTO result = postService.updatePost(1L, postDTO, user);

		// THEN
		assertEquals(postDTO, result);
		verify(postRepository, never()).save(any());
	}

	@Test
	@DisplayName("updatePost should detect topic changes")
	void updatePostTopicChangedTest() {
		// GIVEN
		Topic topic = new Topic();
		topic.setId(2L);

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(postRepository.save(post)).thenReturn(post);
		when(mapper.toDTO(post)).thenReturn(postDTO);

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
}