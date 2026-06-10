package es.codeurjc.backend.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import es.codeurjc.backend.dto.comment.CommentDTO;
import es.codeurjc.backend.dto.comment.CommentMapper;
import es.codeurjc.backend.dto.user.BasicUserDTO;
import es.codeurjc.backend.model.Comment;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.CommentRepository;
import es.codeurjc.backend.repository.PostRepository;
import es.codeurjc.backend.service.CommentService;
import jakarta.persistence.EntityNotFoundException;

@Tag("unit")
@DisplayName("CommentService Unitary tests")
class CommentServiceUnitTest {

	private CommentService commentService;

	private CommentMapper mapper;
	private CommentRepository commentRepository;
	private PostRepository postRepository;

	private User user;
	private User admin;
	private User otherUser;
	private Post post;
	private Comment comment;
	private CommentDTO commentDTO;

	@BeforeEach
	void init() {
		mapper = mock(CommentMapper.class);
		commentRepository = mock(CommentRepository.class);
		postRepository = mock(PostRepository.class);

		commentService = new CommentService(mapper, commentRepository, postRepository);

		user = new User();
		user.setId(1L);
		user.setUsername("testuser");
		user.setRoles(List.of("USER"));
		user.setLikedComments(new HashSet<>());

		admin = new User();
		admin.setId(2L);
		admin.setUsername("admin");
		admin.setRoles(List.of("USER", "ADMIN"));
		admin.setLikedComments(new HashSet<>());

		otherUser = new User();
		otherUser.setId(99L);
		otherUser.setUsername("otheruser");
		otherUser.setRoles(List.of("USER"));
		otherUser.setLikedComments(new HashSet<>());

		post = new Post();
		post.setId(1L);
		post.setAuthor(user);

		comment = new Comment();
		comment.setId(1L);
		comment.setContent("Test comment");
		comment.setAuthor(user);
		comment.setPost(post);
		comment.setLikes(0);
		comment.setUsersThatLiked(new HashSet<>());

		commentDTO = new CommentDTO(1L, "Test comment", LocalDateTime.now(), LocalDateTime.now(),
				new BasicUserDTO(1L, "testuser", LocalDateTime.now(), "Bio", null), null, 0, false, 1L);
	}

	// =============== findById ===============

	@Test
	@DisplayName("findById should return comment when it exists")
	void findByIdSuccessTest() {
		// GIVEN
		when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

		// WHEN
		Comment result = commentService.findById(1L);

		// THEN
		assertEquals(comment, result);
		verify(commentRepository).findById(1L);
	}

	@Test
	@DisplayName("findById should throw EntityNotFoundException when comment does not exist")
	void findByIdNotFoundTest() {
		// GIVEN
		when(commentRepository.findById(999L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.findById(999L));
		verify(commentRepository).findById(999L);
	}

	// =============== save ===============

	@Test
	@DisplayName("save should persist and return comment")
	void saveTest() {
		// GIVEN
		when(commentRepository.save(comment)).thenReturn(comment);

		// WHEN
		Comment result = commentService.save(comment);

		// THEN
		assertEquals(comment, result);
		verify(commentRepository).save(comment);
	}

	// =============== getComment ===============

	@Test
	@DisplayName("getComment should return mapped DTO when comment exists")
	void getCommentSuccessTest() {
		// GIVEN
		when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
		when(mapper.toDTOWithLike(comment, user)).thenReturn(commentDTO);

		// WHEN
		CommentDTO result = commentService.getComment(1L, user);

		// THEN
		assertEquals(commentDTO, result);
		verify(commentRepository).findById(1L);
		verify(mapper).toDTOWithLike(comment, user);
	}

	@Test
	@DisplayName("getComment should throw EntityNotFoundException when comment does not exist")
	void getCommentNotFoundTest() {
		// GIVEN
		when(commentRepository.findById(999L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.getComment(999L, user));
		verify(mapper, never()).toDTOWithLike(any(), any());
	}

	// =============== getPostComments ===============

	@Test
	@DisplayName("getPostComments should return page of comments for existing post")
	void getPostCommentsSuccessTest() {
		// GIVEN
		Pageable pageable = PageRequest.of(0, 10);
		Page<Comment> commentPage = new PageImpl<>(List.of(comment), pageable, 1);

		when(postRepository.existsById(1L)).thenReturn(true);
		when(commentRepository.findByPostIdAndParentCommentIsNull(1L, pageable)).thenReturn(commentPage);
		when(mapper.toDTOWithLike(comment, user)).thenReturn(commentDTO);

		// WHEN
		Page<CommentDTO> result = commentService.getPostComments(1L, pageable, user);

		// THEN
		assertEquals(1, result.getContent().size());
		assertEquals(commentDTO, result.getContent().get(0));
		verify(postRepository).existsById(1L);
		verify(commentRepository).findByPostIdAndParentCommentIsNull(1L, pageable);
		verify(mapper).toDTOWithLike(comment, user);
	}

	@Test
	@DisplayName("getPostComments should return empty page when post has no comments")
	void getPostCommentsEmptyTest() {
		// GIVEN
		Pageable pageable = PageRequest.of(0, 10);
		Page<Comment> emptyPage = new PageImpl<>(List.of(), pageable, 0);

		when(postRepository.existsById(1L)).thenReturn(true);
		when(commentRepository.findByPostIdAndParentCommentIsNull(1L, pageable)).thenReturn(emptyPage);

		// WHEN
		Page<CommentDTO> result = commentService.getPostComments(1L, pageable, user);

		// THEN
		assertTrue(result.isEmpty());
		verify(mapper, never()).toDTOWithLike(any(), any());
	}

	@Test
	@DisplayName("getPostComments should throw EntityNotFoundException when post does not exist")
	void getPostCommentsPostNotFoundTest() {
		// GIVEN
		Pageable pageable = PageRequest.of(0, 10);
		when(postRepository.existsById(999L)).thenReturn(false);

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.getPostComments(999L, pageable, user));
		verify(commentRepository, never()).findByPostIdAndParentCommentIsNull(any(), any());
	}

	// =============== createComment ===============

	@Test
	@DisplayName("createComment should create and return comment DTO")
	void createCommentSuccessTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(mapper.toDomain(commentDTO)).thenReturn(comment);
		when(commentRepository.save(any(Comment.class))).thenReturn(comment);
		when(mapper.toDTOWithLike(comment, user)).thenReturn(commentDTO);

		// WHEN
		CommentDTO result = commentService.createComment(1L, commentDTO, user);

		// THEN
		assertEquals(commentDTO, result);
		verify(postRepository).findById(1L);
		verify(commentRepository).save(any(Comment.class));
		verify(mapper).toDTOWithLike(comment, user);
	}

