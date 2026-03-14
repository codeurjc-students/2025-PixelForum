package es.codeurjc.backend.controller.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.codeurjc.backend.service.FileUploadService;

@RestController
@RequestMapping("/api/v1/images")
public class ImageRestController {

    private final FileUploadService fileUploadService;

    public ImageRestController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "category", defaultValue = "default") String category) {

        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().build();
            }

            // Validate category (prevent directory traversal)
            if (!isValidCategory(category)) {
                return ResponseEntity.badRequest().build();
            }

            List<String> urls = new ArrayList<>();

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String url = fileUploadService.uploadFile(file, category);
                    urls.add(url);
                }
            }

            if (urls.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok(new UploadResponse(urls));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{category}/{filename}")
    public ResponseEntity<byte[]> getImage(
            @PathVariable String category,
            @PathVariable String filename) {

        try {
            // Validate inputs
            if (!isValidCategory(category) || !isValidFilename(filename)) {
                return ResponseEntity.badRequest().build();
            }

            Path filepath = Paths.get("uploads", category, filename);

            // Prevent directory traversal attacks
            Path uploadDir = Paths.get("uploads", category).toRealPath();
            Path realPath = filepath.toRealPath();

            if (!realPath.startsWith(uploadDir)) {
                return ResponseEntity.badRequest().build();
            }

            if (!Files.exists(filepath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = Files.readAllBytes(filepath);
            String contentType = getContentType(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{category}/{filename}/delete")
    public ResponseEntity<Void> deleteImage(
            @PathVariable String category,
            @PathVariable String filename) {

        try {
            // Validate inputs
            if (!isValidCategory(category) || !isValidFilename(filename)) {
                return ResponseEntity.badRequest().build();
            }

            fileUploadService.deleteFile(filename, category);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean isValidCategory(String category) {
        return category != null &&
                !category.isEmpty() &&
                category.matches("^[a-zA-Z0-9_-]+$") &&
                !category.contains("..") &&
                !category.equals(".");
    }

    private boolean isValidFilename(String filename) {
        return filename != null &&
                !filename.isEmpty() &&
                filename.matches("^[a-zA-Z0-9_.-]+\\.(jpg|jpeg|png|gif|webp)$") &&
                !filename.contains("..") &&
                !filename.equals(".");
    }

    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    public record UploadResponse(List<String> urls) {
    }
}