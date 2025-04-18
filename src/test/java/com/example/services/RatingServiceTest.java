package com.example.services;

import com.example.dao.PhotoDao;
import com.example.dao.RatingDao;
import com.example.dao.UserDao;
import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingDao ratingDao;

    @Mock
    private PhotoDao photoDao;

    @Mock
    private UserDao userDao;

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private RatingService ratingService;

    private User testUser;
    private Photo testPhoto;
    private Rating testRating;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("testuser");

        testPhoto = new Photo();
        testPhoto.setId(1L);
        testPhoto.setUser(testUser);

        testRating = new Rating();
        testRating.setId(1L);
        testRating.setRating(4.0);
        testRating.setUser(testUser);
        testRating.setPhoto(testPhoto);
    }

    @Test
    void save_ShouldReturnTrueWhenRatingIsSaved() {
        when(ratingDao.save(any(Rating.class))).thenReturn(true);

        boolean result = ratingService.save(testRating);
        assertTrue(result);
        verify(ratingDao).save(testRating);
    }

    @Test
    void hasRating_ShouldReturnTrueWhenRatingExists() {
        when(ratingDao.ratingExists(testPhoto, testUser)).thenReturn(true);

        boolean result = ratingService.hasRating(testUser, testPhoto);
        assertTrue(result);
    }

    @Test
    void hasRating_ShouldReturnFalseWhenRatingDoesNotExist() {
        when(ratingDao.ratingExists(testPhoto, testUser)).thenReturn(false);

        boolean result = ratingService.hasRating(testUser, testPhoto);
        assertFalse(result);
    }

    @Test
    void getRatingByPhoto_ShouldReturnListOfRatings() {
        List<Rating> expectedRatings = Arrays.asList(testRating);
        when(ratingDao.findByPhoto(testPhoto)).thenReturn(expectedRatings);

        List<Rating> result = ratingService.getRatingByPhoto(testPhoto);
        assertEquals(expectedRatings, result);
    }

    @Test
    void updatePhotoRating_ShouldCallPhotoDaoUpdate() {
        ratingService.updatePhotoRating(testPhoto);
        verify(photoDao).update(testPhoto);
    }

    @Test
    void userRatingExists_ShouldReturnRatingWhenExists() {
        when(ratingDao.findByPhotoAndUser(testPhoto, testUser)).thenReturn(testRating);

        Rating result = ratingService.userRatingExists(testUser, testPhoto);
        assertEquals(testRating, result);
    }

    @Test
    void getUserCount_ShouldReturnPhotoCountForUser() {
        when(photoDao.countByUser(testUser)).thenReturn(5L);

        long result = ratingService.getUserCount(testUser);
        assertEquals(5L, result);
    }

    @Test
    void updateUser_ShouldCallUserDaoUpdate() {
        ratingService.updateUser(testUser);
        verify(userDao).update(testUser);
    }

    @Test
    void deleteRating_ShouldCallRatingDaoDeleteById() {
        ratingService.deleteRating(testRating);
        verify(ratingDao).deleteById(testRating.getId());
    }

    @Test
    void getRatingByUserAndPhoto_ShouldReturnRating() {
        when(ratingDao.findByPhotoAndUser(testPhoto, testUser)).thenReturn(testRating);

        Rating result = ratingService.getRatingByUserAndPhoto(testUser, testPhoto);
        assertEquals(testRating, result);
    }

    @Test
    @Transactional
    void update_ShouldCallRatingDaoUpdate() {
        ratingService.update(testRating);
        verify(ratingDao).update(testRating);
    }

    @Test
    void getRating_ShouldReturnUserRatings() {
        List<Rating> expectedRatings = Arrays.asList(testRating);
        when(ratingDao.findUserRating(testUser)).thenReturn(expectedRatings);

        List<Rating> result = ratingService.getRating(testUser);
        assertEquals(expectedRatings, result);
    }

    @Test
    void getRatingsByUser_ShouldReturnRatingsForUserPhotos() {
        List<Rating> expectedRatings = Arrays.asList(testRating);
        when(ratingDao.findByPhotoOwner(testUser)).thenReturn(expectedRatings);

        List<Rating> result = ratingService.getRatingsByUser(testUser);
        assertEquals(expectedRatings, result);
    }

    @Test
    @Transactional
    void recalculateImageRating_ShouldCalculateCorrectAverageForNewRating() {
        // Setup existing ratings
        Rating existingRating1 = new Rating();
        existingRating1.setRating(3.0);
        Rating existingRating2 = new Rating();
        existingRating2.setRating(5.0);
        List<Rating> existingRatings = Arrays.asList(existingRating1, existingRating2);

        when(ratingDao.findByPhoto(testPhoto)).thenReturn(existingRatings);

        double newRating = 4.0;
        ratingService.recalculateImageRating(testPhoto, newRating);

        // (3.0 + 5.0 + 4.0) / 3 = 4.0
        assertEquals(4.0, testPhoto.getAveragePhotoRating());
        verify(photoDao).update(testPhoto);
    }

    @Test
    @Transactional
    void recalculateImageRating_ShouldHandleNoExistingRatings() {
        when(ratingDao.findByPhoto(testPhoto)).thenReturn(Collections.emptyList());

        double newRating = 4.0;
        ratingService.recalculateImageRating(testPhoto, newRating);

        assertEquals(4.0, testPhoto.getAveragePhotoRating());
        verify(photoDao).update(testPhoto);
    }

    @Test
    @Transactional
    void recalculateUserRating_ShouldCalculateCorrectAverageForUser() {
        // Setup photos with ratings
        Photo photo1 = new Photo();
        photo1.setAveragePhotoRating(3.0);
        Photo photo2 = new Photo();
        photo2.setAveragePhotoRating(5.0);
        Photo photo3 = new Photo();
        photo3.setAveragePhotoRating(0.0); // should be excluded
        List<Photo> userPhotos = Arrays.asList(photo1, photo2, photo3);

        when(photoDao.findByUser(testUser)).thenReturn(userPhotos);

        ratingService.recalculateUserRating(testUser);

        // (3.0 + 5.0) / 2 = 4.0
        assertEquals(4.0, testUser.getAverageRating());
        verify(userDao).update(testUser);
        verify(leaderboardService).updateLeaderBoard(testUser);
    }

    @Test
    @Transactional
    void recalculateUserRating_ShouldHandleNoRatedPhotos() {
        Photo unratedPhoto = new Photo();
        unratedPhoto.setAveragePhotoRating(0.0);
        List<Photo> userPhotos = Collections.singletonList(unratedPhoto);

        when(photoDao.findByUser(testUser)).thenReturn(userPhotos);

        ratingService.recalculateUserRating(testUser);

        assertEquals(0.0, testUser.getAverageRating());
        verify(userDao).update(testUser);
        verify(leaderboardService).updateLeaderBoard(testUser);
    }

    @Test
    @Transactional
    void recalculateUserRating_ShouldHandleNoPhotos() {
        when(photoDao.findByUser(testUser)).thenReturn(Collections.emptyList());

        ratingService.recalculateUserRating(testUser);

        assertEquals(0.0, testUser.getAverageRating());
        verify(userDao).update(testUser);
        verify(leaderboardService).updateLeaderBoard(testUser);
    }

    @Test
    @Transactional
    void adjustRatingsForReRating_ShouldRecalculateCorrectly() {
        // Setup existing ratings (excluding the one being re-rated)
        Rating otherRating1 = new Rating();
        otherRating1.setRating(3.0);
        Rating otherRating2 = new Rating();
        otherRating2.setRating(5.0);
        List<Rating> existingRatings = Arrays.asList(otherRating1, otherRating2);

        when(ratingDao.findByPhoto(testPhoto)).thenReturn(existingRatings);

        double oldRating = 4.0;
        double newRating = 2.0;

        ratingService.adjustRatingsForReRating(testPhoto, testUser, oldRating, newRating);

        // Average should be (3.0 + 5.0) / 2 = 4.0
        assertEquals(4.0, testPhoto.getAveragePhotoRating());
        verify(photoDao).update(testPhoto);
        verify(userDao).update(testUser);
        verify(leaderboardService).updateLeaderBoard(testUser);
    }

    @Test
    @Transactional
    void ratePhoto_ShouldSaveNewRatingWhenNoExistingRating() {
        when(ratingDao.ratingExists(testPhoto, testUser)).thenReturn(false);
        when(ratingDao.save(any(Rating.class))).thenReturn(true);

        ratingService.ratePhoto(testUser, testPhoto, 4.0);

        verify(ratingDao).save(any(Rating.class));
        verify(ratingDao, never()).update(any(Rating.class));
    }

    @Test
    @Transactional
    void ratePhoto_ShouldUpdateExistingRatingWhenRatingExists() {
        when(ratingDao.ratingExists(testPhoto, testUser)).thenReturn(true);
        when(ratingDao.findByPhotoAndUser(testPhoto, testUser)).thenReturn(testRating);

        ratingService.ratePhoto(testUser, testPhoto, 2.0);

        assertEquals(2.0, testRating.getRating());
        verify(ratingDao).update(testRating);
        verify(ratingDao, never()).save(any(Rating.class));
    }

    @Test
    @Transactional
    void ratePhoto_ShouldThrowExceptionForInvalidRatingTooLow() {
        assertThrows(IllegalArgumentException.class, () -> {
            ratingService.ratePhoto(testUser, testPhoto, 0.5);
        });
    }

    @Test
    @Transactional
    void ratePhoto_ShouldThrowExceptionForInvalidRatingTooHigh() {
        assertThrows(IllegalArgumentException.class, () -> {
            ratingService.ratePhoto(testUser, testPhoto, 5.5);
        });
    }

    @Test
    @Transactional
    void ratePhoto_ShouldThrowExceptionForNullParameters() {
        assertThrows(IllegalArgumentException.class, () -> {
            ratingService.ratePhoto(null, testPhoto, 3.0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ratingService.ratePhoto(testUser, null, 3.0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ratingService.ratePhoto(testUser, testPhoto, null);
        });
    }
}