package es.codeurjc.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.Topic;
import es.codeurjc.backend.model.User;
import jakarta.annotation.PostConstruct;

@Component
public class DatabaseInitializer {

	private final PostService postService;
	private final UserService userService;
	private final TopicService topicService;
	private final ImageService imageService;

	public DatabaseInitializer(PostService postService, UserService userService, TopicService topicService,
			ImageService imageService) {
		this.postService = postService;
		this.userService = userService;
		this.topicService = topicService;
		this.imageService = imageService;
	}

	@PostConstruct
	public void init() throws IOException {

		User admin = new User("admin", "admin@host", "admin0", LocalDateTime.of(2023, 1, 23, 10, 43),
				" This is the oficial Admin account", "ADMIN", "USER");
		User user1 = new User("martin", "martin@gmail.com", "user", LocalDateTime.of(2023, 3, 13, 18, 12),
				"Been gaming since the PS2 days. Love everything from story-driven RPGs to competitive multiplayer. Currently obsessed with souls-like games and indie titles. Competitive but chill. Down to team up or just chat about games.",
				"USER");
		User user2 = new User("robert", "robert@gmail.com", "user", LocalDateTime.of(2023, 3, 20, 11, 03),
				"I love playing video games!", "USER");
		User user3 = new User("daniel", "daniel@gmail.com", "user", LocalDateTime.of(2023, 5, 9, 15, 01),
				"I'm suposed to add my bio here", "USER");
		User user4 = new User("alvaro", "alvaro@gmail.com", "user", LocalDateTime.of(2024, 2, 15, 23, 40),
				"If you are reading this, you are spying on me", "USER");
		userService.save(admin);
		userService.save(user1);
		userService.save(user2);
		userService.save(user3);
		userService.save(user4);

		List<Topic> topics = List.of(
				new Topic("GTA VI", "All news about Grand Theft Auto VI"),
				new Topic("Cyberpunk", "All news about Cyberpunk"),
				new Topic("The Witcher", "All news about The Witcher"),
				new Topic("Stardew Valley", "All news about Stardew Valley"),
				new Topic("Halo", "All news about Halo"),
				new Topic("Call of duty", "All news about Call of duty"),
				new Topic("Minecraft", "All news about Minecraft"),
				new Topic("Cuphead", "All news about Cuphead"),
				new Topic("This is a very long topic only for testing", "test"));

		topics.forEach(topicService::save);

		Post post1 = new Post("GTA VI Massive leak",
				"A massive leak has revealed extensive details about Grand Theft Auto VI, including character information, gameplay mechanics, and storyline elements. The leak has sparked significant discussion among fans eagerly anticipating the game's release.",
				LocalDateTime.of(2026, 2, 20, 18, 43), new ArrayList<>(), user1, topics.get(0));
		postService.save(post1);
		addImages(post1, List.of("/images/gta_poster.jpeg", "/images/gta_map.webp"), user1);

		Post post2 = new Post("GTA VI official 2nd trailer",
				"Rockstar Games has released the official second trailer for Grand Theft Auto VI, showcasing new gameplay footage, story elements, and features of the highly anticipated game.",
				LocalDateTime.of(2026, 2, 18, 19, 15), null, user2, topics.get(0));
		postService.save(post2);

		Post post3 = new Post("GTA VI release date rumors",
				"Rumors are swirling about the potential release date for Grand Theft Auto VI, with speculation pointing towards a launch in late 2024 or early 2025. Fans are eagerly awaiting official confirmation from Rockstar Games.",
				LocalDateTime.of(2026, 1, 12, 15, 04), null, user1, topics.get(0));
		postService.save(post3);

		Post post4 = new Post("GTA VI map details",
				"New details about the map of Grand Theft Auto VI have emerged, suggesting a vast and diverse open world that includes multiple cities and rural areas, offering players a rich and immersive gaming experience.",
				LocalDateTime.of(2026, 2, 2, 8, 30), null, user1, topics.get(0));
		postService.save(post4);

		for (int i = 0; i < 20; i++) {
			Post post = new Post();
			post.setTitle("Example post " + i);
			post.setContent(
					"This is a test post to populate the database with sample data. It is only for testing purposes and does not contain any meaningful information.");
			post.setTopic(topics.get(2));
			post.setAuthor(user2);
			post.setCreatedAt(LocalDateTime.of(2025, 12, 22, 22, 02 + i));
			post.setLikes(0);

			postService.save(post);
		}

		addLikes(user1, List.of(post1, post3, post4));
		addLikes(user2, List.of(post1, post2, post3, post4));
		addLikes(user3, List.of(post1, post2, post4));
		addLikes(user4, List.of(post1, post2, post3, post4));
		addLikes(admin, List.of(post1));
	}

	private void addImages(Post post, List<String> imageRoutes, User user) throws IOException {
		for (String route : imageRoutes) {
			InputStream is = getClass().getResourceAsStream(route);
			byte[] data = is.readAllBytes();
			Image image = imageService.saveImage(data, route.substring(route.lastIndexOf("/") + 1),
					"image/" + route.substring(route.lastIndexOf(".") + 1), user);
			image.setPost(post);
			post.getImages().add(image);
		}
		postService.save(post);
	}

	private void addLikes(User user, List<Post> posts) {
		for (Post post : posts) {
			user.getLikedPosts().add(post);
			post.getUsersThatLiked().add(user);
			post.setLikes(post.getUsersThatLiked().size());
			postService.save(post);
		}
		userService.update(user);
	}

}