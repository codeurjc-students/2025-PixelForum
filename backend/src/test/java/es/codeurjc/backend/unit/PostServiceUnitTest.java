package es.codeurjc.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import es.codeurjc.backend.dto.PostDTO;
import es.codeurjc.backend.dto.PostMapper;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.Topic;
import es.codeurjc.backend.repository.PostRepository;
import es.codeurjc.backend.service.PostService;

@Tag("unit")
@DisplayName("PostService Unitary tests")
public class PostServiceUnitTest {

    private PostService postService;
    private PostRepository postRepository;
    private PostMapper postMapper;

    @BeforeEach
    public void init() {
        this.postRepository = mock(PostRepository.class);
        this.postMapper = Mappers.getMapper(PostMapper.class);
        this.postService = new PostService(postRepository, postMapper);
    }

    @Test
    public void findPostByIdTest() {
        //GIVEN
        long id = 1;
        Topic topic = new Topic("GTA VI", "All news about Grand Theft Auto VI");
        Post post = new Post("GTA VI Massive Leak", "A massive leak has revealed extensive details about Grand Theft Auto VI, including character information, gameplay mechanics, and storyline elements. The leak has sparked significant discussion among fans eagerly anticipating the game's release.", 
            List.of(topic));
        post.setId(id);
        Optional<Post> optionalPost = Optional.of(post);
        //WHEN
        when(postRepository.findById(id)).thenReturn(optionalPost);
        PostDTO result = postService.getPost(id);
        PostDTO expected = postMapper.toDTO(post);
        //THEN
        assertEquals(result, expected);
    }
}
