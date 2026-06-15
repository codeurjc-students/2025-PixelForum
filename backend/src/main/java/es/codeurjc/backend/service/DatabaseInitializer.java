package es.codeurjc.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import es.codeurjc.backend.model.Comment;
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
	private final CommentService commentService;
	private final ImageService imageService;

	public DatabaseInitializer(PostService postService, UserService userService, TopicService topicService,
			CommentService commentService, ImageService imageService) {
		this.postService = postService;
		this.userService = userService;
		this.topicService = topicService;
		this.commentService = commentService;
		this.imageService = imageService;
	}

	@PostConstruct
	public void init() throws IOException {

		User admin = new User("admin", "admin@host", "admin0", LocalDateTime.of(2023, Month.JANUARY, 23, 10, 43),
				" This is the oficial Admin account", "ADMIN", "USER");
		User user1 = new User("martin", "martin@gmail.com", "user", LocalDateTime.of(2023, Month.MARCH, 13, 18, 12),
				"Been gaming since the PS2 days. Love everything from story-driven RPGs to competitive multiplayer. Currently obsessed with souls-like games and indie titles. Competitive but chill. Down to team up or just chat about games.",
				"USER");
		User user2 = new User("robert", "robert@gmail.com", "user", LocalDateTime.of(2023, Month.MARCH, 20, 11, 03),
				"I love playing video games!", "USER");
		User user3 = new User("daniel", "daniel@gmail.com", "user", LocalDateTime.of(2023, Month.MAY, 9, 15, 01),
				"I'm suposed to add my bio here", "USER");
		User user4 = new User("alvaro", "alvaro@gmail.com", "user", LocalDateTime.of(2024, Month.FEBRUARY, 15, 23, 40),
				"If you are reading this, you are spying on me", "USER");
		userService.addUser(admin);
		userService.addUser(user1);
		userService.addUser(user2);
		userService.addUser(user3);
		userService.addUser(user4);

		addAvatar(user1, "/images/avatar.png");

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
				LocalDateTime.of(2026, Month.FEBRUARY, 20, 18, 43), new ArrayList<>(), user1, topics.get(0));
		postService.save(post1);
		addImages(post1, List.of("/images/gta_poster.jpeg", "/images/gta_map.webp"), user1);

		Post post2 = new Post("GTA VI official 2nd trailer",
				"Rockstar Games has released the official second trailer for Grand Theft Auto VI, showcasing new gameplay footage, story elements, and features of the highly anticipated game.",
				LocalDateTime.of(2026, Month.FEBRUARY, 18, 19, 15), null, user2, topics.get(0));
		postService.save(post2);

		Post post3 = new Post("GTA VI release date rumors",
				"Rumors are swirling about the potential release date for Grand Theft Auto VI, with speculation pointing towards a launch in late 2024 or early 2025. Fans are eagerly awaiting official confirmation from Rockstar Games.",
				LocalDateTime.of(2026, Month.JANUARY, 12, 15, 04), null, user1, topics.get(0));
		postService.save(post3);

		Post post4 = new Post("GTA VI map details",
				"New details about the map of Grand Theft Auto VI have emerged, suggesting a vast and diverse open world that includes multiple cities and rural areas, offering players a rich and immersive gaming experience.",
				LocalDateTime.of(2026, Month.FEBRUARY, 2, 8, 30), null, user1, topics.get(0));
		postService.save(post4);

		for (int i = 0; i < 20; i++) {
			Post post = new Post();
			post.setTitle("Example post " + i);
			post.setContent(
					"This is a test post to populate the database with sample data. It is only for testing purposes and does not contain any meaningful information.");
			post.setTopic(topics.get(2));
			post.setAuthor(user2);
			post.setCreatedAt(LocalDateTime.of(2025, Month.DECEMBER, 22, 22, 02 + i));
			post.setLikes(0);

			postService.save(post);
		}

		Comment comment1 = new Comment("I can't wait to play GTA VI!",
				LocalDateTime.of(2026, Month.FEBRUARY, 23, 10, 15), user2, post1);
		commentService.save(comment1);
		Comment comment2 = new Comment("Yeah, I'm excited too!", LocalDateTime.of(2026, Month.FEBRUARY, 23, 12, 30),
				user1, post1);
		commentService.save(comment2);
		Comment comment3 = new Comment("I hope the release date rumors are true!",
				LocalDateTime.of(2026, Month.FEBRUARY, 21, 19, 45), user3, post1);
		commentService.save(comment3);
		Comment comment4 = new Comment("The map details sound incredible!",
				LocalDateTime.of(2026, Month.FEBRUARY, 3, 14, 20), user2, post4);
		commentService.save(comment4);

		for (int i = 0; i < 20; i++) {
			Comment comment = new Comment("This is a test comment " + i,
					LocalDateTime.of(2026, Month.FEBRUARY, 22, 10, 00 + i), user2, post1);
			commentService.save(comment);
		}

		addCommentLikes(user1, List.of(comment1, comment3, comment4));
		addCommentLikes(user2, List.of(comment1, comment2, comment3, comment4));
		addCommentLikes(user3, List.of(comment1, comment2, comment3));
		addCommentLikes(user4, List.of(comment1, comment2, comment3, comment4));
		addCommentLikes(admin, List.of(comment1));

		addPostLikes(user1, List.of(post1, post2, post3, post4));
		addPostLikes(user2, List.of(post1, post2, post3, post4));
		addPostLikes(user3, List.of(post1, post2, post4));
		addPostLikes(user4, List.of(post1, post2, post3, post4));
		addPostLikes(admin, List.of(post1));
	}

	private Image uploadImage(String route, User user) throws IOException {
		InputStream is = getClass().getResourceAsStream(route);
		if (is == null) {
			throw new IOException("Image not found: " + route);
		}
		byte[] data = is.readAllBytes();

		return imageService.saveImage(data, route.substring(route.lastIndexOf("/") + 1),
				"image/" + route.substring(route.lastIndexOf(".") + 1), user);
	}

	private void addImages(Post post, List<String> imageRoutes, User user) throws IOException {
		for (String route : imageRoutes) {
			Image image = uploadImage(route, user);
			image.setPost(post);
			post.getImages().add(image);
		}
		postService.save(post);
	}

	private void addAvatar(User user, String route) throws IOException {
		user.setAvatar(uploadImage(route, user));
		userService.save(user);
	}

	private void addPostLikes(User user, List<Post> posts) {
		for (Post post : posts) {
			user.getLikedPosts().add(post);
			post.getUsersThatLiked().add(user);
			post.setLikes(post.getUsersThatLiked().size());
			postService.save(post);
		}
		userService.save(user);
	}

	private void addCommentLikes(User user, List<Comment> comments) {
		for (Comment comment : comments) {
			user.getLikedComments().add(comment);
			comment.getUsersThatLiked().add(user);
			comment.setLikes(comment.getUsersThatLiked().size());
			commentService.save(comment);
		}
	}

}