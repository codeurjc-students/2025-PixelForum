package es.codeurjc.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE "
            + "(:title IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND "
            + "(:authorUsername IS NULL OR p.author.username = :authorUsername) AND "
            + "(:topic IS NULL OR p.topic.name = :topic)")
    Page<Post> findByFilters(
            @Param("title") String title,
            @Param("authorUsername") String authorUsername,
            @Param("topic") String topic,
            Pageable pageable);

    void deleteByAuthor(User user);

}