package es.codeurjc.backend.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import es.codeurjc.backend.dto.UserDTO;
import es.codeurjc.backend.dto.UserMapper;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.CommentRepository;
import es.codeurjc.backend.repository.PostRepository;
import es.codeurjc.backend.repository.UserRepository;
import jakarta.transaction.Transactional;

@Service
public class UserService {

	private final UserMapper mapper;
	private final UserRepository userRepository;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;

	public UserService(UserMapper mapper, UserRepository userRepository, PostRepository postRepository, CommentRepository commentRepository) {
		this.mapper = mapper;
		this.userRepository = userRepository;
		this.postRepository = postRepository;
		this.commentRepository = commentRepository;
	}
	
	public Optional<User> findById(long id) {
		return userRepository.findById(id);
	}

	public List<User> findByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	public Optional<User> findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	public boolean exist(long id) {
		return userRepository.existsById(id);
	}

	public List<User> findAll() {
		return userRepository.findAll();
	}

	public User save(User user){
		return userRepository.save(user);
	}	

    @Transactional 
    public void deleteById(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (user.getId()!= 1){
				postRepository.deleteByAuthor(user);
				commentRepository.deleteByAuthor(user);
				
				userRepository.delete(user);
			}
            
        }
    
    }
	private UserDTO toDTO (User user) {
        return mapper.toDTO(user);
    }

    private User toDomain (UserDTO userDTO) {
        return mapper.toDomain(userDTO);
    }

    private List<UserDTO> toDTOs(Collection<User> users){
        return mapper.toDTOs(users);
    }

	public Collection<UserDTO> getUsers() {
		return toDTOs(userRepository.findAll());
	}

	public UserDTO getUser(long id) {
		return toDTO(userRepository.findById(id).orElseThrow());
	}

	public UserDTO createUser(UserDTO userDTO) {
		User user = toDomain(userDTO);
 		this.save(user);
 		return toDTO(user);
	}

}