package es.codeurjc.backend.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import es.codeurjc.backend.model.User;
import es.codeurjc.backend.service.CommentService;
import es.codeurjc.backend.service.DatabaseInitializer;
import es.codeurjc.backend.service.ImageService;
import es.codeurjc.backend.service.PostService;
import es.codeurjc.backend.service.TopicService;
import es.codeurjc.backend.service.UserService;

@Tag("unit")
@DisplayName("DatabaseInitializer Unitary tests")
class DatabaseInitializerUnitTest {

	@Test
	@DisplayName("uploadImage should throw IOException when the resource does not exist")
	void uploadImageResourceNotFoundTest() throws Exception {
		// GIVEN
		DatabaseInitializer initializer = new DatabaseInitializer(mock(PostService.class), mock(UserService.class),
				mock(TopicService.class), mock(CommentService.class), mock(ImageService.class));

		Method uploadImage = DatabaseInitializer.class.getDeclaredMethod("uploadImage", String.class, User.class);
		uploadImage.setAccessible(true);

		// WHEN & THEN
		InvocationTargetException ex = assertThrows(InvocationTargetException.class,
				() -> uploadImage.invoke(initializer, "/images/does-not-exist.png", new User()));
		assertInstanceOf(IOException.class, ex.getCause());
		assertEquals("Image not found: /images/does-not-exist.png", ex.getCause().getMessage());
	}
}