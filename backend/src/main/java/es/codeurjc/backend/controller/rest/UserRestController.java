package es.codeurjc.backend.controller.rest;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

import java.net.URI;
import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.codeurjc.backend.dto.comment.CommentDTO;
import es.codeurjc.backend.dto.post.PostDTO;
import es.codeurjc.backend.dto.user.BasicUserDTO;
import es.codeurjc.backend.dto.user.ChangePasswordDTO;
import es.codeurjc.backend.dto.user.CreateUserDTO;
import es.codeurjc.backend.dto.user.UserDTO;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {

	private final UserService userService;

	public UserRestController(UserService userService) {
		this.userService = userService;
	}

	private User getCurrentUser(Principal principal) {
		return userService.findByUsername(principal.getName())
				.orElseThrow(() -> new EntityNotFoundException("User not found"));
	}

	private User getCurrentUserOrNull(Principal principal) {
		if (principal == null) {
			return null;
		}
		return userService.findByUsername(principal.getName()).orElse(null);
	}

	@GetMapping
	public ResponseEntity<Page<BasicUserDTO>> getUsers(@PageableDefault(size = 10, page = 0) Pageable pageable) {
		return ResponseEntity.ok(userService.getUsers(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<BasicUserDTO> getUser(@PathVariable Long id) {
		return ResponseEntity.ok(userService.getUser(id));
	}

	@GetMapping("/{id}/details")
	public ResponseEntity<UserDTO> getUserDetails(@PathVariable Long id, Principal principal) {
		User currentUser = getCurrentUser(principal);
		return ResponseEntity.ok(userService.getUserDetails(id, currentUser));
	}

	@PostMapping
	public ResponseEntity<UserDTO> createUser(@RequestBody CreateUserDTO userDTO) {
		UserDTO createdUserDTO = userService.createUser(userDTO);
		URI location = fromCurrentRequest().path("/{id}").buildAndExpand(createdUserDTO.id()).toUri();
		return ResponseEntity.created(location).body(createdUserDTO);
	}

	@PatchMapping("/{id}")
	public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody CreateUserDTO userDTO,
			Principal principal) {
		User currentUser = getCurrentUser(principal);
		return ResponseEntity.ok(userService.updateUser(id, userDTO, currentUser));
	}

	@PatchMapping("/{id}/password")
	public ResponseEntity<Void> changePassword(@PathVariable Long id, @RequestBody ChangePasswordDTO changePasswordDTO,
			Principal principal) {
		User currentUser = getCurrentUser(principal);
		userService.changePassword(id, changePasswordDTO, currentUser);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable Long id, Principal principal) {
		User currentUser = getCurrentUser(principal);
		userService.deleteUser(id, currentUser);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/avatar")
	public ResponseEntity<UserDTO> setProfileImage(@PathVariable Long id, @RequestParam("imageId") Long imageId,
			Principal principal) {
		User currentUser = getCurrentUser(principal);
		return ResponseEntity.ok(userService.setProfileImage(id, imageId, currentUser));
	}

	@DeleteMapping("/{id}/avatar")
	public ResponseEntity<Void> removeProfileImage(@PathVariable Long id, Principal principal) {
		User currentUser = getCurrentUser(principal);
		userService.removeProfileImage(id, currentUser);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/liked-posts")
	public ResponseEntity<Page<PostDTO>> getLikedPosts(@PathVariable Long id, Principal principal,
			@PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		User currentUser = getCurrentUser(principal);
		Page<PostDTO> likedPosts = userService.getLikedPosts(id, currentUser, pageable);
		return ResponseEntity.ok(likedPosts);
	}

	@GetMapping("/{id}/liked-comments")
	public ResponseEntity<Page<CommentDTO>> getLikedComments(@PathVariable Long id, Principal principal,
			@PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		User currentUser = getCurrentUser(principal);
		Page<CommentDTO> likedComments = userService.getLikedComments(id, currentUser, pageable);
		return ResponseEntity.ok(likedComments);
	}

	@GetMapping("/{id}/comments")
	public ResponseEntity<Page<CommentDTO>> getUserComments(@PathVariable Long id,
			@PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
			Principal principal) {
		User currentUser = getCurrentUserOrNull(principal);
		Page<CommentDTO> comments = userService.getUserComments(id, currentUser, pageable);
		return ResponseEntity.ok(comments);
	}
}