package es.codeurjc.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

		addAvatar(user1, "/images/avatar1.png");
		addAvatar(user2, "/images/avatar2.png");
		addAvatar(user3, "/images/avatar3.jpg");

		Map<String, Topic> topicsByName = loadTopics();

		List<User> userList = List.of(user1, user2, user3, user4, admin);
		Map<String, User> users = Map.of("martin", user1, "robert", user2, "daniel", user3, "alvaro", user4);

		List<Comment> comments = new ArrayList<>();
		List<Post> posts = loadSeedPosts(users, topicsByName, comments);

		Post leak = posts.get(posts.size() - 1);

		for (int u = 0; u < userList.size(); u++) {
			Set<Post> liked = new LinkedHashSet<>(pick(posts, u, u + 2));
			liked.add(leak);

			addCommentLikes(userList.get(u), pick(comments, u, u + 3));
			addPostLikes(userList.get(u), new ArrayList<>(liked));
		}
	}

	private Map<String, Topic> loadTopics() throws IOException {
		List<Topic> topics;
		try (InputStream is = getClass().getResourceAsStream("/seed-topics.json")) {
			topics = new ObjectMapper().readValue(is, new TypeReference<>() {
			});
		}

		Map<String, Topic> byName = new LinkedHashMap<>();
		topics.forEach(t -> {
			topicService.save(t);
			byName.put(t.getName(), t);
		});
		return byName;
	}

	private record SeedComment(String author, String text) {
	}

	private record SeedPost(String topic, String title, String content, String author, List<String> images,
			List<SeedComment> comments) {
	}

	private List<Post> loadSeedPosts(Map<String, User> users, Map<String, Topic> topics, List<Comment> allComments)
			throws IOException {

		List<SeedPost> seed;
		try (InputStream is = getClass().getResourceAsStream("/seed-data.json")) {
			seed = new ObjectMapper().readValue(is, new TypeReference<>() {
			});
		}

		LocalDateTime start = LocalDateTime.now(ZoneOffset.UTC).minusDays(seed.size());
		List<Post> posts = new ArrayList<>();

		for (int i = 0; i < seed.size(); i++) {
			SeedPost p = seed.get(i);
			LocalDateTime created = start.plusDays(i);
			User author = users.get(p.author());

			Post post = new Post(p.title(), p.content(), created, new ArrayList<>(), author, topics.get(p.topic()));
			postService.save(post);
			if (p.images() != null && !p.images().isEmpty()) {
				addImages(post, p.images(), author);
			}

			int h = 1;
			for (SeedComment c : p.comments()) {
				Comment comment = new Comment(c.text(), created.plusMinutes(20L * h++), users.get(c.author()), post);
				commentService.save(comment);
				allComments.add(comment);
			}
			posts.add(post);
		}
		return posts;
	}

	private <T> List<T> pick(List<T> items, int offset, int step) {
		List<T> result = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			if ((i + offset) % step == 0) {
				result.add(items.get(i));
			}
		}
		return result;
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