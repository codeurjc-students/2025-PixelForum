package es.codeurjc.backend.controller.rest;

import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.service.ImageService;
import es.codeurjc.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/images")
public class ImageRestController {

    private final ImageService imageService;
    private final UserService userService;

    public ImageRestController(ImageService imageService, UserService userService) {
        this.imageService = imageService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable long id) {
        Image image = imageService.getImageById(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .body(image.getImageData());
    }

    @PostMapping
    public ResponseEntity<List<Long>> uploadImages(@RequestParam("files") MultipartFile[] files,
            Principal principal) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().build();
        }
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<Long> ids = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                Long id = imageService.uploadImage(file, currentUser);
                ids.add(id);
            }
        }

        if (ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(ids);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable long id, Principal principal) {
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        imageService.deleteImage(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}