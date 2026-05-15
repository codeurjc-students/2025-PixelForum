package es.codeurjc.backend.service;

import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.ImageRepository;
import jakarta.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private static final long MAX_FILE_SIZE = 5242880; // 5 MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final String IMAGE_NOT_FOUND = "Image not found";
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public byte[] getImage(Long id, Integer width, Integer height, int quality) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(IMAGE_NOT_FOUND));
        // If no resizing is requested, return original data
        if (width == null && height == null) {
            return image.getImageData();
        }

        try {
            return resizeImage(image.getImageData(), width, height, quality);
        } catch (IOException e) {
            logger.error("Error resizing image. ID: {}, Content-Type: {}, Image Size: {} bytes", id, image.getContentType(), image.getImageData().length, e);
            // If resizing fails, return original image as fallback
            return image.getImageData();
        }
    }

    private byte[] resizeImage(byte[] imageData, Integer targetWidth, Integer targetHeight, int quality)
            throws IOException {
        // Read original image
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        if (originalImage == null) {
            throw new IOException("Could not read image data");
        }

        // If only one dimension is provided, calculate the other to maintain aspect ratio
        int width = targetWidth != null ? targetWidth : targetHeight;
        int height = targetHeight != null ? targetHeight : targetWidth;

        // Crop and resize
        BufferedImage resized = scaleAndCrop(originalImage, width, height);

        // Convert to JPEG
        return toJpegBytes(resized, quality);
    }

    private BufferedImage scaleAndCrop(BufferedImage img, int targetWidth, int targetHeight) {
        int originalWidth = img.getWidth();
        int originalHeight = img.getHeight();

        // Calculate scale to fill the target dimensions
        double scaleX = (double) targetWidth / originalWidth;
        double scaleY = (double) targetHeight / originalHeight;
        double scale = Math.max(scaleX, scaleY);

        // Scale the image
        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);

        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(
                java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(img, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        // Crop to target dimensions
        int x = (scaledWidth - targetWidth) / 2;
        int y = (scaledHeight - targetHeight) / 2;

        BufferedImage cropped = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        cropped.getGraphics().drawImage(scaled, -x, -y, null);

        return cropped;
    }

    private byte[] toJpegBytes(BufferedImage image, int quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        var writers = ImageIO.getImageWritersByFormatName("JPEG");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer found");
        }

        var writer = writers.next();
        var ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        var params = writer.getDefaultWriteParam();
        params.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(Math.min(1.0f, quality / 100f));

        writer.write(null, new javax.imageio.IIOImage(image, null, null), params);
        writer.dispose();
        ios.close();

        return baos.toByteArray();
    }

    public Image getImageById(Long id) {
        return imageRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(IMAGE_NOT_FOUND));
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
        Image image = imageRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(IMAGE_NOT_FOUND));
        if (image.getOwner().getId() != user.getId() && !user.getRoles().contains("ADMIN")) {
            throw new AccessDeniedException("You can only delete your own images");
        }
        imageRepository.deleteById(id);
    }
}