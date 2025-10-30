package es.codeurjc.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.codeurjc.backend.model.Topic;

@Repository
public interface TopicRepository extends JpaRepository<Topic,Long>{
    
}