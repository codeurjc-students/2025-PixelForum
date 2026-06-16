package es.codeurjc.backend.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import es.codeurjc.backend.dto.comment.CommentDTO;
import es.codeurjc.backend.dto.user.BasicUserDTO;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.service.CommentService;
import es.codeurjc.backend.service.PostService;
import es.codeurjc.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Tag("integration")
@DisplayName("CommentService Integration Tests")
@ActiveProfiles("test")
@SpringBootTest
class CommentServiceIntegrationTest {

	@Autowired
	private CommentService commentService;

	@Autowired
	private PostService postService;

	@Autowired
	private UserService userService;

	private User user;
	private User otherUser;
	private User admin;
	private Post post;

	@BeforeEach
	void setup() {
		user = userService.findByUsername("martin").orElseThrow();
		otherUser = userService.findByUsername("robert").orElseThrow();
		admin = userService.findByUsername("admin").orElseThrow();
		post = postService.findAll().get(0);
	}

	private CommentDTO buildCommentDTO(String content) {
		return new CommentDTO(null, content, null, null,
				new BasicUserDTO(user.getId(), user.getUsername(), null, null, null),
				null, 0, false, post.getId());
	}

	// =============== createComment ===============

	@Test
	@Transactional
	@DisplayName("Should create a comment successfully")
	void createCommentTest() {
		// GIVEN
		CommentDTO dto = buildCommentDTO("Integration test comment");

		// WHEN
		CommentDTO created = commentService.createComment(post.getId(), dto, user);

		// THEN
		assertNotNull(created.id(), "Created comment should have an ID");
		assertEquals("Integration test comment", created.content());
		assertEquals(user.getId(), created.author().id());
		assertEquals(post.getId(), created.post());
		assertNotNull(created.createdAt());
		assertNull(created.updatedAt());
		assertEquals(0, created.likes());
	}

