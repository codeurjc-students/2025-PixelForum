package es.codeurjc.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.codeurjc.backend.model.User;

@Repository
public interface UserRepository extends JpaRepository<User,Long>{

    List<User> findByEmail(String email);

    Optional<User> findByUsername(String username);
    
}