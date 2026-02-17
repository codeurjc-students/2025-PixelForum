package es.codeurjc.backend.controller.rest;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.codeurjc.backend.dto.Post.PostDTO;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.service.FileUploadService;
import es.codeurjc.backend.service.PostService;
import es.codeurjc.backend.service.UserService;

@RestController
@RequestMapping("/api/v1/posts")
public class PostRestController {

    private final PostService postService;
    private final UserService userService;
    private final FileUploadService fileUploadService;

    public PostRestController(PostService postService, UserService userService, FileUploadService fileUploadService) {
        this.postService = postService;
        this.userService = userService;
        this.fileUploadService = fileUploadService;
    }

    // Get all posts
    @GetMapping("/")
    public List<Post> getAllPosts() {
        return postService.findAll();
    }

    // Get post by ID
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable long id) {
        Optional<Post> post = postService.findById(id);
        return post.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/")
    public ResponseEntity<PostDTO> createPost(@RequestBody PostDTO postDTO, Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName()).orElseThrow(() -> new IllegalArgumentException("User not found"));
            PostDTO createdPostDTO = postService.createPost(postDTO, user);

            URI location = fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(createdPostDTO.id())
                    .toUri();

            return ResponseEntity.created(location).body(createdPostDTO);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadImages(@RequestParam("files") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().build();
            }

            List<String> urls = new ArrayList<>();

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        String url = fileUploadService.uploadFile(file);
                        urls.add(url);
                    } catch (IllegalArgumentException e) {
                        // Log and continue with next file
                        System.err.println("Error uploading file: " + e.getMessage());
                        continue;
                    }
                }
            }

            if (urls.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok(new UploadResponse(urls));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) {
        try {
            java.nio.file.Path filepath = java.nio.file.Paths.get("uploads", filename);
            
            if (!java.nio.file.Files.exists(filepath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = java.nio.file.Files.readAllBytes(filepath);
            String contentType = getContentType(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    public record UploadResponse(List<String> urls) {
    }

}