package es.codeurjc.backend.controller.rest;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.service.PostService;

@RestController
@RequestMapping("/api/posts")
public class PostRestController {

    @Autowired
    private PostService postService;

    // Obtener todos los posts
    @GetMapping("/")
    public List<Post> getAllPosts() {
        return postService.findAll();
    }

    // Obtener un post por ID
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable long id) {
        Optional<Post> post = postService.findById(id);
        return post.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

}