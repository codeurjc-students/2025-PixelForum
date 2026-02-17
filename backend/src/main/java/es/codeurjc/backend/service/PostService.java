package es.codeurjc.backend.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import es.codeurjc.backend.dto.Post.PostDTO;
import es.codeurjc.backend.dto.Post.PostMapper;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.PostRepository;
import jakarta.transaction.Transactional;

@Service
public class PostService {

	private final PostMapper mapper;
	private final PostRepository postRepository;

	public PostService(PostMapper mapper, PostRepository postRepository) {
		this.mapper = mapper;
		this.postRepository = postRepository;
	}

	public Optional<Post> findById(long id) {
		return postRepository.findById(id);
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

	@Transactional
	public void deleteById(Long id) {
		Optional<Post> postOptional = postRepository.findById(id);
		if (postOptional.isPresent()) {
			Post post = postOptional.get();
			postRepository.delete(post);
		}
	}

	private PostDTO toDTO(Post post) {
		return mapper.toDTO(post);
	}

	private List<PostDTO> toDTOs(Collection<Post> posts) {
		return mapper.toDTOs(posts);
	}

	public Collection<PostDTO> getPosts() {
		return toDTOs(postRepository.findAll());
	}

	public PostDTO getPost(long id) {
		return toDTO(postRepository.findById(id).orElseThrow());
	}

	public PostDTO createPost(PostDTO postDTO, User user) {
		Post post = new Post();
		post.setTitle(postDTO.title());
		post.setContent(postDTO.content());
		post.setImages(postDTO.images());
		post.setCreatedAt(LocalDateTime.now());
		post.setUpdatedAt(LocalDateTime.now());
		post.setLikes(0);
		post.setAuthor(user);
		post.setTopic(postDTO.topic());

		Post savedPost = postRepository.save(post);
		// Return DTO
		return toDTO(savedPost);
	}

}