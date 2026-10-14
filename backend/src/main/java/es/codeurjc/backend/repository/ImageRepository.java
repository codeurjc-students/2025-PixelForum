package es.codeurjc.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import es.codeurjc.backend.model.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {

}