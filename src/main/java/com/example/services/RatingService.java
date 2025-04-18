package com.example.services;

import com.example.dao.PhotoDao;
import com.example.dao.RatingDao;
import com.example.dao.UserDao;
import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class RatingService {

    @Inject
    private RatingDao ratingDao;

    @Inject
    private PhotoDao photoDao;

    @Inject
    private UserDao userDao;

    @Inject
    private LeaderboardService leaderboardService;

    @Transactional
    public boolean save(Rating rating) {
        return ratingDao.save(rating);
    }

    public boolean hasRating(User user, Photo photo) {
        return ratingDao.ratingExists(photo, user);
    }

    public List<Rating> getRatingByPhoto(Photo photo) {
        return ratingDao.findByPhoto(photo);
    }

    public void updatePhotoRating(Photo photo) {
        photoDao.update(photo);
    }

    public Rating userRatingExists(User user, Photo photo) {
        return ratingDao.findByPhotoAndUser(photo, user);
    }

    public long getUserCount(User user) {
        return photoDao.countByUser(user);
    }

    public void updateUser(User user) {
        userDao.update(user);
    }

    public void deleteRating(Rating oldRating) {
        ratingDao.deleteById(oldRating.getId());
    }

    public Rating getRatingByUserAndPhoto(User user, Photo photo) {
        return ratingDao.findByPhotoAndUser(photo, user);
    }

    @Transactional
    public void update(Rating existingRating) {
        ratingDao.update(existingRating);
    }

    public List<Rating> getRating(User user) {
        return ratingDao.findUserRating(user);
    }

    public List<Rating> getRatingsByUser(User user) {
        return ratingDao.findByPhotoOwner(user);
    }

    @Transactional
    public void recalculateImageRating(Photo photo, Double ratingN) {
        List<Rating> ratings = getRatingByPhoto(photo);
        int total = ratings.size();
        double totalRatings = ratings.stream().mapToDouble(Rating::getRating).sum();
        double newRating = total > 0 ? (totalRatings + ratingN) / (total + 1) : ratingN;

        photo.setAveragePhotoRating(newRating);
        photoDao.update(photo);
    }

    @Transactional
    public void recalculateUserRating(User user) {
        List<Photo> userPhotos = photoDao.findByUser(user);
        if (userPhotos == null || userPhotos.isEmpty()) {
            user.setAverageRating(0.0);
        } else {
            double totalRating = 0.0;
            int ratedPhotoCount = 0;
            for (Photo photo : userPhotos) {
                double avgRating = photo.getAveragePhotoRating();
                if (avgRating > 0.0) {
                    totalRating += avgRating;
                    ratedPhotoCount++;
                }
            }
            double newAverageUserRating = ratedPhotoCount > 0 ? totalRating / ratedPhotoCount : 0.0;
            user.setAverageRating(newAverageUserRating);
        }
        userDao.update(user);
        leaderboardService.updateLeaderBoard(user);
    }

    @Transactional
    public void adjustRatingsForReRating(Photo photo, User user, Double oldRating, Double newRating) {
        List<Rating> ratings = getRatingByPhoto(photo);
        int total = ratings.size();

        if (total > 0) {
            double totalRatings = ratings.stream().mapToDouble(Rating::getRating).sum();
            double adjustedRating = totalRatings / total;
            photo.setAveragePhotoRating(adjustedRating);
        } else {
            photo.setAveragePhotoRating(0.0);
        }

        photoDao.update(photo);
        recalculateUserRating(photo.getUser());
    }

    @Transactional
    public void ratePhoto(User user, Photo photo, Double ratingN) {
        if (user == null || photo == null || ratingN == null) {
            throw new IllegalArgumentException("Invalid rating data");
        }
        if (ratingN < 1.0 || ratingN > 5.0) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        if (!hasRating(user, photo)) {
            Rating rating = new Rating();
            rating.setRating(ratingN);
            rating.setUser(user);
            rating.setPhoto(photo);
            save(rating);
            recalculateImageRating(photo, ratingN);
            recalculateUserRating(photo.getUser());
        } else {
            Rating existingRating = getRatingByUserAndPhoto(user, photo);
            if (existingRating != null) {
                Double oldRating = existingRating.getRating();
                existingRating.setRating(ratingN);
                update(existingRating);
                adjustRatingsForReRating(photo, user, oldRating, ratingN);
            }
        }
    }
}