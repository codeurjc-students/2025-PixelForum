package es.codeurjc.backend.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.ImageRepository;
import es.codeurjc.backend.service.ImageService;
import jakarta.persistence.EntityNotFoundException;

@Tag("unit")
@DisplayName("ImageService Unitary tests")
class ImageServiceUnitTest {

	private ImageService imageService;
	private ImageRepository imageRepository;

	private User user;
	private Image image;

	@BeforeEach
	void init() {
		System.setProperty("java.awt.headless", "true");
		imageRepository = mock(ImageRepository.class);
		imageService = new ImageService(imageRepository);

		user = new User();
		user.setId(1L);
		user.setRoles(List.of("USER"));

		image = new Image();
		image.setId(1L);
		image.setOwner(user);
	}

	// ------------------- getImage -------------------

	@Test
	@DisplayName("getImage should return original image when width and height are null")
	void getImageOriginalTest() {
		// GIVEN
		byte[] data = "image-data".getBytes();
		image.setImageData(data);

		when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

		// WHEN
		byte[] result = imageService.getImage(1L, null, null, 80);

		// THEN
		assertArrayEquals(data, result);
		verify(imageRepository).findById(1L);
	}

	@Test
	@DisplayName("getImage should throw when image does not exist")
	void getImageNotFoundTest() {
		// GIVEN
		when(imageRepository.findById(1L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> {
			imageService.getImage(1L, 100, 100, 80);
		});
	}

	@Test
	@DisplayName("getImage should resize image successfully")
	void getImageResizeTest() throws Exception {
		// GIVEN
		BufferedImage bufferedImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "png", baos);
		image.setImageData(baos.toByteArray());

		when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

		// WHEN
		byte[] result = imageService.getImage(1L, 100, 100, 80);

		// THEN
		assertNotNull(result);
		assertTrue(result.length > 0);
	}

	@Test
	@DisplayName("getImage should return original image when resize fails")
	void getImageResizeFallbackTest() {
		// GIVEN
		byte[] invalidData = "invalid-image".getBytes();
		image.setImageData(invalidData);

		when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

		// WHEN
		byte[] result = imageService.getImage(1L, 100, 100, 80);

		// THEN
		assertArrayEquals(invalidData, result);
	}

	// ------------------- getImageById -------------------

	@Test
	@DisplayName("getImageById should return image when exists")
	void getImageByIdSuccessTest() {
		// GIVEN
		when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

		// WHEN
		Image result = imageService.getImageById(1L);

		// THEN
		assertEquals(image, result);
		verify(imageRepository).findById(1L);
	}

