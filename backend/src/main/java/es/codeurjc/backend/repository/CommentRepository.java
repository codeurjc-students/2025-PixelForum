package es.codeurjc.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.codeurjc.backend.model.Comment;
import es.codeurjc.backend.model.User;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Long>{

    void deleteByAuthor(User user);
    
}