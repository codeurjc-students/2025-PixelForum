package es.codeurjc.backend.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.service.ImageService;
import es.codeurjc.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Tag("integration")
@DisplayName("ImageService Integration Tests")
@ActiveProfiles("test")
@SpringBootTest
class ImageServiceIntegrationTest {

	@Autowired
	private ImageService imageService;

	@Autowired
	private UserService userService;

	// ------------------- getImageById -------------------

	@Test
	@DisplayName("Should get image by id")
	@Transactional
	void getImageByIdTest() {
		// GIVEN
		User user = userService.findByUsername("martin").orElseThrow();

		Image image = imageService.saveImage("data".getBytes(), "file.png", "image/png", user);

		// WHEN
		Image result = imageService.getImageById(image.getId());

		// THEN
		assertEquals(image.getId(), result.getId());
		assertEquals(user.getId(), result.getOwner().getId());
	}

	@Test
	@DisplayName("Should throw when image not found")
	void getImageByIdNotFoundTest() {
		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> {
			imageService.getImageById(-1L);
		});
	}

	// ------------------- saveImage -------------------

	@Test
	@DisplayName("Should save image correctly")
	@Transactional
	void saveImageTest() {
		// GIVEN
		User user = userService.findByUsername("martin").orElseThrow();

		// WHEN
		Image image = imageService.saveImage("data".getBytes(), "file.png", "image/png", user);

		// THEN
		assertNotNull(image.getId());
		assertEquals("file.png", image.getFilename());
		assertEquals("image/png", image.getContentType());
		assertEquals(user.getId(), image.getOwner().getId());
	}

	// ------------------- uploadImage -------------------

	@Test
	@DisplayName("Should upload image successfully")
	@Transactional
	void uploadImageSuccessTest() throws IOException {
		// GIVEN
		User user = userService.findByUsername("martin").orElseThrow();

		MockMultipartFile file = new MockMultipartFile(
				"file",
				"test.png",
				"image/png",
				"data".getBytes()
		);

		// WHEN
		Long imageId = imageService.uploadImage(file, user);

		// THEN
		assertNotNull(imageId);

		Image saved = imageService.getImageById(imageId);
		assertEquals("test.png", saved.getFilename());
		assertEquals(user.getId(), saved.getOwner().getId());
	}

	@Test
	@DisplayName("Should fail when file is empty")
	void uploadImageEmptyTest() {
		// GIVEN
		User user = userService.findByUsername("martin").orElseThrow();

		MockMultipartFile file = new MockMultipartFile(
				"file",
				"test.png",
				"image/png",
				new byte[0]
		);

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> {
			imageService.uploadImage(file, user);
		});
	}

	@Test
	@DisplayName("Should fail when type not allowed")
	void uploadImageInvalidTypeTest() {
		// GIVEN
		User user = userService.findByUsername("martin").orElseThrow();

		MockMultipartFile file = new MockMultipartFile(
				"file",
				"test.pdf",
				"application/pdf",
				"data".getBytes()
		);

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> {
			imageService.uploadImage(file, user);
		});
	}

	@Test
	@DisplayName("Should fail when file too large")
	void uploadImageTooLargeTest() {
		// GIVEN
		User user = userService.findByUsername("martin").orElseThrow();

		byte[] bigData = new byte[6000000]; // > 5MB

		MockMultipartFile file = new MockMultipartFile(
				"file",
				"big.png",
				"image/png",
				bigData
		);

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> {
			imageService.uploadImage(file, user);
		});
	}

	// ------------------- deleteImage -------------------

	@Test
	@DisplayName("Should delete image when owner")
	@Transactional
	void deleteImageSuccessTest() {
		// GIVEN
		User user = userService.findByUsername("martin").orElseThrow();

		Image image = imageService.saveImage("data".getBytes(), "file.png", "image/png", user);

		// WHEN
		imageService.deleteImage(image.getId(), user);

		// THEN
		assertThrows(EntityNotFoundException.class, () -> {
			imageService.getImageById(image.getId());
		});
	}

	@Test
	@DisplayName("Should allow admin to delete image")
	@Transactional
	void deleteImageAdminTest() {
		// GIVEN
		User user = userService.findByUsername("martin").orElseThrow();
		User admin = userService.findByUsername("admin").orElseThrow();

		Image image = imageService.saveImage("data".getBytes(), "file.png", "image/png", user);

		// WHEN
		imageService.deleteImage(image.getId(), admin);

		// THEN
		assertThrows(EntityNotFoundException.class, () -> {
			imageService.getImageById(image.getId());
		});
	}

	@Test
	@DisplayName("Should fail when deleting image of another user")
	void deleteImageAccessDeniedTest() {
		// GIVEN
		User owner = userService.findByUsername("martin").orElseThrow();
		User other = userService.findByUsername("robert").orElseThrow();

		Image image = imageService.saveImage("data".getBytes(), "file.png", "image/png", owner);

		// WHEN & THEN
		assertThrows(AccessDeniedException.class, () -> {
			imageService.deleteImage(image.getId(), other);
		});
	}

	@Test
	@DisplayName("Should throw when deleting non-existing image")
	void deleteImageNotFoundTest() {
		// GIVEN
		User user = userService.findByUsername("martin").orElseThrow();

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> {
			imageService.deleteImage(-1L, user);
		});
	}
}