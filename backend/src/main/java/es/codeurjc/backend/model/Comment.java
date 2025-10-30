package es.codeurjc.backend.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Comment {

    @Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
	@JoinColumn(name = "user_id")
	private User author;

    @ManyToOne
	@JoinColumn(name = "post_id")
	private Post post;

    @ManyToOne
	@JoinColumn(name = "comment_id")
	private Comment commentId;
    private int likes;
    private List<Long> usersThatLiked;

    
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
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
    public User getAuthor() {
        return author;
    }
    public void setAuthor(User author) {
        this.author = author;
    }
    public Post getPost() {
        return post;
    }
    public void setPost(Post post) {
        this.post = post;
    }
    public Comment getCommentId() {
        return commentId;
    }
    public void setCommentId(Comment commentId) {
        this.commentId = commentId;
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