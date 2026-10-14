package es.codeurjc.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import es.codeurjc.backend.model.Topic;

public interface TopicRepository extends JpaRepository<Topic, Long> {

}