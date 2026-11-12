package es.codeurjc.backend.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.ImageRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class ImageService {

	private static final long MAX_FILE_SIZE = 5242880; // 5 MB
	private static final int MAX_DIMENSION = 2000;
	private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
	private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

	private final ImageRepository imageRepository;

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

	public byte[] getImage(Long id, Integer width, Integer height, int quality) {
		Image image = getImageById(id);
		// If no resizing is requested, return original data
		if (width == null && height == null) {
			return image.getImageData();
		}

		if (isInvalidDimension(width) || isInvalidDimension(height)) {
			throw new IllegalArgumentException("Dimensions must be between 1 and " + MAX_DIMENSION);
		}
		int safeQuality = Math.clamp(quality, 1, 100);

		try {
			return resizeImage(image.getImageData(), width, height, safeQuality);
		} catch (IOException e) {
			logger.error("Error resizing image. ID: {}, Content-Type: {}, Image Size: {} bytes", id,
					image.getContentType(), image.getImageData().length, e);
			// If resizing fails, return original image as fallback
			return image.getImageData();
		}
	}

	private boolean isInvalidDimension(Integer dimension) {
		return dimension != null && (dimension <= 0 || dimension > MAX_DIMENSION);
	}

	private byte[] resizeImage(byte[] imageData, Integer targetWidth, Integer targetHeight, int quality)
			throws IOException {
		// Read original image
		BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
		if (originalImage == null) {
			throw new IOException("Could not read image data");
		}

		// If only one dimension is given, use it for both (square crop)
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

		// Scale the image (at least 1px to avoid invalid sizes)
		int scaledWidth = Math.max(1, (int) (originalWidth * scale));
		int scaledHeight = Math.max(1, (int) (originalHeight * scale));

		BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = scaled.createGraphics();
		// White background so transparent images don't end up black in JPEG
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, scaledWidth, scaledHeight);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.drawImage(img, 0, 0, scaledWidth, scaledHeight, null);
		g2d.dispose();

		// Crop to target dimensions
		int x = (scaledWidth - targetWidth) / 2;
		int y = (scaledHeight - targetHeight) / 2;

		BufferedImage cropped = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = cropped.createGraphics();
		g.drawImage(scaled, -x, -y, null);
		g.dispose();

		return cropped;
	}

	private byte[] toJpegBytes(BufferedImage image, int quality) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		var writers = ImageIO.getImageWritersByFormatName("JPEG");
		if (!writers.hasNext()) {
			throw new IOException("No JPEG writer found");
		}

		ImageWriter writer = writers.next();
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
			writer.setOutput(ios);

			ImageWriteParam params = writer.getDefaultWriteParam();
			params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			params.setCompressionQuality(quality / 100f);

			writer.write(null, new IIOImage(image, null, null), params);
		} finally {
			writer.dispose();
		}

		return baos.toByteArray();
	}

	public Long uploadImage(MultipartFile file, User user) {
		// Validate file
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("The file is empty");
		}

		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
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
					contentType,
					user);

			// Save to DB and return Id
			Image savedImage = imageRepository.save(image);
			return savedImage.getId();
		} catch (IOException e) {
			throw new IllegalStateException("Error processing the file", e);
		}
	}

	public void deleteImage(Long id, User user) {
		Image image = getImageById(id);
		if (image.getOwner().getId() != user.getId() && !user.getRoles().contains("ADMIN")) {
			throw new AccessDeniedException("You can only delete your own images");
		}
		if (image.getPost() != null) {
			throw new IllegalArgumentException("Image is associated with a post and cannot be deleted");
		}
		User owner = image.getOwner();
		if (owner.getAvatar() != null && owner.getAvatar().getId() == image.getId()) {
			throw new IllegalArgumentException("Image is a profile picture and cannot be deleted");
		}
		imageRepository.delete(image);
	}
}