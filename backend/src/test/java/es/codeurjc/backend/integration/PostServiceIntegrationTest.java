package es.codeurjc.backend.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import es.codeurjc.backend.dto.post.PostDTO;
import es.codeurjc.backend.dto.post.PostMapper;
import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.Topic;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.ImageRepository;
import es.codeurjc.backend.service.PostService;
import es.codeurjc.backend.service.TopicService;
import es.codeurjc.backend.service.UserService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import org.springframework.security.access.AccessDeniedException;

@Tag("integration")
@DisplayName("PostService Integration Tests")
@ActiveProfiles("test")
@SpringBootTest
class PostServiceIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private PostMapper postMapper;

    private Image createImage(User user) {
        Image img = new Image();
        img.setOwner(user);
        img.setPost(null);
        img.setFilename("test.png");
        img.setContentType("image/png");
        return imageRepository.save(img);
    }

    @Test
    @DisplayName("Should create a post successfully")
    void createPostTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        Topic topic = topicService.findAll().get(0);

        Post post = new Post();
        post.setTitle("Integration Test Post");
        post.setContent("Testing content");
        post.setTopic(topic);

        PostDTO postDTO = postMapper.toDTO(post);

        // WHEN
        PostDTO created = postService.createPost(postDTO, user);

        // THEN
        assertNotNull(created.id(), "Created post should have an ID");
        assertEquals(postDTO.title(), created.title(), "Title should match");
        assertEquals(postDTO.content(), created.content(), "Content should match");
        assertEquals(user.getId(), created.author().id(), "Author should match");
    }

    @Test
    @Transactional
    @DisplayName("Should create post with images")
    void createPostWithImagesTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        Topic topic = topicService.findAll().get(0);

        Image img = createImage(user);

        PostDTO dto = new PostDTO(
                null,
                "Post with images",
                "Content",
                null,
                null,
                null,
                topic,
                null,
                null,
                List.of(img.getId()));

        // WHEN
        PostDTO created = postService.createPost(dto, user);

        // THEN
        assertNotNull(created.id());
        assertEquals(1, created.images().size());
    }

    @Test
    @DisplayName("Should fail when image does not exist")
    void createPostImageNotFoundTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        Topic topic = topicService.findAll().get(0);

        PostDTO dto = new PostDTO(
                null,
                "Invalid",
                "Content",
                null,
                null,
                null,
                topic,
                null,
                null,
                List.of(99999L));

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> {
            postService.createPost(dto, user);
        });
    }

    @Test
    @Transactional
    @DisplayName("Should fail when image belongs to another user")
    void createPostWrongOwnerTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        User other = userService.findByUsername("robert").orElseThrow();
        Topic topic = topicService.findAll().get(0);

        Image img = createImage(other);

        PostDTO dto = new PostDTO(
                null,
                "Invalid ownership",
                "Content",
                null,
                null,
                null,
                topic,
                null,
                null,
                List.of(img.getId()));

        // WHEN & THEN
        assertThrows(AccessDeniedException.class, () -> {
            postService.createPost(dto, user);
        });
    }

    @Test
    @DisplayName("Should get a post by ID")
    @Transactional
    void getPostTest() {
        // GIVEN
        Post post = postService.findAll().get(0);

        // WHEN
        PostDTO dto = postService.getPost(post.getId(), null);

        // THEN
        assertEquals(post.getTitle(), dto.title());
        assertEquals(post.getContent(), dto.content());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when getting non-existing post")
    void getPostNotFoundTest() {
        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> postService.getPost(-1L, null));
    }

    @Test
    @DisplayName("Should update a post when author is the user")
    @Transactional
    void updatePostSuccessTest() {
        // GIVEN
        Post post = postService.findAll().get(0);
        User author = post.getAuthor();

        PostDTO updateDTO = new PostDTO(
                null,
                post.getTitle() + " Updated",
                post.getContent() + " Updated",
                null, null,
                null,
                post.getTopic(),
                null,
                null,
                null);

        // WHEN
        PostDTO updated = postService.updatePost(post.getId(), updateDTO, author);

        // THEN
        assertEquals(updateDTO.title(), updated.title());
        assertEquals(updateDTO.content(), updated.content());
        assertEquals(updateDTO.topic(), updated.topic());
        assertTrue(updated.updatedAt().isAfter(post.getCreatedAt()), "UpdatedAt should be later than CreatedAt");
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when updating post as non-author")
    void updatePostAccessDeniedTest() {
        // GIVEN
        Post post = postService.findAll().get(0);
        User nonAuthor = userService.findByUsername("robert").orElseThrow();

        PostDTO dto = new PostDTO(
                null,
                "Invalid Update",
                "Invalid content",
                null, null,
                null,
                post.getTopic(),
                null,
                null,
                null);

        long postId = post.getId();
        // WHEN & THEN
        assertThrows(AccessDeniedException.class, () -> postService.updatePost(postId, dto, nonAuthor));
    }

    @Test
    @DisplayName("Should update post when user is ADMIN even if not author")
    @Transactional
    void updatePostAsAdminTest() {
        // GIVEN
        Post post = postService.findAll().get(0);
        User admin = userService.findByUsername("admin").orElseThrow();

        PostDTO dto = new PostDTO(
                null,
                "Admin Update",
                "Admin content",
                null, null,
                null,
                post.getTopic(),
                null,
                null,
                null);

        // WHEN
        PostDTO updated = postService.updatePost(post.getId(), dto, admin);

        // THEN
        assertEquals("Admin Update", updated.title());
        assertEquals("Admin content", updated.content());
        assertEquals(dto.topic(), updated.topic());
    }

    @Test
    @Transactional
    @DisplayName("Should add images when updating post")
    void updatePostAddImagesTest() {
        // GIVEN
        Post post = postService.findAll().get(0);
        User user = post.getAuthor();

        Image img = createImage(user);

        PostDTO dto = new PostDTO(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                null,
                null,
                null,
                post.getTopic(),
                null,
                null,
                List.of(img.getId()));

        // WHEN
        PostDTO updated = postService.updatePost(post.getId(), dto, user);

        // THEN
        assertEquals(1, updated.images().size());
    }

    @Test
    @Transactional
    @DisplayName("Should remove images when updating post")
    void updatePostRemoveImagesTest() {
        // GIVEN
        Post post = postService.findAll().get(0);
        User user = post.getAuthor();

        Image img = createImage(user);

        // Add image first
        PostDTO withImage = new PostDTO(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                null,
                null,
                null,
                post.getTopic(),
                null,
                null,
                List.of(img.getId()));

        postService.updatePost(post.getId(), withImage, user);

        // Remove image
        PostDTO withoutImages = new PostDTO(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                null,
                null,
                null,
                post.getTopic(),
                null,
                null,
                List.of());

        // WHEN
        PostDTO updated = postService.updatePost(post.getId(), withoutImages, user);

        // THEN
        assertTrue(updated.images().isEmpty());
    }

    @Test
    @DisplayName("Should delete post when author deletes it")
    void deletePostSuccessTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        Topic topic = topicService.findAll().get(0);

        Post post = new Post();
        post.setTitle("To Delete");
        post.setContent("Content");
        post.setTopic(topic);

        PostDTO postDTO = postMapper.toDTO(post);
        PostDTO created = postService.createPost(postDTO, user);

        // WHEN
        postService.deletePost(created.id(), user);

        // THEN
        assertFalse(postService.exist(created.id()), "Post should be deleted");
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when non-author deletes post")
    void deletePostAccessDeniedTest() {
        // GIVEN
        Post post = postService.findAll().get(0);
        User nonAuthor = userService.findByUsername("robert").orElseThrow();

        // WHEN & THEN
        long postId = post.getId();
        assertThrows(AccessDeniedException.class, () -> postService.deletePost(postId, nonAuthor));
    }

    @Test
    @DisplayName("Should delete post when ADMIN deletes it")
    void deletePostAsAdminTest() {
        // GIVEN
        User admin = userService.findByUsername("admin").orElseThrow();
        User author = userService.findByUsername("martin").orElseThrow();
        Topic topic = topicService.findAll().get(0);

        Post post = new Post();
        post.setTitle("Admin Delete");
        post.setContent("Content");
        post.setTopic(topic);

        PostDTO postDTO = postMapper.toDTO(post);
        PostDTO created = postService.createPost(postDTO, author);

        // WHEN
        postService.deletePost(created.id(), admin);

        // THEN
        assertFalse(postService.exist(created.id()), "Post should be deleted by admin");
    }

    @Test
    @DisplayName("Should filter posts by title and topic")
    @Transactional
    void searchAndFilterPostsTest() {
        // GIVEN
        Pageable pageable = PageRequest.of(0, 10);

        // WHEN
        var page = postService.searchAndFilterPosts("GTA VI", null, "GTA VI", pageable, null);

        // THEN
        assertTrue(page.stream().allMatch(p -> p.title().contains("GTA VI")));
    }

    @Test
    @DisplayName("Exist should return true for existing post and false for non-existing")
    void existTest() {
        // GIVEN
        Post post = postService.findAll().get(0);

        // THEN
        assertTrue(postService.exist(post.getId()));
        assertFalse(postService.exist(-1L));
    }

    @Test
    @DisplayName("FindAll should return all posts")
    void findAllTest() {
        List<Post> posts = postService.findAll();
        assertFalse(posts.isEmpty(), "There should be posts in the database");
    }
}