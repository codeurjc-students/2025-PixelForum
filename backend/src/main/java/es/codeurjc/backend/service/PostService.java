package es.codeurjc.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import es.codeurjc.backend.dto.post.PostDTO;
import es.codeurjc.backend.dto.post.PostMapper;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class PostService {

	private static final String POST_NOT_FOUND = "Post not found";

	private final PostMapper mapper;
	private final PostRepository postRepository;

	public PostService(PostMapper mapper, PostRepository postRepository) {
		this.mapper = mapper;
		this.postRepository = postRepository;
	}

	public PostDTO getPost(long id) {
		return toDTO(postRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(POST_NOT_FOUND)));
	}

	public Page<PostDTO> searchAndFilterPosts(String title, String authorUsername, String topic, Pageable pageable) {
		Page<Post> postsPage = postRepository.findByFilters(title, authorUsername, topic, pageable);
		return postsPage.map(mapper::toDTO);
	}

	public boolean exist(long id) {
		return postRepository.existsById(id);
	}

	public List<Post> findAll() {
		return postRepository.findAll();
	}

	public Post save(Post post) {
		return postRepository.save(post);
	}

	private PostDTO toDTO(Post post) {
		return mapper.toDTO(post);
	}

	public PostDTO createPost(PostDTO postDTO, User user) {
		Post post = mapper.toDomain(postDTO);
		post.setCreatedAt(LocalDateTime.now());
		post.setUpdatedAt(LocalDateTime.now());
		post.setLikes(0);
		post.setAuthor(user);

		Post savedPost = postRepository.save(post);
		return toDTO(savedPost);
	}

	@Transactional
	public PostDTO updatePost(Long id, PostDTO postDTO, User user) {
		Post post = postRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException(POST_NOT_FOUND));

		if (post.getAuthor().getId() != user.getId() && !user.getRoles().contains("ADMIN")) {
			throw new AccessDeniedException("You can only edit your own posts");
		}

		if (!hasChanges(post, postDTO)) {
			return toDTO(post);
		}

		post.setTitle(postDTO.title());
		post.setContent(postDTO.content());
		post.setImages(postDTO.images());
		post.setTopic(postDTO.topic());
		post.setUpdatedAt(LocalDateTime.now());

		Post updatedPost = postRepository.save(post);
		return toDTO(updatedPost);
	}

	private boolean hasChanges(Post post, PostDTO dto) {
		if (!Objects.equals(post.getTitle(), dto.title()))
			return true;
		if (!Objects.equals(post.getContent(), dto.content()))
			return true;

		List<String> currentImages = post.getImages() == null
				? Collections.emptyList()
				: new ArrayList<>(post.getImages());

		List<String> newImages = dto.images() == null
				? Collections.emptyList()
				: new ArrayList<>(dto.images());

		if (!currentImages.equals(newImages))
			return true;

		return (!Objects.equals(
				post.getTopic() != null ? post.getTopic().getId() : null,
				dto.topic() != null ? dto.topic().getId() : null));
	}

	@Transactional
	public void deletePost(Long id, User user) {
		Post post = postRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException(POST_NOT_FOUND));

		if (post.getAuthor().getId() != user.getId() && !user.getRoles().contains("ADMIN")) {
			throw new AccessDeniedException("You can only delete your own posts");
		}

		postRepository.delete(post);
	}
}