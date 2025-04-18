package com.example.controller;

import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.User;
import com.example.services.LeaderboardService;
import com.example.services.PhotoService;
import com.example.services.RatingService;
import com.example.utils.MessageHandler;

import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named("ratingController")
@ViewScoped
public class RatingController implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private RatingService ratingService;

    @Inject
    private LeaderboardService leaderboardService;

    @Inject
    private PhotoController photoController;

    @Inject
    private UserController userController;

    @Inject
    private PhotoService photoService;

    @Inject
    private MessageHandler messageHandler;

    public void addRating(User user, Photo photo, Double ratingN) {
        if (user == null || photo == null || ratingN == null) {
            messageHandler.addErrorMessage("Error", "Invalid rating data", ":growl");
            return;
        }

        if (!ratingService.hasRating(user, photo)) {
            saveNewRating(user, photo, ratingN);
        } else {
            Rating existingRating = ratingService.getRatingByUserAndPhoto(user, photo);
            if (existingRating != null) {
                Double oldRating = existingRating.getRating();
                existingRating.setRating(ratingN);
                ratingService.update(existingRating);

                adjustRatingsForReRating(photo, user, oldRating, ratingN);

                messageHandler.addInfoMessage("Rating Updated", "Your rating has been updated", ":growl");
            }
        }
    }

    private void saveNewRating(User user, Photo photo, Double ratingN) {
        Rating rating = new Rating();
        rating.setRating(ratingN);
        rating.setUser(user);
        rating.setPhoto(photo);

        if (ratingService.save(rating)) {
            recalculateImageRating(photo, ratingN);
            recalculateUserRating(photo.getUser());
            messageHandler.addInfoMessage("Rating Saved", "Your rating has been saved", ":growl");
        }
    }

    public void updateExistingRating(Photo photo, Double reRatingValue) {
        if (photo == null || reRatingValue == null) {
            messageHandler.addErrorMessage("Error", "Invalid rating data", ":growl");
            return;
        }

        User currentUser = userController.getUser();
        Rating existingRating = ratingService.getRatingByUserAndPhoto(currentUser, photo);

        if (existingRating != null) {
            Double oldRating = existingRating.getRating();
            existingRating.setRating(reRatingValue);
            ratingService.update(existingRating);

            adjustRatingsForReRating(photo, currentUser, oldRating, reRatingValue);

            messageHandler.addInfoMessage("Success", "Rating updated successfully", ":growl");
        }
    }

    private void adjustRatingsForReRating(Photo photo, User user, Double oldRating, Double newRating) {
        List<Rating> ratings = ratingService.getRatingByPhoto(photo);
        int total = ratings.size();

        if (total > 0) {
            double totalRatings = ratings.stream().mapToDouble(Rating::getRating).sum();
            double adjustedRating = totalRatings / total; // Use updated ratings directly
            photo.setAveragePhotoRating(adjustedRating);
        } else {
            photo.setAveragePhotoRating(0.0); // Handle no ratings case
        }

        ratingService.updatePhotoRating(photo);
        recalculateUserRating(photo.getUser());
        leaderboardService.updateLeaderBoard(photo.getUser());
        photoController.reSetRating();
    }

    public void recalculateImageRating(Photo photo, Double ratingN) {
        List<Rating> ratings = ratingService.getRatingByPhoto(photo);
        int total = ratings.size();
        double totalRatings = ratings.stream().mapToDouble(Rating::getRating).sum();
        double newRating = total > 0 ? (totalRatings + ratingN) / (total + 1) : ratingN;

        photo.setAveragePhotoRating(newRating);
        ratingService.updatePhotoRating(photo);
    }

    public void recalculateUserRating(User user) {
        List<Photo> userPhotos = photoService.getPhotosByUser(user);
        if (userPhotos == null || userPhotos.isEmpty()) {
            user.setAverageRating(0.0);
        } else {
            double totalRating = 0.0;
            int ratedPhotoCount = 0;
            for (Photo photo : userPhotos) {
                double avgRating = photo.getAveragePhotoRating();
                if (avgRating > 0.0) { // Treat 0.0 as "unrated"
                    totalRating += avgRating;
                    ratedPhotoCount++;
                }
            }
            double newAverageUserRating = ratedPhotoCount > 0 ? totalRating / ratedPhotoCount : 0.0;
            user.setAverageRating(newAverageUserRating);
        }
        ratingService.updateUser(user);
        leaderboardService.updateLeaderBoard(user);
    }
}