package es.codeurjc.backend.controller.rest;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.codeurjc.backend.dto.user.UserDTO;
import es.codeurjc.backend.dto.user.UserMapper;
import es.codeurjc.backend.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {

    private final UserService userService;
	private final UserMapper mapper;

	public UserRestController(UserService userService, UserMapper mapper) {
		this.userService = userService;
		this.mapper = mapper;
	}
    
    @GetMapping("/me")
	public UserDTO me() {
		return userService.getLoggedUserDTO();
	}

    @GetMapping("/")
	public List<UserDTO> getUsers() {
		return mapper.toDTOs(userService.findAll());
	}

    @PostMapping("/")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        UserDTO createdUserDTO = userService.createUser(userDTO);
        URI location = fromCurrentRequest().path("/{id}").buildAndExpand(createdUserDTO.id()).toUri();
        return ResponseEntity.created(location).body(createdUserDTO);
    }

    @GetMapping("/{id}")
	public UserDTO getUser(@PathVariable long id) {
        return userService.getUser(id);
	}

	@DeleteMapping("/{id}")
    public UserDTO deleteUser(@PathVariable long id) {
        UserDTO deletedUser = userService.getUser(id);
        userService.deleteById(id);
        return deletedUser;
    }
}