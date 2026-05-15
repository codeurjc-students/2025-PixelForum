package es.codeurjc.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import es.codeurjc.backend.dto.post.PostDTO;
import es.codeurjc.backend.dto.post.PostMapper;
import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.ImageRepository;
import es.codeurjc.backend.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class PostService {

	private static final String POST_NOT_FOUND = "Post not found";
	private static final String ADMIN = "ADMIN";

	private final PostMapper mapper;
	private final PostRepository postRepository;
	private final ImageRepository imageRepository;

	public PostService(PostMapper mapper, PostRepository postRepository, ImageRepository imageRepository) {
		this.mapper = mapper;
		this.postRepository = postRepository;
		this.imageRepository = imageRepository;
	}

	public PostDTO getPost(Long id, User user) {
		return toDTO(postRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(POST_NOT_FOUND)), user);
	}

	public Page<PostDTO> searchAndFilterPosts(String title, String authorUsername, String topic, Pageable pageable,
			User user) {
		Page<Post> postsPage = postRepository.findByFilters(title, authorUsername, topic, pageable);
		return postsPage.map(post -> toDTO(post, user));
	}

	public boolean exist(Long id) {
		return postRepository.existsById(id);
	}

	public List<Post> findAll() {
		return postRepository.findAll();
	}

	public Post save(Post post) {
		return postRepository.save(post);
	}

	private PostDTO toDTO(Post post, User user) {
		return mapper.toDTOWithLike(post, user);
	}

	public PostDTO createPost(PostDTO postDTO, User user) {
		Post post = mapper.toDomain(postDTO);
		post.setCreatedAt(LocalDateTime.now());
		post.setUpdatedAt(LocalDateTime.now());
		post.setLikes(0);
		post.setAuthor(user);

		if (postDTO.images() != null && !postDTO.images().isEmpty()) {
			List<Image> images = imageRepository.findAllById(postDTO.images());
			if (images.size() != postDTO.images().size()) {
				throw new IllegalArgumentException("Some images not found");
			}
			checkImagesOwnership(images, user, null);
			post.setImages(images);
			images.forEach(img -> img.setPost(post));
		}
		Post savedPost = postRepository.save(post);
		return toDTO(savedPost, user);
	}

	@Transactional
	public PostDTO updatePost(Long id, PostDTO postDTO, User user) {
		Post post = postRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException(POST_NOT_FOUND));

		if (post.getAuthor().getId() != user.getId() && !user.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only edit your own posts");
		}

		if (!hasChanges(post, postDTO)) {
			return toDTO(post, user);
		}

		manageImages(post, postDTO, user);
		post.setTitle(postDTO.title());
		post.setContent(postDTO.content());
		post.setTopic(postDTO.topic());
		post.setUpdatedAt(LocalDateTime.now());

		Post updatedPost = postRepository.save(post);
		return toDTO(updatedPost, user);
	}

	private boolean hasChanges(Post post, PostDTO dto) {
		if (!Objects.equals(post.getTitle(), dto.title()))
			return true;
		if (!Objects.equals(post.getContent(), dto.content()))
			return true;

		List<Long> currentImageIds = post.getImages().stream().map(Image::getId).toList();
		List<Long> newImagesIds = dto.images() != null ? dto.images() : List.of();
		if (!Objects.equals(currentImageIds, newImagesIds))
			return true;

		return (!Objects.equals(
				post.getTopic() != null ? post.getTopic().getId() : null,
				dto.topic() != null ? dto.topic().getId() : null));
	}

	public void checkImagesOwnership(List<Image> images, User user, Post post) {
		for (Image img : images) {
			if (post == null) {
				validateOwnership(img, user);
			} else {
				validateOwnership(img, post, user);
			}
		}
	}

	private void validateOwnership(Image img, User user) {
		if (img.getOwner().getId() != user.getId() && !user.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only add your own images to the post");
		}
		if (img.getPost() != null) {
			throw new IllegalArgumentException(
					"Image with id " + img.getId() + " is already associated with another post");
		}
	}

	private void validateOwnership(Image img, Post post, User user) {
		if (img.getOwner().getId() != user.getId() && user.getId() != post.getAuthor().getId()
				&& !user.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only add your own images to the post");
		}
		if (img.getPost() != null && img.getPost().getId() != post.getId()) {
			throw new IllegalArgumentException(
					"Image with id " + img.getId() + " is already associated with another post");
		}
	}

	public void manageImages(Post post, PostDTO postDTO, User user) {
		List<Long> newImageIds = postDTO.images() != null ? postDTO.images() : List.of();
		List<Long> currentImageIds = post.getImages().stream().map(Image::getId).toList();

		List<Long> imagesToDelete = currentImageIds.stream()
				.filter(newId -> !newImageIds.contains(newId))
				.toList();
		List<Long> imagesToAdd = newImageIds.stream()
				.filter(newId -> !currentImageIds.contains(newId))
				.toList();

		List<Image> removeImages = imageRepository.findAllById(imagesToDelete);
		List<Image> newImages = imageRepository.findAllById(imagesToAdd);

		if (newImages.size() != imagesToAdd.size()) {
			throw new IllegalArgumentException("Some images not found");
		}
		checkImagesOwnership(removeImages, user, post);
		checkImagesOwnership(newImages, user, post);

		if (!imagesToDelete.isEmpty()) {
			post.getImages().removeAll(removeImages);
			imageRepository.deleteAllById(imagesToDelete);
		}

		if (!imagesToAdd.isEmpty()) {
			post.getImages().addAll(newImages);
			for (Image img : newImages) {
				img.setPost(post);
				img.setOwner(post.getAuthor());
			}
		}
	}

	@Transactional
	public void deletePost(Long id, User user) {
		Post post = postRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException(POST_NOT_FOUND));
		if (post.getAuthor().getId() != user.getId() && !user.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only delete your own posts");
		}
		for (User userLikes : new ArrayList<>(post.getUsersThatLiked())) {
			userLikes.getLikedPosts().remove(post);
		}
		postRepository.delete(post);
	}

	@Transactional
	public PostDTO toggleLike(Long postId, User user) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new EntityNotFoundException(POST_NOT_FOUND));

		List<Post> likedPosts = user.getLikedPosts();
		boolean hasLiked = likedPosts.contains(post);
		if (hasLiked) {
			user.getLikedPosts().remove(post);
			post.getUsersThatLiked().remove(user);
		} else {
			user.getLikedPosts().add(post);
			post.getUsersThatLiked().add(user);
		}
		post.setLikes(post.getUsersThatLiked().size());
		return toDTO(post, user);
	}
}