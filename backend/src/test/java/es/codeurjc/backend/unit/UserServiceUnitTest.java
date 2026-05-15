package es.codeurjc.backend.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import es.codeurjc.backend.dto.post.PostDTO;
import es.codeurjc.backend.dto.post.PostMapper;
import es.codeurjc.backend.dto.user.BasicUserDTO;
import es.codeurjc.backend.dto.user.ChangePasswordDTO;
import es.codeurjc.backend.dto.user.CreateUserDTO;
import es.codeurjc.backend.dto.user.UserDTO;
import es.codeurjc.backend.dto.user.UserMapper;
import es.codeurjc.backend.model.Image;
import es.codeurjc.backend.model.Post;
import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.CommentRepository;
import es.codeurjc.backend.repository.ImageRepository;
import es.codeurjc.backend.repository.PostRepository;
import es.codeurjc.backend.repository.UserRepository;
import es.codeurjc.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;

@Tag("unit")
@DisplayName("UserService Unitary tests")
class UserServiceUnitTest {

    private UserService userService;

    private UserRepository userRepository;
    private PostRepository postRepository;
    private PostMapper postMapper;
    private CommentRepository commentRepository;
    private ImageRepository imageRepository;
    private PasswordEncoder passwordEncoder;
    private UserMapper userMapper;

    private User user;
    private User admin;
    private User otherUser;
    private Image image;
    private Post post;
    private PostDTO postDTO;

    @BeforeEach
    void init() {
        userRepository = mock(UserRepository.class);
        postRepository = mock(PostRepository.class);
        postMapper = mock(PostMapper.class);
        commentRepository = mock(CommentRepository.class);
        imageRepository = mock(ImageRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userMapper = mock(UserMapper.class);

        userService = new UserService(userMapper, userRepository, postRepository, postMapper, commentRepository,
                imageRepository, passwordEncoder);

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRoles(List.of("USER"));
        user.setCreatedAt(LocalDateTime.now());

        admin = new User();
        admin.setId(2L);
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setPassword("encodedPassword");
        admin.setRoles(List.of("USER", "ADMIN"));

        otherUser = new User();
        otherUser.setId(99L);
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("encodedPassword");
        otherUser.setRoles(List.of("USER"));

        image = new Image();
        image.setId(1L);
        image.setOwner(user);
        image.setContentType("image/png");
        image.setFilename("avatar.png");

        post = new Post();
        post.setId(1L);
        post.setAuthor(user);
        post.setUsersThatLiked(new ArrayList<>());

        postDTO = new PostDTO(1L, "Test Post", "Content", LocalDateTime.now(), LocalDateTime.now(),
                new BasicUserDTO(1L, "testuser", LocalDateTime.now(), "Bio", null), null, 0, false, List.of());
    }

    // =============== getUser ===============

    @Test
    @DisplayName("getUser should return user when exists")
    void getUserSuccessTest() {
        // GIVEN
        BasicUserDTO basicUserDTO = new BasicUserDTO(1L, "testuser", LocalDateTime.now(), "Bio", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toBasicDTO(user)).thenReturn(basicUserDTO);

        // WHEN
        BasicUserDTO result = userService.getUser(1L);

        // THEN
        assertEquals(basicUserDTO, result);
        verify(userRepository).findById(1L);
        verify(userMapper).toBasicDTO(user);
    }

    @Test
    @DisplayName("getUser should throw EntityNotFoundException when user does not exist")
    void getUserNotFoundTest() {
        // GIVEN
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> {
            userService.getUser(999L);
        });
    }

    // =============== getUsers ===============

    @Test
    @DisplayName("getUsers should return page of users")
    void getUsersSuccessTest() {
        // GIVEN
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = List.of(user, admin);
        Page<User> userPage = new PageImpl<>(users, pageable, 2);
        List<BasicUserDTO> dtos = List.of(
                new BasicUserDTO(1L, "testuser", LocalDateTime.now(), "Bio", null),
                new BasicUserDTO(2L, "admin", LocalDateTime.now(), "Admin bio", null));

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toBasicDTO(user)).thenReturn(dtos.get(0));
        when(userMapper.toBasicDTO(admin)).thenReturn(dtos.get(1));

        // WHEN
        Page<BasicUserDTO> result = userService.getUsers(pageable);

        // THEN
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("getUsers should return empty page when no users exist")
    void getUsersEmptyTest() {
        // GIVEN
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findAll(pageable)).thenReturn(emptyPage);

        // WHEN
        Page<BasicUserDTO> result = userService.getUsers(pageable);

