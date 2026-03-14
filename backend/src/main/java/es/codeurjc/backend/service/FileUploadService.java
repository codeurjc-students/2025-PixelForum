package es.codeurjc.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {

    @Value("${upload.dir:uploads/}")
    private String uploadDir;

    @Value("${upload.allowed-extensions:jpg,jpeg,png,gif,webp}")
    private String allowedExtensions;

    public FileUploadService() {
        try {
            Files.createDirectories(Paths.get("uploads"));
        } catch (IOException e) {
            throw new IllegalStateException("Could not create uploads directory", e);
        }
    }

    public String uploadFile(MultipartFile file) throws IOException {
        return uploadFile(file, "default");
    }

    public String uploadFile(MultipartFile file, String category) throws IOException {
        // Validate file is not empty
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Get original filename and extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();

        // Validate file extension
        if (!isAllowedExtension(extension)) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + allowedExtensions);
        }

        // Create category directory if it doesn't exist
        Path categoryDir = Paths.get(uploadDir, category);
        Files.createDirectories(categoryDir);

        // Sanitize filename: remove spaces and special characters
        String sanitizedFilename = sanitizeFilename(originalFilename);

        // Generate unique filename to avoid conflicts
        String uniqueFilename = UUID.randomUUID().toString() + "_" + sanitizedFilename;
        Path filepath = categoryDir.resolve(uniqueFilename);

        // Save file to disk
        Files.write(filepath, file.getBytes());

        // Return relative URL path
        return "/api/v1/images/" + category + "/" + uniqueFilename;
    }

    private String sanitizeFilename(String filename) {
        // Replace spaces with underscores
        String sanitized = filename.replaceAll("\\s+", "_");
        // Remove special characters, keep only alphanumeric, underscore, hyphen, and dot
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "");
        // Remove multiple consecutive dots
        sanitized = sanitized.replaceAll("\\.{2,}", ".");
        // Remove leading/trailing dots
        sanitized = sanitized.replaceAll("(^\\.+)|(\\.+$)", "");
        return sanitized;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private boolean isAllowedExtension(String extension) {
        String[] allowed = allowedExtensions.split(",");
        for (String ext : allowed) {
            if (ext.trim().equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    public void deleteFile(String filename) throws IOException {
        deleteFile(filename, "default");
    }

    public void deleteFile(String filename, String category) throws IOException {
        Path filepath = Paths.get(uploadDir, category, filename);
        if (Files.exists(filepath)) {
            Files.delete(filepath);
        }
    }
}