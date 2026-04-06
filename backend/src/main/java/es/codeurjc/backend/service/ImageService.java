package es.codeurjc.backend.service;

import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.ImageRepository;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private static final long MAX_FILE_SIZE = 5242880; // 5 MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public Image getImageById(Long id) {
        return imageRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Image not found"));
    }

    public Image saveImage(byte[] data, String filename, String contentType, User user) {
        Image image = new Image(data, filename, contentType, user);
        return imageRepository.save(image);
    }

    public Long uploadImage(MultipartFile file, User user) {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The file is empty");
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("File type not allowed");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds the maximum size of 5MB");
        }

        try {
            // Create Image
            Image image = new Image(
                    file.getBytes(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    user);

            // Save to DB and return Id
            Image savedImage = imageRepository.save(image);
            return savedImage.getId();
        } catch (IOException e) {
            throw new IllegalStateException("Error processing the file", e);
        }
    }

    public void deleteImage(Long id, User user) {
        Image image = imageRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Image not found"));
        if (image.getOwner().getId() != user.getId() && !user.getRoles().contains("ADMIN")) {
            throw new AccessDeniedException("You can only delete your own images");
        }
        imageRepository.deleteById(id);
    }
}