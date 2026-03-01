package es.codeurjc.backend.controller.rest;

import java.net.URI;
import java.security.Principal;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.codeurjc.backend.dto.Post.PostDTO;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.service.PostService;
import es.codeurjc.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/v1/posts")
public class PostRestController {

    private final PostService postService;
    private final UserService userService;

    public PostRestController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Page<PostDTO>> getAllPosts(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String authorUsername,
            @RequestParam(required = false) String topic,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostDTO> page = postService.searchAndFilterPosts(title, authorUsername, topic, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @PostMapping
    public ResponseEntity<PostDTO> createPost(@RequestBody PostDTO postDTO, Principal principal) {
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        PostDTO createdPostDTO = postService.createPost(postDTO, currentUser);

        URI location = fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdPostDTO.id())
                .toUri();

        return ResponseEntity.created(location).body(createdPostDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostDTO> updatePost(@PathVariable long id, @RequestBody PostDTO postDTO,
            Principal principal) {
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        PostDTO updatedPostDTO = postService.updatePost(id, postDTO, currentUser);
        return ResponseEntity.ok(updatedPostDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable long id, Principal principal) {
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        postService.deletePost(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}