	@Test
	@DisplayName("createComment should initialize default fields on the comment")
	void createCommentInitializesFieldsTest() {
		// GIVEN
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(mapper.toDomain(commentDTO)).thenReturn(comment);
		when(commentRepository.save(any(Comment.class))).thenReturn(comment);
		when(mapper.toDTOWithLike(comment, user)).thenReturn(commentDTO);

		// WHEN
		commentService.createComment(1L, commentDTO, user);

		// THEN
		assertNotNull(comment.getCreatedAt());
		assertNull(comment.getUpdatedAt());
		assertEquals(0, comment.getLikes());
		assertEquals(user, comment.getAuthor());
		assertEquals(post, comment.getPost());
	}

	@Test
	@DisplayName("createComment should throw EntityNotFoundException when post does not exist")
	void createCommentPostNotFoundTest() {
		// GIVEN
		when(postRepository.findById(999L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.createComment(999L, commentDTO, user));
		verify(commentRepository, never()).save(any());
	}

	// =============== updateComment ===============

	@Test
	@DisplayName("updateComment should update content when author edits")
	void updateCommentSuccessTest() {
		// GIVEN
		CommentDTO updatedDTO = new CommentDTO(1L, "Updated content", LocalDateTime.now(), LocalDateTime.now(),
				new BasicUserDTO(1L, "testuser", LocalDateTime.now(), "Bio", null), null, 0, false, 1L);

		when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
		when(commentRepository.save(comment)).thenReturn(comment);
		when(mapper.toDTOWithLike(comment, user)).thenReturn(updatedDTO);

		// WHEN
		CommentDTO result = commentService.updateComment(1L, updatedDTO, user);

		// THEN
		assertEquals(updatedDTO, result);
		assertEquals("Updated content", comment.getContent());
		assertNotNull(comment.getUpdatedAt());
		verify(commentRepository).save(comment);
		verify(mapper).toDTOWithLike(comment, user);
	}

	@Test
	@DisplayName("updateComment should allow admin to update any comment")
	void updateCommentAdminTest() {
		// GIVEN
		CommentDTO updatedDTO = new CommentDTO(1L, "Updated by admin", LocalDateTime.now(), LocalDateTime.now(),
				new BasicUserDTO(1L, "testuser", LocalDateTime.now(), "Bio", null), null, 0, false, 1L);

		when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
		when(commentRepository.save(comment)).thenReturn(comment);
		when(mapper.toDTOWithLike(comment, admin)).thenReturn(updatedDTO);

		// WHEN
		CommentDTO result = commentService.updateComment(1L, updatedDTO, admin);

		// THEN
		assertEquals(updatedDTO, result);
		verify(commentRepository).save(comment);
	}

	@Test
	@DisplayName("updateComment should return same DTO without saving when content is unchanged")
	void updateCommentNoChangesTest() {
		// GIVEN
		CommentDTO sameContentDTO = new CommentDTO(1L, "Test comment", LocalDateTime.now(), LocalDateTime.now(),
				new BasicUserDTO(1L, "testuser", LocalDateTime.now(), "Bio", null), null, 0, false, 1L);

		when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
		when(mapper.toDTOWithLike(comment, user)).thenReturn(sameContentDTO);

		// WHEN
		CommentDTO result = commentService.updateComment(1L, sameContentDTO, user);

		// THEN
		assertEquals(sameContentDTO, result);
		verify(commentRepository, never()).save(any());
	}

	@Test
	@DisplayName("updateComment should throw AccessDeniedException when user is not author or admin")
	void updateCommentAccessDeniedTest() {
		// GIVEN
		when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

		// WHEN & THEN
		assertThrows(AccessDeniedException.class, () -> commentService.updateComment(1L, commentDTO, otherUser));
		verify(commentRepository, never()).save(any());
	}

	@Test
	@DisplayName("updateComment should throw EntityNotFoundException when comment does not exist")
	void updateCommentNotFoundTest() {
		// GIVEN
		when(commentRepository.findById(999L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.updateComment(999L, commentDTO, user));
	}

	// =============== deleteComment ===============

	@Test
	@DisplayName("deleteComment should delete comment when user is author")
	void deleteCommentSuccessTest() {
		// GIVEN
		when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

		// WHEN
		commentService.deleteComment(1L, user);

		// THEN
		verify(commentRepository).deleteLikesByCommentIds(List.of(1L));
		verify(commentRepository).delete(comment);
	}

	@Test
	@DisplayName("deleteComment should allow admin to delete any comment")
	void deleteCommentAdminTest() {
		// GIVEN
		when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

		// WHEN
		commentService.deleteComment(1L, admin);

		// THEN
		verify(commentRepository).deleteLikesByCommentIds(List.of(1L));
		verify(commentRepository).delete(comment);
	}

	@Test
	@DisplayName("deleteComment should delete likes before deleting the comment")
	void deleteCommentDeletesLikesFirstTest() {
		// GIVEN
		when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

		// WHEN
		commentService.deleteComment(1L, user);

		// THEN
		// Verify order: likes deleted before the comment itself
		var inOrder = inOrder(commentRepository);
		inOrder.verify(commentRepository).deleteLikesByCommentIds(List.of(1L));
		inOrder.verify(commentRepository).delete(comment);
	}

	@Test
	@DisplayName("deleteComment should throw AccessDeniedException when user is not author or admin")
	void deleteCommentAccessDeniedTest() {
		// GIVEN
		when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

		// WHEN & THEN
		assertThrows(AccessDeniedException.class, () -> commentService.deleteComment(1L, otherUser));
		verify(commentRepository, never()).delete(any());
		verify(commentRepository, never()).deleteLikesByCommentIds(any());
	}

	@Test
	@DisplayName("deleteComment should throw EntityNotFoundException when comment does not exist")
	void deleteCommentNotFoundTest() {
		// GIVEN
		when(commentRepository.findById(999L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.deleteComment(999L, user));
		verify(commentRepository, never()).delete(any());
	}

	// =============== toggleLike ===============

	@Test
	@DisplayName("toggleLike should add like when user has not liked the comment")
	void toggleLikeAddLikeTest() {
		// GIVEN
		when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
		when(mapper.toDTOWithLike(comment, user)).thenReturn(commentDTO);

		// WHEN
		CommentDTO result = commentService.toggleLike(1L, user);

		// THEN
		assertEquals(commentDTO, result);
		assertTrue(user.getLikedComments().contains(comment));
		assertTrue(comment.getUsersThatLiked().contains(user));
		assertEquals(1, comment.getLikes());
		verify(commentRepository).findById(1L);
		verify(mapper).toDTOWithLike(comment, user);
	}

	@Test
	@DisplayName("toggleLike should remove like when user has already liked the comment")
	void toggleLikeRemoveLikeTest() {
		// GIVEN
		user.getLikedComments().add(comment);
		comment.getUsersThatLiked().add(user);
		comment.setLikes(1);

		when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
		when(mapper.toDTOWithLike(comment, user)).thenReturn(commentDTO);

		// WHEN
		CommentDTO result = commentService.toggleLike(1L, user);

		// THEN
		assertEquals(commentDTO, result);
		assertFalse(user.getLikedComments().contains(comment));
		assertFalse(comment.getUsersThatLiked().contains(user));
		assertEquals(0, comment.getLikes());
		verify(commentRepository).findById(1L);
	}

	@Test
	@DisplayName("toggleLike should update likes count to match usersThatLiked size")
	void toggleLikeSyncsLikesCountTest() {
		// GIVEN
		User anotherUser = new User();
		anotherUser.setId(50L);
		anotherUser.setLikedComments(new HashSet<>());
		comment.getUsersThatLiked().add(anotherUser);
		comment.setLikes(1);

		when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
		when(mapper.toDTOWithLike(comment, user)).thenReturn(commentDTO);

		// WHEN
		commentService.toggleLike(1L, user);

		// THEN
		assertEquals(comment.getUsersThatLiked().size(), comment.getLikes());
	}

	@Test
	@DisplayName("toggleLike should throw EntityNotFoundException when comment does not exist")
	void toggleLikeNotFoundTest() {
		// GIVEN
		when(commentRepository.findById(999L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.toggleLike(999L, user));
		verify(mapper, never()).toDTOWithLike(any(), any());
	}
}