package es.codeurjc.backend.controller.rest;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

import java.net.URI;
import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import es.codeurjc.backend.dto.user.ChangePasswordDTO;
import es.codeurjc.backend.dto.user.CreateUserDTO;
import es.codeurjc.backend.dto.user.UserDTO;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {

    private static final String USER_NOT_FOUND = "User not found";

    private final UserService userService;

    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Page<UserDTO>> getUsers(@PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ResponseEntity.ok(userService.getUsers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
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
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
        return ResponseEntity.ok(userService.updateUser(id, userDTO, currentUser));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable Long id, @RequestBody ChangePasswordDTO changePasswordDTO,
            Principal principal) {
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
        userService.changePassword(id, changePasswordDTO, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Principal principal) {
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
        userService.deleteUser(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<UserDTO> setProfileImage(@PathVariable Long id, @RequestParam("imageId") Long imageId,
            Principal principal) {
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        UserDTO updatedUserDTO = userService.setProfileImage(id, imageId, currentUser);
        if (updatedUserDTO == null) {
            throw new IllegalArgumentException(
                    "Profile pictures must be JPG or PNG format. WebP and other formats are not allowed.");
        }
        return ResponseEntity.ok(updatedUserDTO);
    }

    @DeleteMapping("/{id}/avatar")
    public ResponseEntity<Void> removeProfileImage(@PathVariable Long id, Principal principal) {
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        userService.removeProfileImage(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}