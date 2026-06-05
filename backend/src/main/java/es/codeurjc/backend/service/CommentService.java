package es.codeurjc.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.codeurjc.backend.dto.comment.CommentDTO;
import es.codeurjc.backend.dto.comment.CommentMapper;
import es.codeurjc.backend.model.Comment;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.CommentRepository;
import es.codeurjc.backend.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class CommentService {

	private final CommentMapper mapper;
	private final CommentRepository commentRepository;
	private final PostRepository postRepository;

	public CommentService(CommentMapper mapper, CommentRepository commentRepository, PostRepository postRepository) {
		this.mapper = mapper;
		this.commentRepository = commentRepository;
		this.postRepository = postRepository;
	}

	public Comment findById(Long id) {
		return commentRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Comment not found"));
	}

	public Comment save(Comment comment) {
		return commentRepository.save(comment);
	}

	private CommentDTO toDTO(Comment comment, User currentUser) {
		 return mapper.toDTOWithLike(comment, currentUser);
	}

	public CommentDTO getComment(Long id, User currentUser) {
		return toDTO(findById(id), currentUser);
	}

	public Page<CommentDTO> getPostComments(Long postId, Pageable pageable, User currentUser) {
		if (!postRepository.existsById(postId)) {
			throw new EntityNotFoundException("Post not found");
		}
		Page<Comment> commentsPage = commentRepository.findByPostIdAndParentCommentIsNull(postId, pageable);
		return commentsPage.map(comment -> toDTO(comment, currentUser));
	}

	public CommentDTO createComment(Long postId, CommentDTO commentDTO, User user) {
		Post post = postRepository.findById(postId).orElseThrow(() -> new EntityNotFoundException("Post not found"));

		Comment comment = mapper.toDomain(commentDTO);
		comment.setCreatedAt(LocalDateTime.now());
		comment.setUpdatedAt(null);
		comment.setLikes(0);
		comment.setAuthor(user);
		comment.setPost(post);

		Comment savedComment = commentRepository.save(comment);
		return toDTO(savedComment, user);
	}

	@Transactional
	public CommentDTO updateComment(Long commentId, CommentDTO commentDTO, User user) {
		Comment comment = findById(commentId);

		if (comment.getAuthor().getId() != user.getId() && !user.getRoles().contains("ADMIN")) {
			throw new AccessDeniedException("You can only edit your own comments");
		}

		if (Objects.equals(comment.getContent(), commentDTO.content())) {
			return toDTO(comment, user);
		}

		comment.setContent(commentDTO.content());
		comment.setUpdatedAt(LocalDateTime.now());

		Comment updatedComment = commentRepository.save(comment);
		return toDTO(updatedComment, user);
	}

	@Transactional
	public void deleteComment(Long commentId, User user) {
		Comment comment = findById(commentId);
		if (comment.getAuthor().getId() != user.getId() && !user.getRoles().contains("ADMIN")) {
			throw new AccessDeniedException("You can only delete your own comments");
		}
		commentRepository.deleteLikesByCommentIds(List.of(commentId));
		commentRepository.delete(comment);
	}

	@Transactional
	public CommentDTO toggleLike(Long commentId, User user) {
		Comment comment = findById(commentId);

		boolean hasLiked = user.getLikedComments().contains(comment);
		if (hasLiked) {
			user.getLikedComments().remove(comment);
			comment.getUsersThatLiked().remove(user);
		} else {
			user.getLikedComments().add(comment);
			comment.getUsersThatLiked().add(user);
		}
		comment.setLikes(comment.getUsersThatLiked().size());
		return toDTO(comment, user);
	}
}