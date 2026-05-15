package es.codeurjc.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private LocalDateTime createdAt;
    private String bio;

    @OneToOne
    @JoinColumn(name = "profile_image_id")
    private Image avatar;

    @ManyToMany
    @JoinTable(name = "user_liked_posts", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "post_id"))
    private List<Post> likedPosts = new ArrayList<>();

    private List<Long> likedComments;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles;

    public User() {

    }

    public User(String username, String email, String password, LocalDateTime createdAt, String bio, String... roles) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.createdAt = createdAt;
        this.bio = bio;
        likedPosts = new ArrayList<>();
        likedComments = new ArrayList<>();
        this.roles = List.of(roles);
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getBio() {
        return bio;
    }

    public Image getAvatar() {
        return avatar;
    }

    public List<Post> getLikedPosts() {
        return likedPosts;
    }

    public List<Long> getLikedComments() {
        return likedComments;
    }

    public List<String> getRoles() {
        return roles;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setAvatar(Image avatar) {
        this.avatar = avatar;
    }

    public void setLikedPosts(List<Post> likedPosts) {
        this.likedPosts = likedPosts;
    }

    public void setLikedComments(List<Long> likedComments) {
        this.likedComments = likedComments;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

}