package es.codeurjc.backend.model;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

    @Column (unique = true)
    private String username;

    @Column (unique = true)
    private String email;
    private String password;

    @Lob
    private Blob avatar;

    private List<Long> likedPosts;
    private List<Long> likedComments;

    @ElementCollection(fetch = FetchType.EAGER)
	private List<String> roles;
    
    public User(){
        
    }

    public User(String username, String email, String password, String... roles) {
        this.username = username;
        this.email = email;
        this.password = password;
        likedPosts = new ArrayList<>();
        likedComments = new ArrayList<>();
        this.roles = List.of(roles);
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getUsername(){
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
    }

    public Blob getAvatar() {
		return avatar;
	}

    public List<Long> getLikedPosts() {
		return likedPosts;
	}

    public List<Long> getLikedComments() {
		return likedComments;
	}

    public List<String> getRoles() {
		return roles;
	}

    //Setters

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

    public void setAvatar(Blob avatar) {
		this.avatar = avatar;
	}

    public void setLikedPosts(ArrayList<Long> likedPosts) {
        this.likedPosts = likedPosts;
    }

    public void setLikedComments(ArrayList<Long> likedComments) {
        this.likedComments = likedComments;
    }

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

    @Override
    public String toString() {
        return "User [id=" + id + ", username=" + username + ", mail=" + email + ", password=" + password;
                
    }

}