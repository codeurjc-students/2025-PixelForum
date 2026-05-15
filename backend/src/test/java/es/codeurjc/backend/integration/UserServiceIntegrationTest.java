package es.codeurjc.backend.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import es.codeurjc.backend.dto.post.PostDTO;
import es.codeurjc.backend.dto.user.BasicUserDTO;
import es.codeurjc.backend.dto.user.ChangePasswordDTO;
import es.codeurjc.backend.dto.user.CreateUserDTO;
import es.codeurjc.backend.dto.user.UserDTO;
import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.service.ImageService;
import es.codeurjc.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Tag("integration")
@DisplayName("UserService Integration Tests")
@ActiveProfiles("test")
@SpringBootTest
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ImageService imageService;

    // =============== getUser ===============

    @Test
    @DisplayName("Should get user by id successfully")
    @Transactional
    void getUserSuccessTest() {
        // GIVEN
        User existingUser = userService.findByUsername("martin").orElseThrow();

        // WHEN
        BasicUserDTO result = userService.getUser(existingUser.getId());

        // THEN
        assertNotNull(result);
        assertEquals(existingUser.getId(), result.id());
        assertEquals("martin", result.username());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user does not exist")
    void getUserNotFoundTest() {
        // GIVEN
        Long nonExistentId = -1L;

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> userService.getUser(nonExistentId));
    }

    // =============== getUsers ===============

    @Test
    @DisplayName("Should return paginated list of users")
    void getUsersSuccessTest() {
        // GIVEN
        Pageable pageable = PageRequest.of(0, 10);

        // WHEN
        Page<BasicUserDTO> result = userService.getUsers(pageable);

        // THEN
        assertNotNull(result);
        assertTrue(result.getTotalElements() > 0);
        assertTrue(result.getContent().size() > 0);
    }

    @Test
    @DisplayName("Should return empty page when pagination exceeds total users")
    void getUsersEmptyPageTest() {
        // GIVEN
        Pageable pageable = PageRequest.of(100, 10);

        // WHEN
        Page<BasicUserDTO> result = userService.getUsers(pageable);

        // THEN
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    // =============== getUserDetails ===============

    @Test
    @DisplayName("Should get user details when user is the owner")
    @Transactional
    void getUserDetailsOwnerTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();

        // WHEN
        UserDTO result = userService.getUserDetails(user.getId(), user);

        // THEN
        assertNotNull(result);
        assertEquals(user.getId(), result.id());
        assertEquals("martin", result.username());
        assertEquals(user.getEmail(), result.email());
    }

    @Test
    @DisplayName("Should allow admin to view any user details")
    @Transactional
    void getUserDetailsAdminTest() {
        // GIVEN
        User targetUser = userService.findByUsername("martin").orElseThrow();
        User adminUser = userService.findByUsername("admin").orElseThrow();

        // WHEN
        UserDTO result = userService.getUserDetails(targetUser.getId(), adminUser);

        // THEN
        assertNotNull(result);
        assertEquals(targetUser.getId(), result.id());
        assertEquals("martin", result.username());
    }

    @Test
    @DisplayName("Should deny access when user tries to view another user's details")
    void getUserDetailsAccessDeniedTest() {
        // GIVEN
        User martin = userService.findByUsername("martin").orElseThrow();
        User robert = userService.findByUsername("robert").orElseThrow();

        // WHEN & THEN
        Long martinId = martin.getId();
        assertThrows(AccessDeniedException.class, () -> userService.getUserDetails(martinId, robert));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user does not exist")
    void getUserDetailsNotFoundTest() {
        // GIVEN
        User admin = userService.findByUsername("admin").orElseThrow();
        Long nonExistentId = -1L;

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> userService.getUserDetails(nonExistentId, admin));
    }

    // =============== findByUsername ===============

    @Test
    @DisplayName("Should find user by existing username")
    void findByUsernameSuccessTest() {
        // GIVEN
        String username = "martin";

        // WHEN
        var result = userService.findByUsername(username);

        // THEN
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
    }

    @Test
    @DisplayName("Should return empty Optional when username does not exist")
    void findByUsernameNotFoundTest() {
        // GIVEN
        String nonExistentUsername = "nonexistentuser123";

        // WHEN
        var result = userService.findByUsername(nonExistentUsername);

        // THEN
        assertTrue(result.isEmpty());
    }

    // =============== createUser ===============

    @Test
    @DisplayName("Should create new user successfully")
    @Transactional
    void createUserSuccessTest() {
        // GIVEN
        CreateUserDTO newUserDTO = new CreateUserDTO("newuser" + System.currentTimeMillis(),
                "newuser" + System.currentTimeMillis() + "@example.com", "password123", "My bio");

        // WHEN
        UserDTO result = userService.createUser(newUserDTO);

        // THEN
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(newUserDTO.username(), result.username());
        assertEquals(newUserDTO.email(), result.email());
        assertTrue(result.roles().contains("USER"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when username already exists")
    void createUserUsernameTakenTest() {
        // GIVEN
        CreateUserDTO newUserDTO = new CreateUserDTO("martin", "different@example.com", "password123", "My bio");

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(newUserDTO));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when email already exists")
    void createUserEmailTakenTest() {
        // GIVEN
        User existingUser = userService.findByUsername("martin").orElseThrow();
        CreateUserDTO newUserDTO = new CreateUserDTO("differentuser", existingUser.getEmail(), "password123", "My bio");

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(newUserDTO));
    }

    // =============== updateUser ===============

    @Test
    @DisplayName("Should update user username successfully")
    @Transactional
    void updateUserChangeUsernameTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        String newUsername = "martin_updated_" + System.currentTimeMillis();
        CreateUserDTO updateDTO = new CreateUserDTO(newUsername, null, null, null);

        // WHEN
        UserDTO result = userService.updateUser(user.getId(), updateDTO, user);

        // THEN
        assertNotNull(result);
        assertEquals(newUsername, result.username());
    }

    @Test
    @DisplayName("Should update user email successfully")
    @Transactional
    void updateUserChangeEmailTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        String newEmail = "martin_" + System.currentTimeMillis() + "@example.com";
        CreateUserDTO updateDTO = new CreateUserDTO(null, newEmail, null, null);

        // WHEN
        UserDTO result = userService.updateUser(user.getId(), updateDTO, user);

        // THEN
        assertNotNull(result);
        assertEquals(newEmail, result.email());
    }

    @Test
    @DisplayName("Should update user bio successfully")
    @Transactional
    void updateUserChangeBioTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        String newBio = "New bio updated at " + System.currentTimeMillis();
        CreateUserDTO updateDTO = new CreateUserDTO(null, null, null, newBio);

        // WHEN
        UserDTO result = userService.updateUser(user.getId(), updateDTO, user);

        // THEN
        assertNotNull(result);
        assertEquals(newBio, result.bio());
    }

    @Test
    @DisplayName("Should allow admin to update any user")
    @Transactional
    void updateUserAdminTest() {
        // GIVEN
        User targetUser = userService.findByUsername("martin").orElseThrow();
        User adminUser = userService.findByUsername("admin").orElseThrow();
        String newBio = "Updated by admin " + System.currentTimeMillis();
        CreateUserDTO updateDTO = new CreateUserDTO(null, null, null, newBio);

        // WHEN
        UserDTO result = userService.updateUser(targetUser.getId(), updateDTO, adminUser);

        // THEN
        assertNotNull(result);
        assertEquals(newBio, result.bio());
    }

    @Test
    @DisplayName("Should deny access when non-owner tries to update user")
    void updateUserAccessDeniedTest() {
        // GIVEN
        User martin = userService.findByUsername("martin").orElseThrow();
        User robert = userService.findByUsername("robert").orElseThrow();
        CreateUserDTO updateDTO = new CreateUserDTO("newusername", null, null, null);

        // WHEN & THEN
        Long martinId = martin.getId();
        assertThrows(AccessDeniedException.class, () -> userService.updateUser(martinId, updateDTO, robert));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when updating to existing username")
    @Transactional
    void updateUserUsernameTakenTest() {
        // GIVEN
        User martin = userService.findByUsername("martin").orElseThrow();
        User robert = userService.findByUsername("robert").orElseThrow();
        CreateUserDTO updateDTO = new CreateUserDTO(robert.getUsername(), null, null, null);

        // WHEN & THEN
        Long martinId = martin.getId();
        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(martinId, updateDTO, martin));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent user")
    void updateUserNotFoundTest() {
        // GIVEN
        User admin = userService.findByUsername("admin").orElseThrow();
        Long nonExistentId = -1L;
        CreateUserDTO updateDTO = new CreateUserDTO("newusername", null, null, null);

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> userService.updateUser(nonExistentId, updateDTO, admin));
    }

    // =============== changePassword ===============

    @Test
    @DisplayName("Should change password successfully with correct old password")
    @Transactional
    void changePasswordSuccessTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        String oldPassword = "user";
        String newPassword = "newPassword123";
        ChangePasswordDTO dto = new ChangePasswordDTO(oldPassword, newPassword);

        // WHEN
        userService.changePassword(user.getId(), dto, user);

        // THEN
        User updatedUser = userService.findByUsername("martin").orElseThrow();
        assertNotNull(updatedUser.getPassword());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when old password is incorrect")
    void changePasswordWrongOldPasswordTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        String wrongPassword = "wrongPassword123";
        String newPassword = "newPassword123";
        ChangePasswordDTO dto = new ChangePasswordDTO(wrongPassword, newPassword);

        // WHEN & THEN
        Long userId = user.getId();
        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(userId, dto, user));
    }

    @Test
    @DisplayName("Should allow admin to change any user password")
    @Transactional
    void changePasswordAdminTest() {
        // GIVEN
        User targetUser = userService.findByUsername("martin").orElseThrow();
        User adminUser = userService.findByUsername("admin").orElseThrow();
        String oldPassword = "user";
        String newPassword = "adminChangedPassword123";
        ChangePasswordDTO dto = new ChangePasswordDTO(oldPassword, newPassword);

        // WHEN
        userService.changePassword(targetUser.getId(), dto, adminUser);

        // THEN
        User updatedUser = userService.findByUsername("martin").orElseThrow();
        assertNotNull(updatedUser.getPassword());
    }

    @Test
    @DisplayName("Should deny access when non-owner tries to change password")
    void changePasswordAccessDeniedTest() {
        // GIVEN
        User martin = userService.findByUsername("martin").orElseThrow();
        User robert = userService.findByUsername("robert").orElseThrow();
        ChangePasswordDTO dto = new ChangePasswordDTO("password", "newPassword");

        // WHEN & THEN
        Long martinId = martin.getId();
        assertThrows(AccessDeniedException.class, () -> userService.changePassword(martinId, dto, robert));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when changing password of non-existent user")
    void changePasswordNotFoundTest() {
        // GIVEN
        User admin = userService.findByUsername("admin").orElseThrow();
        Long nonExistentId = -1L;
        ChangePasswordDTO dto = new ChangePasswordDTO("password", "newPassword");

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> userService.changePassword(nonExistentId, dto, admin));
    }

    // =============== deleteUser ===============

    @Test
    @DisplayName("Should delete user successfully when user is owner")
    @Transactional
    void deleteUserSuccessTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        Long userId = user.getId();

        // WHEN
        userService.deleteUser(userId, user);

        // THEN
        assertThrows(EntityNotFoundException.class, () -> userService.getUser(userId));
    }

    @Test
    @DisplayName("Should allow admin to delete any user")
    @Transactional
    void deleteUserAdminTest() {
        // GIVEN
        User targetUser = userService.findByUsername("robert").orElseThrow();
        User adminUser = userService.findByUsername("admin").orElseThrow();
        Long userId = targetUser.getId();

        // WHEN
        userService.deleteUser(userId, adminUser);

        // THEN
        assertThrows(EntityNotFoundException.class, () -> userService.getUser(userId));
    }

    @Test
    @DisplayName("Should delete user avatar if user has one")
    @Transactional
    void deleteUserWithAvatarTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        Image avatar = imageService.saveImage("data".getBytes(), "avatar.png", "image/png", user);
        user.setAvatar(avatar);
        userService.save(user);

        Long userId = user.getId();

        // WHEN
        userService.deleteUser(userId, user);

        // THEN
        assertThrows(EntityNotFoundException.class, () -> userService.getUser(userId));
    }

    @Test
    @DisplayName("Should deny access when non-owner tries to delete user")
    void deleteUserAccessDeniedTest() {
        // GIVEN
        User martin = userService.findByUsername("martin").orElseThrow();
        User robert = userService.findByUsername("robert").orElseThrow();

        // WHEN & THEN
        Long martinId = martin.getId();
        assertThrows(AccessDeniedException.class, () -> userService.deleteUser(martinId, robert));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting non-existent user")
    void deleteUserNotFoundTest() {
        // GIVEN
        User admin = userService.findByUsername("admin").orElseThrow();
        Long nonExistentId = -1L;

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> userService.deleteUser(nonExistentId, admin));
    }

    // =============== setProfileImage ===============

    @Test
    @DisplayName("Should set profile image successfully when user is owner")
    @Transactional
    void setProfileImageSuccessTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        Image image = imageService.saveImage("data".getBytes(), "avatar.png", "image/png", user);

        // WHEN
        UserDTO result = userService.setProfileImage(user.getId(), image.getId(), user);

        // THEN
        assertNotNull(result);
        assertEquals(user.getId(), result.id());
        assertEquals(image.getId(), result.avatar());
    }

    @Test
    @DisplayName("Should replace old avatar when setting new profile image")
    @Transactional
    void setProfileImageReplaceAvatarTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        Image oldAvatar = imageService.saveImage("olddata".getBytes(), "old_avatar.png", "image/png", user);
        Image newAvatar = imageService.saveImage("newdata".getBytes(), "new_avatar.png", "image/png", user);

        user.setAvatar(oldAvatar);
        userService.save(user);

        Long oldAvatarId = oldAvatar.getId();

        // WHEN
        UserDTO result = userService.setProfileImage(user.getId(), newAvatar.getId(), user);

        // THEN
        assertNotNull(result);
        assertEquals(newAvatar.getId(), result.avatar());
        assertThrows(EntityNotFoundException.class, () -> imageService.getImageById(oldAvatarId));
    }

    @Test
    @DisplayName("Should deny access when trying to set image of another user")
    void setProfileImageUnownedImageTest() {
        // GIVEN
        User martin = userService.findByUsername("martin").orElseThrow();
        User robert = userService.findByUsername("robert").orElseThrow();
        Image image = imageService.saveImage("data".getBytes(), "avatar.png", "image/png", robert);

        // WHEN & THEN
        Long martinId = martin.getId();
        Long imageId = image.getId();
        assertThrows(AccessDeniedException.class, () -> userService.setProfileImage(martinId, imageId, martin));
    }

    @Test
    @DisplayName("Should allow admin to set profile image for any user")
    @Transactional
    void setProfileImageAdminTest() {
        // GIVEN
        User targetUser = userService.findByUsername("martin").orElseThrow();
        User adminUser = userService.findByUsername("admin").orElseThrow();
        Image image = imageService.saveImage("data".getBytes(), "avatar.png", "image/png", targetUser);

        // WHEN
        UserDTO result = userService.setProfileImage(targetUser.getId(), image.getId(), adminUser);

        // THEN
        assertNotNull(result);
        assertEquals(image.getId(), result.avatar());
    }

    @Test
    @DisplayName("Should reject image associated with post as profile picture")
    @Transactional
    void setProfileImagePostImageTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        Image image = imageService.saveImage("data".getBytes(), "post_image.png", "image/png", user);
        image.setPost(new es.codeurjc.backend.model.Post());

        // WHEN & THEN
        Long userId = user.getId();
        Long imageId = image.getId();
        assertThrows(IllegalArgumentException.class, () -> userService.setProfileImage(userId, imageId, user));
    }

    @Test
    @DisplayName("Should reject image with invalid content type")
    @Transactional
    void setProfileImageInvalidTypeTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        Image image = imageService.saveImage("data".getBytes(), "image.gif", "image/gif", user);

        // WHEN
        UserDTO result = userService.setProfileImage(user.getId(), image.getId(), user);

        // THEN
        assertNull(result);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user does not exist")
    void setProfileImageUserNotFoundTest() {
        // GIVEN
        User admin = userService.findByUsername("admin").orElseThrow();
        Image image = imageService.saveImage("data".getBytes(), "avatar.png", "image/png", admin);
        Long nonExistentUserId = -1L;

        // WHEN & THEN
        Long imageId = image.getId();
        assertThrows(EntityNotFoundException.class,
                () -> userService.setProfileImage(nonExistentUserId, imageId, admin));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when image does not exist")
    void setProfileImageImageNotFoundTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        Long nonExistentImageId = -1L;

        // WHEN & THEN
        Long userId = user.getId();
        assertThrows(EntityNotFoundException.class,
                () -> userService.setProfileImage(userId, nonExistentImageId, user));
    }

    // =============== removeProfileImage ===============

    @Test
    @DisplayName("Should remove profile image successfully")
    @Transactional
    void removeProfileImageSuccessTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        Image avatar = imageService.saveImage("data".getBytes(), "avatar.png", "image/png", user);
        user.setAvatar(avatar);
        userService.save(user);

        // WHEN
        userService.removeProfileImage(user.getId(), user);

        // THEN
        BasicUserDTO updatedUser = userService.getUser(user.getId());
        assertNull(updatedUser.avatar());
    }

    @Test
    @DisplayName("Should not fail when user has no avatar")
    void removeProfileImageNoAvatarTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        user.setAvatar(null);
        userService.save(user);

        // WHEN - Should not throw any exception
        assertDoesNotThrow(() -> userService.removeProfileImage(user.getId(), user));

        // THEN
        User updatedUser = userService.findByUsername("martin").orElseThrow();
        assertNull(updatedUser.getAvatar());
    }

    @Test
    @DisplayName("Should allow admin to remove any user avatar")
    @Transactional
    void removeProfileImageAdminTest() {
        // GIVEN
        User targetUser = userService.findByUsername("martin").orElseThrow();
        User adminUser = userService.findByUsername("admin").orElseThrow();
        Image avatar = imageService.saveImage("data".getBytes(), "avatar.png", "image/png", targetUser);
        targetUser.setAvatar(avatar);
        userService.save(targetUser);

        // WHEN
        userService.removeProfileImage(targetUser.getId(), adminUser);

        // THEN
        User updatedUser = userService.findByUsername("martin").orElseThrow();
        assertNull(updatedUser.getAvatar());
    }

    @Test
    @DisplayName("Should deny access when non-owner tries to remove avatar")
    void removeProfileImageAccessDeniedTest() {
        // GIVEN
        User martin = userService.findByUsername("martin").orElseThrow();
        User robert = userService.findByUsername("robert").orElseThrow();

        // WHEN & THEN
        Long martinId = martin.getId();
        assertThrows(AccessDeniedException.class, () -> userService.removeProfileImage(martinId, robert));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user does not exist")
    void removeProfileImageNotFoundTest() {
        // GIVEN
        User admin = userService.findByUsername("admin").orElseThrow();
        Long nonExistentId = -1L;

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> userService.removeProfileImage(nonExistentId, admin));
    }

    // =============== getLikedPosts ===============

    @Test
    @DisplayName("Should return paginated liked posts for user")
    @Transactional
    void getLikedPostsSuccessTest() {
        // GIVEN
        User user = userService.findByUsername("martin").orElseThrow();
        Pageable pageable = PageRequest.of(0, 10);

        // WHEN
        Page<PostDTO> result = userService.getLikedPosts(user.getId(), user, pageable);

        // THEN
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should allow admin to view any user liked posts")
    @Transactional
    void getLikedPostsAdminTest() {
        // GIVEN
        User targetUser = userService.findByUsername("martin").orElseThrow();
        User adminUser = userService.findByUsername("admin").orElseThrow();
        Pageable pageable = PageRequest.of(0, 10);

        // WHEN
        Page<PostDTO> result = userService.getLikedPosts(targetUser.getId(), adminUser, pageable);

        // THEN
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should deny access when non-owner tries to view liked posts")
    void getLikedPostsAccessDeniedTest() {
        // GIVEN
        User martin = userService.findByUsername("martin").orElseThrow();
        User robert = userService.findByUsername("robert").orElseThrow();
        Pageable pageable = PageRequest.of(0, 10);

        // WHEN & THEN
        Long martinId = martin.getId();
        assertThrows(AccessDeniedException.class, () -> userService.getLikedPosts(martinId, robert, pageable));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user does not exist")
    void getLikedPostsNotFoundTest() {
        // GIVEN
        User admin = userService.findByUsername("admin").orElseThrow();
        Long nonExistentId = -1L;
        Pageable pageable = PageRequest.of(0, 10);

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> userService.getLikedPosts(nonExistentId, admin, pageable));
    }
}