	@Test
	@DisplayName("Should throw EntityNotFoundException when creating comment on non-existing post")
	void createCommentPostNotFoundTest() {
		// GIVEN
		CommentDTO dto = buildCommentDTO("Orphan comment");

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.createComment(-1L, dto, user));
	}

	// =============== getComment ===============

	@Test
	@Transactional
	@DisplayName("Should get a comment by ID")
	void getCommentTest() {
		// GIVEN
		CommentDTO created = commentService.createComment(post.getId(), buildCommentDTO("Fetch me"), user);

		// WHEN
		CommentDTO result = commentService.getComment(created.id(), user);

		// THEN
		assertEquals(created.id(), result.id());
		assertEquals("Fetch me", result.content());
	}

	@Test
	@DisplayName("Should throw EntityNotFoundException when getting non-existing comment")
	void getCommentNotFoundTest() {
		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.getComment(-1L, user));
	}

	// =============== getPostComments ===============

	@Test
	@Transactional
	@DisplayName("Should return paginated comments for a post")
	void getPostCommentsTest() {
		// GIVEN
		commentService.createComment(post.getId(), buildCommentDTO("Comment A"), user);
		commentService.createComment(post.getId(), buildCommentDTO("Comment B"), user);
		Pageable pageable = PageRequest.of(0, 10);

		// WHEN
		Page<CommentDTO> page = commentService.getPostComments(post.getId(), pageable, user);

		// THEN
		assertFalse(page.isEmpty());
		assertTrue(page.getContent().stream().allMatch(c -> c.post().equals(post.getId())));
	}

	@Test
	@DisplayName("Should throw EntityNotFoundException when getting comments for non-existing post")
	void getPostCommentsPostNotFoundTest() {
		// GIVEN
		Pageable pageable = PageRequest.of(0, 10);

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.getPostComments(-1L, pageable, user));
	}

	// =============== updateComment ===============

	@Test
	@Transactional
	@DisplayName("Should update comment content when author edits it")
	void updateCommentSuccessTest() {
		// GIVEN
		CommentDTO created = commentService.createComment(post.getId(), buildCommentDTO("Original"), user);
		CommentDTO updateDTO = new CommentDTO(created.id(), "Edited content", null, null, null, null, 0, false,
				post.getId());

		// WHEN
		CommentDTO updated = commentService.updateComment(created.id(), updateDTO, user);

		// THEN
		assertEquals("Edited content", updated.content());
		assertNotNull(updated.updatedAt(), "updatedAt should be set after edit");
	}

	@Test
	@Transactional
	@DisplayName("Should allow admin to update any comment")
	void updateCommentAsAdminTest() {
		// GIVEN
		CommentDTO created = commentService.createComment(post.getId(), buildCommentDTO("Original"), user);
		CommentDTO updateDTO = new CommentDTO(created.id(), "Admin edit", null, null, null, null, 0, false,
				post.getId());

		// WHEN
		CommentDTO updated = commentService.updateComment(created.id(), updateDTO, admin);

		// THEN
		assertEquals("Admin edit", updated.content());
	}

	@Test
	@Transactional
	@DisplayName("Should not update or set updatedAt when content is unchanged")
	void updateCommentNoChangesTest() {
		// GIVEN
		CommentDTO created = commentService.createComment(post.getId(), buildCommentDTO("Same content"), user);
		CommentDTO sameDTO = new CommentDTO(created.id(), "Same content", null, null, null, null, 0, false,
				post.getId());

		// WHEN
		CommentDTO result = commentService.updateComment(created.id(), sameDTO, user);

		// THEN
		assertEquals("Same content", result.content());
		assertNull(result.updatedAt(), "updatedAt should remain null when content does not change");
	}

	@Test
	@Transactional
	@DisplayName("Should throw AccessDeniedException when non-author tries to update comment")
	void updateCommentAccessDeniedTest() {
		// GIVEN
		CommentDTO created = commentService.createComment(post.getId(), buildCommentDTO("Protected"), user);
		CommentDTO updateDTO = new CommentDTO(created.id(), "Hijacked", null, null, null, null, 0, false, post.getId());

		long commentId = created.id();

		// WHEN & THEN
		assertThrows(AccessDeniedException.class, () -> commentService.updateComment(commentId, updateDTO, otherUser));
	}

	// =============== deleteComment ===============

	@Test
	@Transactional
	@DisplayName("Should delete comment when author deletes it")
	void deleteCommentSuccessTest() {
		// GIVEN
		CommentDTO created = commentService.createComment(post.getId(), buildCommentDTO("To delete"), user);
		long commentId = created.id();

		// WHEN
		commentService.deleteComment(commentId, user);

		// THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.getComment(commentId, user));
	}

	@Test
	@Transactional
	@DisplayName("Should allow admin to delete any comment")
	void deleteCommentAsAdminTest() {
		// GIVEN
		CommentDTO created = commentService.createComment(post.getId(), buildCommentDTO("Admin delete"), user);
		long commentId = created.id();

		// WHEN
		commentService.deleteComment(commentId, admin);

		// THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.getComment(commentId, user));
	}

	@Test
	@Transactional
	@DisplayName("Should throw AccessDeniedException when non-author tries to delete comment")
	void deleteCommentAccessDeniedTest() {
		// GIVEN
		CommentDTO created = commentService.createComment(post.getId(), buildCommentDTO("Protected"), user);
		long commentId = created.id();

		// WHEN & THEN
		assertThrows(AccessDeniedException.class, () -> commentService.deleteComment(commentId, otherUser));
	}

	@Test
	@DisplayName("Should throw EntityNotFoundException when deleting non-existing comment")
	void deleteCommentNotFoundTest() {
		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.deleteComment(-1L, user));
	}

	// =============== toggleLike ===============

	@Test
	@Transactional
	@DisplayName("Should add like when user has not liked the comment")
	void toggleLikeAddTest() {
		// GIVEN
		CommentDTO created = commentService.createComment(post.getId(), buildCommentDTO("Like me"), user);

		// WHEN
		CommentDTO result = commentService.toggleLike(created.id(), otherUser);

		// THEN
		assertEquals(1, result.likes());
		assertTrue(result.hasUserLiked());
	}

	@Test
	@Transactional
	@DisplayName("Should remove like when user has already liked the comment")
	void toggleLikeRemoveTest() {
		// GIVEN
		CommentDTO created = commentService.createComment(post.getId(), buildCommentDTO("Unlike me"), user);
		commentService.toggleLike(created.id(), otherUser);

		// WHEN
		CommentDTO result = commentService.toggleLike(created.id(), otherUser);

		// THEN
		assertEquals(0, result.likes());
		assertFalse(result.hasUserLiked());
	}

	@Test
	@DisplayName("Should throw EntityNotFoundException when liking non-existing comment")
	void toggleLikeNotFoundTest() {
		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> commentService.toggleLike(-1L, user));
	}
}