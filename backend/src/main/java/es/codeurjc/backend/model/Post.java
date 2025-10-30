package es.codeurjc.backend.model;

import java.sql.Blob;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

@Entity
public class Post {

    @Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Lob
    private List<Blob> images;

    @ManyToOne
	@JoinColumn(name = "user_id")
	private User author;

    @ManyToMany
	@JoinTable(
		name = "post_topics",
		joinColumns = @JoinColumn(name = "post_id"),
		inverseJoinColumns = @JoinColumn(name = "topic_id")
	)
	private List<Topic> topics;

    private int likes;
    private List<Long> usersThatLiked;

    public Post() {
    }
    public Post(String title, String content, List<Topic> topics) {
        this.title = title;
        this.content = content;
        this.topics = topics;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public List<Blob> getImages() {
        return images;
    }
    public void setImages(List<Blob> images) {
        this.images = images;
    }
    public User getAuthor() {
        return author;
    }
    public void setAuthor(User author) {
        this.author = author;
    }
    public List<Topic> getTopics() {
        return topics;
    }
    public void setTopics(List<Topic> topics) {
        this.topics = topics;
    }
    public int getLikes() {
        return likes;
    }
    public void setLikes(int likes) {
        this.likes = likes;
    }
    public List<Long> getUsersThatLiked() {
        return usersThatLiked;
    }
    public void setUsersThatLiked(List<Long> usersThatLiked) {
        this.usersThatLiked = usersThatLiked;
    }

}