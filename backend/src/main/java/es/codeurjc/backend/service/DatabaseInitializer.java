package es.codeurjc.backend.service;

import java.util.List;

import org.springframework.stereotype.Component;

import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.Topic;
import es.codeurjc.backend.model.User;
import jakarta.annotation.PostConstruct;


@Component
public class DatabaseInitializer {

    private final PostService postService;
	private final UserService userService;
	private final TopicService topicService;

	public DatabaseInitializer(PostService postService, UserService userService, TopicService topicService) {
		this.postService = postService;
		this.userService = userService;
		this.topicService = topicService;
	}

    @PostConstruct
    public void init() {
        
        User admin = new User("admin", "admin@host", "admin0", "ADMIN", "USER");
        User usuario1 = new User("martin", "martin@gmail.com", "user", "USER");
        User usuario2 = new User("robert", "robert@gmail.com", "user", "USER");
        User usuario3 = new User("daniel", "daniel@gmail.com", "user", "USER");
        User usuario4 = new User("alvaro", "alvaro@gmail.com", "user", "USER");

        userService.save(admin);
        userService.save(usuario1);
        userService.save(usuario2);
        userService.save(usuario3);
        userService.save(usuario4);

        List<Topic> topics = List.of(
            new Topic("GTA VI", "All news about Grand Theft Auto VI"),
            new Topic("Cyberpunk", "All news about Cyberpunk"),
            new Topic("The Witcher", "All news about The Witcher")
        );

        topics.forEach(topicService::save);

        Post post1 = new Post("GTA VI Massive leak", "A massive leak has revealed extensive details about Grand Theft Auto VI, including character information, gameplay mechanics, and storyline elements. The leak has sparked significant discussion among fans eagerly anticipating the game's release.", 
            topics.get(0));
        postService.save(post1);

        Post post2 = new Post("GTA VI official 2nd trailer", "Rockstar Games has released the official second trailer for Grand Theft Auto VI, showcasing new gameplay footage, story elements, and features of the highly anticipated game.", 
            topics.get(0));
        postService.save(post2);

        Post post3 = new Post("GTA VI release date rumors", "Rumors are swirling about the potential release date for Grand Theft Auto VI, with speculation pointing towards a launch in late 2024 or early 2025. Fans are eagerly awaiting official confirmation from Rockstar Games.", 
            topics.get(0));
        postService.save(post3);

        Post post4 = new Post("GTA VI map details", "New details about the map of Grand Theft Auto VI have emerged, suggesting a vast and diverse open world that includes multiple cities and rural areas, offering players a rich and immersive gaming experience.", 
            topics.get(0));
        postService.save(post4);
    
    }   

}