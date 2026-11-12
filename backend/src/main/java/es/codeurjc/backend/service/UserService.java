package es.codeurjc.backend.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.codeurjc.backend.dto.comment.CommentDTO;
import es.codeurjc.backend.dto.comment.CommentMapper;
import es.codeurjc.backend.dto.post.PostDTO;
import es.codeurjc.backend.dto.post.PostMapper;
import es.codeurjc.backend.dto.user.BasicUserDTO;
import es.codeurjc.backend.dto.user.ChangePasswordDTO;
import es.codeurjc.backend.dto.user.CreateUserDTO;
import es.codeurjc.backend.dto.user.UserDTO;
import es.codeurjc.backend.dto.user.UserMapper;
import es.codeurjc.backend.model.Comment;
import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.CommentRepository;
import es.codeurjc.backend.repository.ImageRepository;
import es.codeurjc.backend.repository.PostRepository;
import es.codeurjc.backend.repository.UserRepository;
import es.codeurjc.backend.utils.TextRules;
import jakarta.persistence.EntityNotFoundException;

@Service
public class UserService {

	private static final String ADMIN = "ADMIN";
	private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png");

	private final UserMapper mapper;
	private final UserRepository userRepository;
	private final PostRepository postRepository;
	private final PostMapper postMapper;
	private final CommentRepository commentRepository;
	private final CommentMapper commentMapper;
	private final ImageRepository imageRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserMapper mapper, UserRepository userRepository, PostRepository postRepository,
			PostMapper postMapper, CommentRepository commentRepository, CommentMapper commentMapper,
			ImageRepository imageRepository, PasswordEncoder passwordEncoder) {
		this.mapper = mapper;
		this.userRepository = userRepository;
		this.postRepository = postRepository;
		this.postMapper = postMapper;
		this.commentRepository = commentRepository;
		this.commentMapper = commentMapper;
		this.imageRepository = imageRepository;
		this.passwordEncoder = passwordEncoder;
	}

	private User findById(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
	}

	public BasicUserDTO getUser(Long id) {
		return mapper.toBasicDTO(findById(id));
	}

	public Page<BasicUserDTO> getUsers(Pageable pageable) {
		return userRepository.findAll(pageable).map(mapper::toBasicDTO);
	}

	public UserDTO getUserDetails(Long id, User currentUser) {
		if (id != currentUser.getId() && !currentUser.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only view your own secret details");
		}
		return mapper.toDTO(findById(id));
	}

	public Optional<User> findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	public User addUser(User user) {
		String password = user.getPassword();
		String encodedPassword = passwordEncoder.encode(password);
		user.setPassword(encodedPassword);
		if (user.getUsername().equals("admin")) {
			user.setRoles(List.of("USER", ADMIN));
		} else {
			user.setRoles(List.of("USER"));
		}
		return userRepository.save(user);
	}

	public User save(User user) {
		return userRepository.save(user);
	}

	@Transactional
	public UserDTO createUser(CreateUserDTO userDTO) {
		String username = TextRules.username(userDTO.username());
		String email = TextRules.email(userDTO.email());

		if (userRepository.findByUsername(username).isPresent()) {
			throw new IllegalArgumentException("Username already taken");
		}
		if (userRepository.findByEmail(email).isPresent()) {
			throw new IllegalArgumentException("Email already taken");
		}
		User user = mapper.toDomain(userDTO);

		user.setUsername(username);
		user.setEmail(email);
		user.setBio(TextRules.optionalMultiline("The bio", userDTO.bio(), 300, 5));
		user.setPassword(passwordEncoder.encode(TextRules.password(userDTO.password())));
		user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
		user.setRoles(List.of("USER"));

		return mapper.toDTO(userRepository.save(user));
	}

	@Transactional
	public UserDTO updateUser(Long id, CreateUserDTO userDTO, User currentUser) {
		if (id != currentUser.getId() && !currentUser.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only edit your own profile");
		}
		User user = findById(id);

		// USERNAME
		if (userDTO.username() != null) {
			String username = TextRules.username(userDTO.username());
			if (!user.getUsername().equals(username)) {
				if (userRepository.findByUsername(username).isPresent()) {
					throw new IllegalArgumentException("Username already taken");
				}
				user.setUsername(username);
			}
		}

		// EMAIL
		if (userDTO.email() != null) {
			String email = TextRules.email(userDTO.email());
			if (!user.getEmail().equals(email)) {
				if (userRepository.findByEmail(email).isPresent()) {
					throw new IllegalArgumentException("Email already taken");
				}
				user.setEmail(email);
			}
		}

		// BIO
		if (userDTO.bio() != null) {
			user.setBio(TextRules.optionalMultiline("The bio", userDTO.bio(), 300, 5));
		}

		return mapper.toDTO(userRepository.save(user));
	}

	@Transactional
	public void changePassword(Long id, ChangePasswordDTO dto, User currentUser) {
		if (id != currentUser.getId() && !currentUser.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only change your own password");
		}
		User user = findById(id);

		if (!passwordEncoder.matches(dto.oldPassword(), user.getPassword())) {
			throw new IllegalArgumentException("Invalid current password");
		}

		user.setPassword(passwordEncoder.encode(TextRules.password(dto.newPassword())));
	}

	@Transactional
	public void deleteUser(Long id, User currentUser) {
		if (id != currentUser.getId() && !currentUser.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only delete your own account");
		}
		User user = findById(id);

		removeLikes(user);
		commentRepository.deleteByAuthor(user);
		postRepository.deleteByAuthor(user);

		if (user.getAvatar() != null) {
			Image avatar = user.getAvatar();
			user.setAvatar(null);
			imageRepository.delete(avatar);
		}
		imageRepository.deleteByOwner(user);

		userRepository.delete(user);
	}

	private void removeLikes(User user) {
		// Delete likes given from this user
		user.getLikedPosts().forEach(p -> {
			p.getUsersThatLiked().remove(user);
			p.setLikes(p.getUsersThatLiked().size());
		});
		user.getLikedComments().forEach(c -> {
			c.getUsersThatLiked().remove(user);
			c.setLikes(c.getUsersThatLiked().size());
		});
		user.getLikedPosts().clear();
		user.getLikedComments().clear();

		// Delete likes from posts owned by this user
		List<Post> posts = postRepository.findByAuthor(user);
		List<Long> postIds = posts.stream().map(Post::getId).toList();
		postRepository.deleteLikesByPostIds(postIds);

		// Delete likes from comments owned by this user and comments on posts owned by
		// this user
		List<Comment> comments = commentRepository.findByAuthor(user);
		Set<Long> commentIds = new HashSet<>();
		commentIds.addAll(posts.stream().flatMap(p -> p.getComments().stream()).map(Comment::getId).toList());
		commentIds.addAll(comments.stream().map(Comment::getId).toList());
		commentRepository.deleteLikesByCommentIds(new ArrayList<>(commentIds));
	}

	@Transactional(noRollbackFor = IllegalArgumentException.class)
	public UserDTO setProfileImage(Long id, Long imageId, User currentUser) {
		if (id != currentUser.getId() && !currentUser.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only set your own profile image");
		}
		User user = findById(id);

		Image image = imageRepository.findById(imageId)
				.orElseThrow(() -> new EntityNotFoundException("Image not found"));

		// Validate ownership of the image
		if (image.getOwner().getId() != currentUser.getId() && !currentUser.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only set your own images as profile picture");
		}

		if (image.getPost() != null) {
			throw new IllegalArgumentException("Image is associated with a post and cannot be used as profile picture");
		}

		// Already the current avatar
		if (user.getAvatar() != null && user.getAvatar().getId() == image.getId()) {
			return mapper.toDTO(user);
		}

		// Validate image type
		if (!ALLOWED_AVATAR_TYPES.contains(image.getContentType())) {
			imageRepository.delete(image);
			throw new IllegalArgumentException(
					"Profile pictures must be JPG or PNG format. WebP and other formats are not allowed.");

		}

		// Remove old avatar if exists
		if (user.getAvatar() != null) {
			Image oldAvatar = user.getAvatar();
			user.setAvatar(null);
			imageRepository.delete(oldAvatar);
		}

		// Set new avatar
		user.setAvatar(image);
		image.setOwner(user);
		userRepository.save(user);
		return mapper.toDTO(user);
	}

	@Transactional
	public void removeProfileImage(Long id, User currentUser) {
		if (id != currentUser.getId() && !currentUser.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only remove your own profile image");
		}
		User user = findById(id);

		if (user.getAvatar() != null) {
			Image avatar = user.getAvatar();
			user.setAvatar(null);
			imageRepository.delete(avatar);
			userRepository.save(user);
		}
	}

	public Page<PostDTO> getLikedPosts(Long id, User currentUser, Pageable pageable) {
		if (id != currentUser.getId() && !currentUser.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only view your own liked posts");
		}
		User user = findById(id);
		return postRepository.findByUsersThatLikedContains(user, pageable)
				.map(post -> postMapper.toDTOWithLike(post, currentUser));
	}

	public Page<CommentDTO> getLikedComments(Long id, User currentUser, Pageable pageable) {
		if (id != currentUser.getId() && !currentUser.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only view your own liked comments");
		}
		User user = findById(id);
		return commentRepository.findByUsersThatLikedContains(user, pageable)
				.map(comment -> commentMapper.toDTOWithLike(comment, currentUser));
	}

	public Page<CommentDTO> getUserComments(Long id, User currentUser, Pageable pageable) {
		User user = findById(id);
		return commentRepository.findByAuthor(user, pageable)
				.map(comment -> commentMapper.toDTOWithLike(comment, currentUser));
	}

}