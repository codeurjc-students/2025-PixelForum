package es.codeurjc.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.codeurjc.backend.model.Comment;
import es.codeurjc.backend.model.User;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdAndParentCommentIsNull(Long postId, Pageable pageable);

    List<Comment> findByAuthor(User user);
    Page<Comment> findByAuthor(User author, Pageable pageable);

    void deleteByAuthor(User user);

    Page<Comment> findByUsersThatLikedContains(User user, Pageable pageable);

    @Modifying
    @Query(value = """
            DELETE FROM user_liked_comments
            WHERE comment_id IN :commentIds
            """, nativeQuery = true)
    void deleteLikesByCommentIds(@Param("commentIds") List<Long> commentIds);

}