package es.codeurjc.backend.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import es.codeurjc.backend.dto.PostMapper;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.repository.PostRepository;
import es.codeurjc.backend.service.PostService;

@Tag("integration")
@DisplayName("PostService Integration tests")
@ActiveProfiles("test")
@SpringBootTest
class PostServiceIntegrationTest {
    @Autowired
    PostRepository postRepository;

    PostService postService;

    @BeforeEach
    void init() {
        PostMapper postMapper = Mappers.getMapper(PostMapper.class);
        postService = new PostService(postMapper, postRepository);
    }

    @Test
    void savePostIntTest(){
        int numPostsBefore = postService.findAll().size();

        Post post = new Post();
        post.setTitle("New Post Title");
        post.setContent("This is the content of the new post.");

        Post savedpost = postService.save(post);
        assertNotNull(savedpost);
        assertTrue(savedpost.getId() > 0);

        int numPostsAfter = postService.findAll().size();
        assertEquals(numPostsAfter, (numPostsBefore + 1));
        assertEquals(true, postService.exist(savedpost.getId()));
    }
}
