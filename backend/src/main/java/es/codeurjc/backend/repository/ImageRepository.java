package es.codeurjc.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.User;

public interface ImageRepository extends JpaRepository<Image, Long> {

    void deleteByOwner(User owner);

}