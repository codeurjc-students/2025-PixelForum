package es.codeurjc.backend.controller.rest;

import java.net.URI;
import java.security.Principal;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.codeurjc.backend.dto.comment.CommentDTO;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.service.CommentService;
import es.codeurjc.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
public class CommentRestController {

    private static final String USER_NOT_FOUND = "User not found";

    private final CommentService commentService;
    private final UserService userService;

    public CommentRestController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    private User getCurrentUser(Principal principal) {
        return userService.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<Page<CommentDTO>> getPostComments(@PathVariable Long postId,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal) {
        User user = null;
        if (principal != null) {
            user = userService.findByUsername(principal.getName()).orElse(null);
        }
        Page<CommentDTO> page = commentService.getPostComments(postId, pageable, user);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentDTO> getComment(@PathVariable Long postId, @PathVariable Long commentId,
            Principal principal) {
        User user = null;
        if (principal != null) {
            user = userService.findByUsername(principal.getName()).orElse(null);
        }
        CommentDTO commentDTO = commentService.getComment(commentId, user);
        return ResponseEntity.ok(commentDTO);
    }

    @PostMapping
    public ResponseEntity<CommentDTO> createComment(@PathVariable Long postId, @RequestBody CommentDTO commentDTO,
            Principal principal) {
        User currentUser = getCurrentUser(principal);
        CommentDTO createdCommentDTO = commentService.createComment(postId, commentDTO, currentUser);

        URI location = fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCommentDTO.id())
                .toUri();

        return ResponseEntity.created(location).body(createdCommentDTO);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDTO> updateComment(@PathVariable Long postId, @PathVariable Long commentId,
            @RequestBody CommentDTO commentDTO, Principal principal) {
        User currentUser = getCurrentUser(principal);
        CommentDTO updatedCommentDTO = commentService.updateComment(commentId, commentDTO, currentUser);
        return ResponseEntity.ok(updatedCommentDTO);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long postId, @PathVariable Long commentId,
            Principal principal) {
        User currentUser = getCurrentUser(principal);
        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<CommentDTO> toggleLike(@PathVariable Long postId, @PathVariable Long commentId,
            Principal principal) {
        User currentUser = getCurrentUser(principal);
        CommentDTO likedCommentDTO = commentService.toggleLike(commentId, currentUser);
        return ResponseEntity.ok(likedCommentDTO);
    }
}