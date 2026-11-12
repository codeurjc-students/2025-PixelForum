package es.codeurjc.backend.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import es.codeurjc.backend.utils.TextRules;
import jakarta.persistence.EntityNotFoundException;

@Service
public class PostService {

	private static final String ADMIN = "ADMIN";

	private final PostMapper mapper;
	private final PostRepository postRepository;
	private final ImageRepository imageRepository;
	private final CommentRepository commentRepository;
	private final TopicRepository topicRepository;

	public PostService(PostMapper mapper, PostRepository postRepository, ImageRepository imageRepository,
			CommentRepository commentRepository, TopicRepository topicRepository) {
		this.mapper = mapper;
		this.postRepository = postRepository;
		this.imageRepository = imageRepository;
		this.commentRepository = commentRepository;
		this.topicRepository = topicRepository;
	}

	private Post findById(Long id) {
		return postRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Post not found"));
	}

	private Topic resolveTopic(Topic topic) {
		if (topic == null) {
			return null;
		}
		return topicRepository.findById(topic.getId())
				.orElseThrow(() -> new EntityNotFoundException("Topic not found"));
	}

	public PostDTO getPost(Long id, User user) {
		return toDTO(findById(id), user);
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

	@Transactional
	public PostDTO createPost(PostDTO postDTO, User user) {
		Post post = mapper.toDomain(postDTO);
		post.setTitle(TextRules.singleLine("The title", postDTO.title(), 300));
		post.setContent(TextRules.optionalMultiline("The post content", postDTO.content(), 5000, 50));
		post.setTopic(resolveTopic(postDTO.topic()));
		post.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
		post.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
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
		Post post = findById(id);

		if (post.getAuthor().getId() != user.getId() && !user.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only edit your own posts");
		}

		String title = TextRules.singleLine("The title", postDTO.title(), 300);
		String content = TextRules.optionalMultiline("The post content", postDTO.content(), 5000, 50);

		if (!hasChanges(post, postDTO, title, content)) {
			return toDTO(post, user);
		}

		manageImages(post, postDTO, user);
		post.setTitle(title);
		post.setContent(content);
		post.setTopic(resolveTopic(postDTO.topic()));
		post.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

		Post updatedPost = postRepository.save(post);
		return toDTO(updatedPost, user);
	}

	private boolean hasChanges(Post post, PostDTO dto, String title, String content) {
		if (!Objects.equals(post.getTitle(), title))
			return true;
		if (!Objects.equals(post.getContent(), content))
			return true;

		List<Long> currentImageIds = post.getImages().stream().map(Image::getId).toList();
		List<Long> newImagesIds = dto.images() != null ? dto.images() : List.of();
		if (!new HashSet<>(currentImageIds).equals(new HashSet<>(newImagesIds)))
			return true;

		return (!Objects.equals(
				post.getTopic() != null ? post.getTopic().getId() : null,
				dto.topic() != null ? dto.topic().getId() : null));
	}

	public void checkImagesOwnership(List<Image> images, User user, Post post) {
		for (Image img : images) {
			validateOwnership(img, post, user);
		}
	}

	private void validateOwnership(Image img, Post post, User user) {
		if (img.getOwner().getId() != user.getId() && !user.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only manage your own images");
		}
		if (img.getPost() != null && (post == null || img.getPost().getId() != post.getId())) {
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
		Post post = findById(id);
		if (post.getAuthor().getId() != user.getId() && !user.getRoles().contains(ADMIN)) {
			throw new AccessDeniedException("You can only delete your own posts");
		}
		postRepository.deleteLikesByPostIds(List.of(id));
		commentRepository.deleteLikesByCommentIds(post.getComments().stream().map(Comment::getId).toList());
		postRepository.delete(post);
	}

	@Transactional
	public PostDTO toggleLike(Long postId, User user) {
		Post post = findById(postId);

		boolean hasLiked = user.getLikedPosts().contains(post);
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