        // THEN
        assertEquals(0, result.getContent().size());
        verify(userRepository).findAll(pageable);
    }

    // =============== getUserDetails ===============

    @Test
    @DisplayName("getUserDetails should return user details when user is the owner")
    void getUserDetailsOwnerTest() {
        // GIVEN
        UserDTO userDTO = new UserDTO(1L, "testuser", "test@example.com", LocalDateTime.now(), "Bio", null, List.of(),
                List.of("USER"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        // WHEN
        UserDTO result = userService.getUserDetails(1L, user);

        // THEN
        assertEquals(userDTO, result);
        verify(userRepository).findById(1L);
        verify(userMapper).toDTO(user);
    }

    @Test
    @DisplayName("getUserDetails should allow admin to view any user details")
    void getUserDetailsAdminTest() {
        // GIVEN
        UserDTO userDTO = new UserDTO(1L, "testuser", "test@example.com", LocalDateTime.now(), "Bio", null, List.of(),
                List.of("USER"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        // WHEN
        UserDTO result = userService.getUserDetails(1L, admin);

        // THEN
        assertEquals(userDTO, result);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("getUserDetails should throw AccessDeniedException when user is not owner or admin")
    void getUserDetailsAccessDeniedTest() {
        // GIVEN
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // WHEN & THEN
        assertThrows(AccessDeniedException.class, () -> {
            userService.getUserDetails(1L, otherUser);
        });
        verify(userRepository, never()).findById(1L);
    }

    @Test
    @DisplayName("getUserDetails should throw EntityNotFoundException when user not found")
    void getUserDetailsNotFoundTest() {
        // GIVEN
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> {
            userService.getUserDetails(999L, admin);
        });
    }

    // =============== findByUsername ===============

    @Test
    @DisplayName("findByUsername should return user when username exists")
    void findByUsernameSuccessTest() {
        // GIVEN
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // WHEN
        Optional<User> result = userService.findByUsername("testuser");

        // THEN
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("findByUsername should return empty Optional when username does not exist")
    void findByUsernameNotFoundTest() {
        // GIVEN
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // WHEN
        Optional<User> result = userService.findByUsername("nonexistent");

        // THEN
        assertTrue(result.isEmpty());
        verify(userRepository).findByUsername("nonexistent");
    }

    // =============== addUser ===============

    @Test
    @DisplayName("addUser should encode password and add USER role")
    void addUserSuccessTest() {
        // GIVEN
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("plainPassword");

        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // WHEN
        userService.addUser(newUser);

        // THEN
        verify(passwordEncoder).encode("plainPassword");
        verify(userRepository).save(any(User.class));
        assertEquals(List.of("USER"), newUser.getRoles());
    }

    @Test
    @DisplayName("addUser should assign ADMIN role when username is 'admin'")
    void addUserAdminTest() {
        // GIVEN
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPassword("plainPassword");

        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        // WHEN
        userService.addUser(adminUser);

        // THEN
        verify(passwordEncoder).encode("plainPassword");
        assertEquals(List.of("USER", "ADMIN"), adminUser.getRoles());
    }

    // =============== save ===============

    @Test
    @DisplayName("save should persist user")
    void saveTest() {
        // GIVEN
        when(userRepository.save(user)).thenReturn(user);

        // WHEN
        User result = userService.save(user);

        // THEN
        assertEquals(user, result);
        verify(userRepository).save(user);
    }

    // =============== createUser ===============

    @Test
    @DisplayName("createUser should create new user successfully")
    void createUserSuccessTest() {
        // GIVEN
        CreateUserDTO userDTO = new CreateUserDTO("newuser", "new@example.com", "password", "My bio");
        User createdUser = new User();
        createdUser.setId(3L);
        createdUser.setUsername("newuser");
        createdUser.setEmail("new@example.com");
        createdUser.setPassword("encodedPassword");
        createdUser.setRoles(List.of("USER"));

        UserDTO resultDTO = new UserDTO(3L, "newuser", "new@example.com", LocalDateTime.now(), "Bio", null, List.of(),
                List.of("USER"));

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userMapper.toDomain(userDTO)).thenReturn(createdUser);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(createdUser);
        when(userMapper.toDTO(createdUser)).thenReturn(resultDTO);

        // WHEN
        UserDTO result = userService.createUser(userDTO);

        // THEN
        assertEquals(resultDTO, result);
        verify(userRepository).findByUsername("newuser");
        verify(userRepository).findByEmail("new@example.com");
        verify(passwordEncoder).encode("encodedPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser should throw IllegalArgumentException when username already taken")
    void createUserUsernameTakenTest() {
        // GIVEN
        CreateUserDTO userDTO = new CreateUserDTO("testuser", "new@example.com", "password", "My bio");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(userDTO);
        });
        verify(userRepository).findByUsername("testuser");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("createUser should throw IllegalArgumentException when email already taken")
    void createUserEmailTakenTest() {
        // GIVEN
        CreateUserDTO userDTO = new CreateUserDTO("newuser", "test@example.com", "password", "My bio");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(userDTO);
        });
        verify(userRepository).findByUsername("newuser");
        verify(userRepository).findByEmail("test@example.com");
    }

    // =============== updateUser ===============

    @Test
    @DisplayName("updateUser should update username when user is owner")
    void updateUserChangeUsernameTest() {
        // GIVEN
        CreateUserDTO userDTO = new CreateUserDTO("newusername", null, null, null);
        UserDTO resultDTO = new UserDTO(1L, "newusername", "test@example.com", LocalDateTime.now(), "Bio", null,
                List.of(), List.of("USER"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("newusername")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(resultDTO);

        // WHEN
        UserDTO result = userService.updateUser(1L, userDTO, user);

        // THEN
        assertEquals(resultDTO, result);
        verify(userRepository).findById(1L);
        verify(userRepository).findByUsername("newusername");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser should update email when user is owner")
    void updateUserChangeEmailTest() {
        // GIVEN
        CreateUserDTO userDTO = new CreateUserDTO(null, "newemail@example.com", null, null);
        UserDTO resultDTO = new UserDTO(1L, "testuser", "newemail@example.com", LocalDateTime.now(), "Bio", null,
                List.of(), List.of("USER"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(resultDTO);

        // WHEN
        UserDTO result = userService.updateUser(1L, userDTO, user);

        // THEN
        assertEquals(resultDTO, result);
        verify(userRepository).findById(1L);
        verify(userRepository).findByEmail("newemail@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser should update bio when provided")
    void updateUserChangeBioTest() {
        // GIVEN
        CreateUserDTO userDTO = new CreateUserDTO(null, null, null, "New bio");
        UserDTO resultDTO = new UserDTO(1L, "testuser", "test@example.com", LocalDateTime.now(), "New bio", null,
                List.of(), List.of("USER"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(resultDTO);

        // WHEN
        UserDTO result = userService.updateUser(1L, userDTO, user);

        // THEN
        assertEquals(resultDTO, result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser should allow admin to update any user")
    void updateUserAdminTest() {
        // GIVEN
        CreateUserDTO userDTO = new CreateUserDTO("newusername", null, null, null);
        UserDTO resultDTO = new UserDTO(1L, "newusername", "test@example.com", LocalDateTime.now(), "Bio", null,
                List.of(), List.of("USER"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("newusername")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(resultDTO);

        // WHEN
        UserDTO result = userService.updateUser(1L, userDTO, admin);

        // THEN
        assertEquals(resultDTO, result);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("updateUser should throw AccessDeniedException when user is not owner or admin")
    void updateUserAccessDeniedTest() {
        // GIVEN
        CreateUserDTO userDTO = new CreateUserDTO("newusername", null, null, null);

        // WHEN & THEN
        assertThrows(AccessDeniedException.class, () -> {
            userService.updateUser(1L, userDTO, otherUser);
        });
        verify(userRepository, never()).findById(1L);
    }

    @Test
    @DisplayName("updateUser should throw IllegalArgumentException when new username already taken")
    void updateUserUsernameTakenTest() {
        // GIVEN
        CreateUserDTO userDTO = new CreateUserDTO("admin", null, null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(1L, userDTO, user);
        });
    }

    @Test
    @DisplayName("updateUser should throw IllegalArgumentException when new email already taken")
    void updateUserEmailTakenTest() {
        // GIVEN
        CreateUserDTO userDTO = new CreateUserDTO("admin", "admin@example.com", null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUser(1L, userDTO, user);
        });
    }

    @Test
    @DisplayName("updateUser should not check username if it does not change")
    void updateUserSameUsernameTest() {
        // GIVEN
        CreateUserDTO userDTO = new CreateUserDTO("testuser", null, null, null);
        UserDTO resultDTO = new UserDTO(1L, "testuser", "test@example.com", LocalDateTime.now(), "Bio", null, List.of(),
                List.of("USER"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(resultDTO);

        // WHEN
        UserDTO result = userService.updateUser(1L, userDTO, user);

        // THEN
        assertEquals(resultDTO, result);
        verify(userRepository, never()).findByUsername("testuser");
    }

    @Test
    @DisplayName("updateUser should not check email if it does not change")
    void updateUserSameEmailTest() {
        // GIVEN
        CreateUserDTO userDTO = new CreateUserDTO("testuser", "test@example.com", null, null);
        UserDTO resultDTO = new UserDTO(1L, "testuser", "test@example.com", LocalDateTime.now(), "Bio", null, List.of(),
                List.of("USER"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(resultDTO);

        // WHEN
        UserDTO result = userService.updateUser(1L, userDTO, user);

        // THEN
        assertEquals(resultDTO, result);
        verify(userRepository, never()).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("updateUser should throw EntityNotFoundException when user not found")
    void updateUserNotFoundTest() {
        // GIVEN
        CreateUserDTO userDTO = new CreateUserDTO("newusername", null, null, null);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> {
            userService.updateUser(999L, userDTO, admin);
        });
    }

    // =============== changePassword ===============

    @Test
    @DisplayName("changePassword should change password when old password is correct")
    void changePasswordSuccessTest() {
        // GIVEN
        ChangePasswordDTO dto = new ChangePasswordDTO("oldPassword", "newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        // WHEN
        userService.changePassword(1L, dto, user);

        // THEN
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword");
    }

    @Test
    @DisplayName("changePassword should throw IllegalArgumentException when old password is incorrect")
    void changePasswordWrongOldPasswordTest() {
        // GIVEN
        ChangePasswordDTO dto = new ChangePasswordDTO("wrongPassword", "newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> {
            userService.changePassword(1L, dto, user);
        });
        verify(passwordEncoder, never()).encode("newPassword");
    }

    @Test
    @DisplayName("changePassword should allow admin to change any user password")
    void changePasswordAdminTest() {
        // GIVEN
        ChangePasswordDTO dto = new ChangePasswordDTO("oldPassword", "newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        // WHEN
        userService.changePassword(1L, dto, admin);

        // THEN
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
    }

    @Test
    @DisplayName("changePassword should throw AccessDeniedException when user is not owner or admin")
    void changePasswordAccessDeniedTest() {
        // GIVEN
        ChangePasswordDTO dto = new ChangePasswordDTO("oldPassword", "newPassword");

        // WHEN & THEN
        assertThrows(AccessDeniedException.class, () -> {
            userService.changePassword(1L, dto, otherUser);
        });
        verify(userRepository, never()).findById(1L);
    }

    @Test
    @DisplayName("changePassword should throw EntityNotFoundException when user not found")
    void changePasswordNotFoundTest() {
        // GIVEN
        ChangePasswordDTO dto = new ChangePasswordDTO("oldPassword", "newPassword");
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> {
            userService.changePassword(999L, dto, admin);
        });
    }

    // =============== deleteUser ===============

    @Test
    @DisplayName("deleteUser should delete user when user is owner")
    void deleteUserSuccessTest() {
        // GIVEN
        user.setLikedPosts(new ArrayList<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.findByAuthor(user)).thenReturn(List.of());

        // WHEN
        userService.deleteUser(1L, user);

        // THEN
        verify(userRepository).findById(1L);
        verify(commentRepository).deleteByAuthor(user);
        verify(postRepository).findByAuthor(user);
        verify(postRepository).deleteByAuthor(user);
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("deleteUser should remove user likes from posts")
    void deleteUserRemoveLikesTest() {
        // GIVEN
        Post likedPost = new Post();
        likedPost.setId(2L);
        likedPost.setUsersThatLiked(new ArrayList<>(List.of(user)));
        likedPost.setLikes(1);
        user.setLikedPosts(new ArrayList<>(List.of(likedPost)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.findByAuthor(user)).thenReturn(List.of());

        // WHEN
        userService.deleteUser(1L, user);

        // THEN
        assertTrue(user.getLikedPosts().isEmpty());
        assertTrue(likedPost.getUsersThatLiked().isEmpty());
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("deleteUser should remove likes on user posts")
    void deleteUserRemoveLikesOnPostsTest() {
        // GIVEN
        post.getUsersThatLiked().add(otherUser);
        otherUser.setLikedPosts(new ArrayList<>(List.of(post)));
        user.setLikedPosts(new ArrayList<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.findByAuthor(user)).thenReturn(List.of(post));

        // WHEN
        userService.deleteUser(1L, user);

        // THEN
        assertTrue(post.getUsersThatLiked().isEmpty());
        verify(postRepository).deleteByAuthor(user);
    }

    @Test
    @DisplayName("deleteUser should delete user avatar if exists")
    void deleteUserWithAvatarTest() {
        // GIVEN
        user.setAvatar(image);
        user.setLikedPosts(new ArrayList<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.findByAuthor(user)).thenReturn(List.of());

        // WHEN
        userService.deleteUser(1L, user);

        // THEN
        assertNull(user.getAvatar());
        verify(imageRepository).delete(image);
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("deleteUser should allow admin to delete any user")
    void deleteUserAdminTest() {
        // GIVEN
        user.setLikedPosts(new ArrayList<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.findByAuthor(user)).thenReturn(List.of());

        // WHEN
        userService.deleteUser(1L, admin);

        // THEN
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("deleteUser should throw AccessDeniedException when user is not owner or admin")
    void deleteUserAccessDeniedTest() {
        // WHEN & THEN
        assertThrows(AccessDeniedException.class, () -> {
            userService.deleteUser(1L, otherUser);
        });
        verify(userRepository, never()).findById(1L);
    }

    @Test
    @DisplayName("deleteUser should throw EntityNotFoundException when user not found")
    void deleteUserNotFoundTest() {
        // GIVEN
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> {
            userService.deleteUser(999L, admin);
        });
    }

    // =============== setProfileImage ===============

    @Test
    @DisplayName("setProfileImage should set profile image when user is owner")
    void setProfileImageSuccessTest() {
        // GIVEN
        image.setPost(null);
        UserDTO userDTO = new UserDTO(1L, "testuser", "test@example.com", LocalDateTime.now(), "Bio", null, List.of(),
                List.of("USER"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        // WHEN
        UserDTO result = userService.setProfileImage(1L, 1L, user);

        // THEN
        assertEquals(userDTO, result);
        assertEquals(image, user.getAvatar());
        verify(imageRepository).findById(1L);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("setProfileImage should delete old avatar when setting new one")
    void setProfileImageReplaceAvatarTest() {
        // GIVEN
        Image oldAvatar = new Image();
        oldAvatar.setId(999L);
        user.setAvatar(oldAvatar);
        image.setPost(null);

        UserDTO userDTO = new UserDTO(1L, "testuser", "test@example.com", LocalDateTime.now(), "Bio", null, List.of(),
                List.of("USER"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        // WHEN
        userService.setProfileImage(1L, 1L, user);

        // THEN
        verify(imageRepository).delete(oldAvatar);
        assertEquals(image, user.getAvatar());
    }

    @Test
    @DisplayName("setProfileImage should throw AccessDeniedException when image is not owned by user")
    void setProfileImageUnownedImageTest() {
        // GIVEN
        image.setOwner(otherUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

        // WHEN & THEN
        assertThrows(AccessDeniedException.class, () -> {
            userService.setProfileImage(1L, 1L, user);
        });
    }

    @Test
    @DisplayName("setProfileImage should allow admin to set image for any user")
    void setProfileImageAdminTest() {
        // GIVEN
        image.setOwner(user);
        image.setPost(null);
        UserDTO userDTO = new UserDTO(1L, "testuser", "test@example.com", LocalDateTime.now(), "Bio", null, List.of(),
                List.of("USER"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        // WHEN
        UserDTO result = userService.setProfileImage(1L, 1L, admin);

        // THEN
        assertEquals(userDTO, result);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("setProfileImage should throw IllegalArgumentException when image is associated with post")
    void setProfileImagePostImageTest() {
        // GIVEN
        image.setPost(post);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> {
            userService.setProfileImage(1L, 1L, user);
        });
    }

    @Test
    @DisplayName("setProfileImage should delete image and return null when content type not allowed")
    void setProfileImageInvalidTypeTest() {
        // GIVEN
        image.setContentType("image/gif");
        image.setPost(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

        // WHEN
        UserDTO result = userService.setProfileImage(1L, 1L, user);

        // THEN
        assertNull(result);
        verify(imageRepository).delete(image);
    }

    @Test
    @DisplayName("setProfileImage should throw AccessDeniedException when user is not owner or admin")
    void setProfileImageAccessDeniedTest() {
        // WHEN & THEN
        assertThrows(AccessDeniedException.class, () -> {
            userService.setProfileImage(1L, 1L, otherUser);
        });
    }

    @Test
    @DisplayName("setProfileImage should throw EntityNotFoundException when user not found")
    void setProfileImageUserNotFoundTest() {
        // GIVEN
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> {
            userService.setProfileImage(999L, 1L, admin);
        });
    }

    @Test
    @DisplayName("setProfileImage should throw EntityNotFoundException when image not found")
    void setProfileImageImageNotFoundTest() {
        // GIVEN
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(imageRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> {
            userService.setProfileImage(1L, 999L, user);
        });
    }

    // =============== removeProfileImage ===============

    @Test
    @DisplayName("removeProfileImage should remove user avatar")
    void removeProfileImageSuccessTest() {
        // GIVEN
        user.setAvatar(image);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // WHEN
        userService.removeProfileImage(1L, user);

        // THEN
        assertNull(user.getAvatar());
        verify(imageRepository).delete(image);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("removeProfileImage should not fail when user has no avatar")
    void removeProfileImageNoAvatarTest() {
        // GIVEN
        user.setAvatar(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // WHEN
        userService.removeProfileImage(1L, user);

        // THEN
        verify(imageRepository, never()).delete(any());
        verify(userRepository, never()).save(user);
    }

    @Test
    @DisplayName("removeProfileImage should allow admin to remove any user avatar")
    void removeProfileImageAdminTest() {
        // GIVEN
        user.setAvatar(image);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // WHEN
        userService.removeProfileImage(1L, admin);

        // THEN
        assertNull(user.getAvatar());
        verify(imageRepository).delete(image);
    }

    @Test
    @DisplayName("removeProfileImage should throw AccessDeniedException when user is not owner or admin")
    void removeProfileImageAccessDeniedTest() {
        // WHEN & THEN
        assertThrows(AccessDeniedException.class, () -> {
            userService.removeProfileImage(1L, otherUser);
        });
    }

    @Test
    @DisplayName("removeProfileImage should throw EntityNotFoundException when user not found")
    void removeProfileImageNotFoundTest() {
        // GIVEN
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> {
            userService.removeProfileImage(999L, admin);
        });
    }

    // =============== getLikedPosts ===============

    @Test
    @DisplayName("getLikedPosts should return liked posts for user")
    void getLikedPostsSuccessTest() {
        // GIVEN
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> posts = List.of(post);
        Page<Post> postPage = new PageImpl<>(posts, pageable, 1);
        List<PostDTO> postDTOs = List.of(postDTO);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.findByUsersThatLikedContains(user, pageable)).thenReturn(postPage);
        when(postMapper.toDTOWithLike(post, user)).thenReturn(postDTOs.get(0));

        // WHEN
        Page<PostDTO> result = userService.getLikedPosts(1L, user, pageable);

        // THEN
        assertEquals(1, result.getContent().size());
        verify(userRepository).findById(1L);
        verify(postRepository).findByUsersThatLikedContains(user, pageable);
    }

    @Test
    @DisplayName("getLikedPosts should return empty page when user has no liked posts")
    void getLikedPostsEmptyTest() {
        // GIVEN
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.findByUsersThatLikedContains(user, pageable)).thenReturn(emptyPage);

        // WHEN
        Page<PostDTO> result = userService.getLikedPosts(1L, user, pageable);

        // THEN
        assertEquals(0, result.getContent().size());
        verify(postRepository).findByUsersThatLikedContains(user, pageable);
    }

    @Test
    @DisplayName("getLikedPosts should allow admin to view any user liked posts")
    void getLikedPostsAdminTest() {
        // GIVEN
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> posts = List.of(post);
        Page<Post> postPage = new PageImpl<>(posts, pageable, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.findByUsersThatLikedContains(user, pageable)).thenReturn(postPage);
        when(postMapper.toDTOWithLike(post, admin)).thenReturn(postDTO);

        // WHEN
        Page<PostDTO> result = userService.getLikedPosts(1L, admin, pageable);

        // THEN
        assertEquals(1, result.getContent().size());
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("getLikedPosts should throw AccessDeniedException when user is not owner or admin")
    void getLikedPostsAccessDeniedTest() {
        // GIVEN
        Pageable pageable = PageRequest.of(0, 10);

        // WHEN & THEN
        assertThrows(AccessDeniedException.class, () -> {
            userService.getLikedPosts(1L, otherUser, pageable);
        });
        verify(userRepository, never()).findById(1L);
    }

    @Test
    @DisplayName("getLikedPosts should throw EntityNotFoundException when user not found")
    void getLikedPostsNotFoundTest() {
        // GIVEN
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> {
            userService.getLikedPosts(999L, admin, pageable);
        });
    }
}