	@Test
	@DisplayName("getImageById should throw when not found")
	void getImageByIdNotFoundTest() {
		// GIVEN
		when(imageRepository.findById(1L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> {
			imageService.getImageById(1L);
		});
	}

	// ------------------- saveImage -------------------

	@Test
	@DisplayName("saveImage should persist image")
	void saveImageTest() {
		// GIVEN
		byte[] data = "test".getBytes();

		when(imageRepository.save(any(Image.class))).thenReturn(image);

		// WHEN
		Image result = imageService.saveImage(data, "file.png", "image/png", user);

		// THEN
		assertEquals(image, result);
		verify(imageRepository).save(any(Image.class));
	}

	@Test
	@DisplayName("saveImage should create image with correct attributes")
	void saveImageAttributesTest() {
		// GIVEN
		byte[] data = "test".getBytes();

		when(imageRepository.save(any(Image.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// WHEN
		Image result = imageService.saveImage(data, "file.png", "image/png", user);

		// THEN
		assertArrayEquals(data, result.getImageData());
		assertEquals("file.png", result.getFilename());
		assertEquals("image/png", result.getContentType());
		assertEquals(user, result.getOwner());
	}

	// ------------------- uploadImage -------------------

	@Test
	@DisplayName("uploadImage should save valid image and return id")
	void uploadImageSuccessTest() throws IOException {
		// GIVEN
		MultipartFile file = mock(MultipartFile.class);

		when(file.isEmpty()).thenReturn(false);
		when(file.getContentType()).thenReturn("image/png");
		when(file.getSize()).thenReturn(100L);
		when(file.getBytes()).thenReturn("data".getBytes());
		when(file.getOriginalFilename()).thenReturn("file.png");

		when(imageRepository.save(any(Image.class))).thenReturn(image);

		// WHEN
		Long id = imageService.uploadImage(file, user);

		// THEN
		assertEquals(1L, id);
		verify(imageRepository).save(any(Image.class));
	}

	@Test
	@DisplayName("uploadImage should throw when file is null")
	void uploadImageNullFileTest() {
		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> {
			imageService.uploadImage(null, user);
		});
	}

	@Test
	@DisplayName("uploadImage should throw when file is empty")
	void uploadImageEmptyFileTest() {
		// GIVEN
		MultipartFile file = mock(MultipartFile.class);
		when(file.isEmpty()).thenReturn(true);

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> {
			imageService.uploadImage(file, user);
		});
	}

	@Test
	@DisplayName("uploadImage should throw when file type not allowed")
	void uploadImageInvalidTypeTest() {
		// GIVEN
		MultipartFile file = mock(MultipartFile.class);

		when(file.isEmpty()).thenReturn(false);
		when(file.getContentType()).thenReturn("application/pdf");

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> {
			imageService.uploadImage(file, user);
		});
	}

	@Test
	@DisplayName("uploadImage should throw when file too large")
	void uploadImageTooLargeTest() {
		// GIVEN
		MultipartFile file = mock(MultipartFile.class);

		when(file.isEmpty()).thenReturn(false);
		when(file.getContentType()).thenReturn("image/png");
		when(file.getSize()).thenReturn(6000000L); // > 5MB

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> {
			imageService.uploadImage(file, user);
		});
	}

	@Test
	@DisplayName("uploadImage should throw RuntimeException when IO fails")
	void uploadImageIOExceptionTest() throws IOException {
		// GIVEN
		MultipartFile file = mock(MultipartFile.class);

		when(file.isEmpty()).thenReturn(false);
		when(file.getContentType()).thenReturn("image/png");
		when(file.getSize()).thenReturn(100L);
		when(file.getBytes()).thenThrow(new IOException());

		// WHEN & THEN
		assertThrows(RuntimeException.class, () -> {
			imageService.uploadImage(file, user);
		});
	}

	// ------------------- deleteImage -------------------

	@Test
	@DisplayName("deleteImage should delete when owner")
	void deleteImageSuccessTest() {
		// GIVEN
		when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

		// WHEN
		imageService.deleteImage(1L, user);

		// THEN
		verify(imageRepository).deleteById(1L);
	}

	@Test
	@DisplayName("deleteImage should allow admin")
	void deleteImageAdminTest() {
		// GIVEN
		User admin = new User();
		admin.setId(2L);
		admin.setRoles(List.of("ADMIN"));

		when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

		// WHEN
		imageService.deleteImage(1L, admin);

		// THEN
		verify(imageRepository).deleteById(1L);
	}

	@Test
	@DisplayName("deleteImage should throw AccessDeniedException when not owner")
	void deleteImageAccessDeniedTest() {
		// GIVEN
		User other = new User();
		other.setId(99L);
		other.setRoles(List.of("USER"));

		when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

		// WHEN & THEN
		assertThrows(AccessDeniedException.class, () -> {
			imageService.deleteImage(1L, other);
		});
	}

	@Test
	@DisplayName("deleteImage should throw when not found")
	void deleteImageNotFoundTest() {
		// GIVEN
		when(imageRepository.findById(1L)).thenReturn(Optional.empty());

		// WHEN & THEN
		assertThrows(EntityNotFoundException.class, () -> {
			imageService.deleteImage(1L, user);
		});
	}
}