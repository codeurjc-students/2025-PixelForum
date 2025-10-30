package es.codeurjc.backend.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.codeurjc.backend.dto.PostDTO;
import es.codeurjc.backend.dto.PostMapper;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.repository.PostRepository;
import jakarta.transaction.Transactional;

@Service
public class PostService {

	@Autowired
    private PostMapper mapper;

    @Autowired
    private PostRepository postRepository;

	
	public Optional<Post> findById(long id) {
		return postRepository.findById(id);
	}

	public boolean exist(long id) {
		return postRepository.existsById(id);
	}

	public List<Post> findAll() {
		return postRepository.findAll();
	}

	public Post save(Post post){
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

	private PostDTO toDTO (Post post) {
        return mapper.toDTO(post);
    }

    private Post toDomain (PostDTO postDTO) {
        return mapper.toDomain(postDTO);
    }

    private List<PostDTO> toDTOs(Collection<Post> posts){
        return mapper.toDTOs(posts);
    }

	public Collection<PostDTO> getposts() {
		return toDTOs(postRepository.findAll());
	}

	public PostDTO getPost(long id) {
		return toDTO(postRepository.findById(id).orElseThrow());
	}

	public PostDTO createPost(PostDTO postDTO) {
		Post post = toDomain(postDTO);
 		this.save(post);
 		return toDTO(post);
	}

}