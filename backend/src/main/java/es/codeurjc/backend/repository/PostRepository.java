package es.codeurjc.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;

@Repository
public interface PostRepository extends JpaRepository<Post,Long>{

    void deleteByAuthor(User user);
    
}
