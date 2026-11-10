package es.codeurjc.backend.controller.rest;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.service.ImageService;
import es.codeurjc.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;


@RestController
@RequestMapping("/api/v1/images")
public class ImageRestController {

	private final ImageService imageService;
	private final UserService userService;

	public ImageRestController(ImageService imageService, UserService userService) {
		this.imageService = imageService;
		this.userService = userService;
	}

	private User getCurrentUser(Principal principal) {
		return userService.findByUsername(principal.getName())
				.orElseThrow(() -> new EntityNotFoundException("User not found"));
	}

	@GetMapping("/{id}")
	public ResponseEntity<byte[]> getImage(@PathVariable long id, @RequestParam(required = false) Integer w,
			@RequestParam(required = false) Integer h, @RequestParam(required = false, defaultValue = "95") int q) {

		// Without resize -> return original with its real content type
		if (w == null && h == null) {
			Image image = imageService.getImageById(id);
			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(image.getContentType()))
					.header("Cache-Control", "public, max-age=31536000")
					.body(image.getImageData());
		}

		byte[] imageData = imageService.getImage(id, w, h, q);

		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_JPEG)
				.header("Cache-Control", "public, max-age=31536000")
				.body(imageData);
	}

	@PostMapping
	public ResponseEntity<List<Long>> uploadImages(@RequestParam("files") MultipartFile[] files,
			Principal principal) {
		User currentUser = getCurrentUser(principal);
		List<Long> ids = new ArrayList<>();

		for (MultipartFile file : files) {
			if (!file.isEmpty()) {
				Long id = imageService.uploadImage(file, currentUser);
				ids.add(id);
			}
		}

		if (ids.isEmpty()) {
			throw new IllegalArgumentException("No valid files were provided");
		}

		return ResponseEntity.ok(ids);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteImage(@PathVariable long id, Principal principal) {
		User currentUser = getCurrentUser(principal);
		imageService.deleteImage(id, currentUser);
		return ResponseEntity.noContent().build();
	}
}