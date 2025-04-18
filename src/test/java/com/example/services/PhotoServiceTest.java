package com.example.services;

import com.example.dao.PhotoDao;
import com.example.dao.PhotoTagDao;
import com.example.dao.TagDao;
import com.example.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.file.UploadedFile;

import javax.persistence.EntityManager;
import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock
    private PhotoDao photoDao;

    @Mock
    private PhotoTagDao photoTagDao;

    @Mock
    private TagDao tagDao;

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private PhotoTagService photoTagService;

    @Mock
    private RatingService ratingService;

    @Mock
    private LeaderboardService leaderboardService;

    @Mock
    private EntityManager em;

    @Mock
    private UploadedFile uploadedFile;

    @InjectMocks
    private PhotoService photoService;

    private User user;
    private Photo photo;
    private Tag tag;
    private String imageDirectory = "/tmp/images";
    private List<String> allowedTypes = Arrays.asList("image/jpeg", "image/png", "image/gif");
    private long maxFileSize = 1024 * 1024; // 1MB

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserName("testUser");
        user.setRole("USER");

        photo = new Photo();
        photo.setId(1L);
        photo.setUser(user);

        tag = new Tag();
        tag.setTagName("testTag");
    }

    @Test
    void savePhoto_Success() throws IOException {
        // Arrange
        List<String> tagNames = Arrays.asList("testTag");
        when(uploadedFile.getSize()).thenReturn(100L);
        when(uploadedFile.getContentType()).thenReturn("image/jpeg");
        when(uploadedFile.getFileName()).thenReturn("test.jpg");
        when(uploadedFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        when(photoTagService.findTagByName("testTag")).thenReturn(tag);

        // Act
        assertDoesNotThrow(() -> photoService.savePhoto(photo, uploadedFile, tagNames, user, imageDirectory, maxFileSize, allowedTypes));

        // Assert
        verify(photoDao).save(any(Photo.class));
        verify(photoTagService).addTagsToPhoto(eq(photo), anyList(), eq(user));
        verify(uploadedFile).getInputStream();
    }

    @Test
    void savePhoto_NullFile_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> photoService.savePhoto(photo, null, null, user, imageDirectory, maxFileSize, allowedTypes));
        assertEquals("No image selected", exception.getMessage());
    }

    @Test
    void savePhoto_EmptyFile_ThrowsException() {
        // Arrange
        when(uploadedFile.getSize()).thenReturn(0L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> photoService.savePhoto(photo, uploadedFile, null, user, imageDirectory, maxFileSize, allowedTypes));
        assertEquals("Empty image file", exception.getMessage());
    }

    @Test
    void savePhoto_FileTooLarge_ThrowsException() {
        // Arrange
        when(uploadedFile.getSize()).thenReturn(maxFileSize + 1);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> photoService.savePhoto(photo, uploadedFile, null, user, imageDirectory, maxFileSize, allowedTypes));
        assertEquals("Maximum file size is 1MB", exception.getMessage());
    }

    @Test
    void savePhoto_InvalidFileType_ThrowsException() {
        // Arrange
        when(uploadedFile.getSize()).thenReturn(100L);
        when(uploadedFile.getContentType()).thenReturn("image/bmp");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> photoService.savePhoto(photo, uploadedFile, null, user, imageDirectory, maxFileSize, allowedTypes));
        assertEquals("Only JPEG, PNG, and GIF files are allowed", exception.getMessage());
    }

    @Test
    void ratePhoto_Success_NewRating() {
        // Arrange
        Map<Long, Integer> ratingMap = new HashMap<>();
        when(ratingService.userRatingExists(user, photo)).thenReturn(null);
        when(em.find(Photo.class, photo.getId())).thenReturn(photo);

        // Act
        Photo result = photoService.ratePhoto(photo, user, 4, ratingMap);

        // Assert
        verify(ratingService).save(any(Rating.class));
        verify(ratingService).recalculateImageRating(photo, 4.0);
        verify(ratingService).recalculateUserRating(user);
        assertEquals(4, ratingMap.get(photo.getId()));
        assertEquals(photo, result);
    }

    @Test
    void ratePhoto_UpdateExistingRating() {
        // Arrange
        Rating existingRating = new Rating();
        existingRating.setRating(3.0);
        Map<Long, Integer> ratingMap = new HashMap<>();
        when(ratingService.userRatingExists(user, photo)).thenReturn(existingRating);
        when(em.find(Photo.class, photo.getId())).thenReturn(photo);

        // Act
        Photo result = photoService.ratePhoto(photo, user, 5, ratingMap);

        // Assert
        verify(ratingService).update(existingRating);
        verify(ratingService).adjustRatingsForReRating(photo, user, 3.0, 5.0);
        assertEquals(5, ratingMap.get(photo.getId()));
        assertEquals(photo, result);
    }

    @Test
    void ratePhoto_NullUser_ThrowsException() {
        // Arrange
        Map<Long, Integer> ratingMap = new HashMap<>();

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> photoService.ratePhoto(photo, null, 4, ratingMap));
        assertEquals("Please login to rate photos", exception.getMessage());
    }

    @Test
    void ratePhoto_InvalidRating_ThrowsException() {
        // Arrange
        Map<Long, Integer> ratingMap = new HashMap<>();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> photoService.ratePhoto(photo, user, 6, ratingMap));
        assertEquals("Invalid rating value", exception.getMessage());
    }

    @Test
    void deletePhoto_Success() {
        photo.setUser(user);
        assertDoesNotThrow(() -> photoService.deletePhoto(photo, user));

        verify(photoDao).deleteById(photo.getId());
        verify(ratingService).recalculateUserRating(user);
        verify(leaderboardService).updateLeaderBoard(user);
    }

    @Test
    void deletePhoto_Unauthorized_ThrowsException() {
        // Arrange
        User otherUser = new User();
        otherUser.setUserName("otherUser");
        photo.setUser(otherUser);

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class,
                () -> photoService.deletePhoto(photo, user));
        assertEquals("You can only delete your own photos", exception.getMessage());
    }

    @Test
    void updatePhoto_Success() throws IOException {
        // Arrange
        photo.setUser(user);
        when(uploadedFile.getSize()).thenReturn(100L);
        when(uploadedFile.getContentType()).thenReturn("image/jpeg");
        when(uploadedFile.getFileName()).thenReturn("test.jpg");
        when(uploadedFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

        // Act
        assertDoesNotThrow(() -> photoService.updatePhoto(photo, uploadedFile, user, imageDirectory, maxFileSize, allowedTypes));

        // Assert
        verify(photoDao).update(photo);
        assertEquals("PENDING", photo.getStatus());
    }

    @Test
    void updatePhoto_Unauthorized_ThrowsException() {
        // Arrange
        User otherUser = new User();
        otherUser.setUserName("otherUser");
        photo.setUser(otherUser);

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class,
                () -> photoService.updatePhoto(photo, uploadedFile, user, imageDirectory, maxFileSize, allowedTypes));
        assertEquals("You can only update your own photos", exception.getMessage());
    }

    @Test
    void approvePhoto_Success() {
        // Arrange
        User admin = new User();
        admin.setRole("ADMIN");

        // Act
        assertDoesNotThrow(() -> photoService.approvePhoto(photo, admin));

        // Assert
        verify(photoDao).update(photo);
        assertEquals("APPROVED", photo.getStatus());
        assertEquals(admin, photo.getApprovedBy());
        assertNotNull(photo.getApprovedDate());
    }

    @Test
    void approvePhoto_NonAdmin_ThrowsException() {
        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class,
                () -> photoService.approvePhoto(photo, user));
        assertEquals("Admin role required", exception.getMessage());
    }

    @Test
    void getFilteredPhotos_Success() {
        // Arrange
        List<Photo> photos = Collections.singletonList(photo);
        Map<String, Object> filters = new HashMap<>();
        when(photoDao.findFilteredPhotos(anyMap(), eq(0), eq(10))).thenReturn(photos);

        // Act
        List<Photo> result = photoService.getFilteredPhotos(0, 10, "location", Arrays.asList("tag"), 3.0, "search", user);

        // Assert
        assertEquals(photos, result);
        verify(photoDao).findFilteredPhotos(anyMap(), eq(0), eq(10));
    }
}