package com.example.controller;

import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.User;
import com.example.services.LeaderboardService;
import com.example.services.RatingService;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
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

    // Store the current rating attempt details for potential re-rating
    private Double pendingRating;
    private Photo pendingPhoto;
    private User pendingUser;

    public void addRating(User user, Photo photo, Double ratingN) {
        if (!ratingService.hasRating(user, photo)) {
            saveNewRating(user, photo, ratingN);
        } else {
            // Store the details for potential re-rating
            this.pendingRating = ratingN;
            this.pendingPhoto = photo;
            this.pendingUser = user;

            // Ask user if they want to re-rate
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Rating exists",
                            "You've already rated this image. Would you like to update your rating?"));
        }
    }

    public void confirmReRating() {
        if (pendingUser != null && pendingPhoto != null && pendingRating != null) {
            // Remove the old rating first
            Rating oldRating = ratingService.getRatingByUserAndPhoto(pendingUser, pendingPhoto);
            if (oldRating != null) {
                // Adjust the photo and user ratings by removing the old rating
                adjustRatingsForReRating(pendingPhoto, pendingUser, oldRating.getRating());

                // Delete the old rating
                ratingService.deleteRating(oldRating);

                // Save the new rating
                saveNewRating(pendingUser, pendingPhoto, pendingRating);
            }
        }

        // Reset pending rating fields
        resetPendingRating();
    }

    public void cancelReRating() {
        resetPendingRating();
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Cancelled", "Rating remains unchanged"));
    }

    private void resetPendingRating() {
        this.pendingRating = null;
        this.pendingPhoto = null;
        this.pendingUser = null;
    }

    private void saveNewRating(User user, Photo photo, Double ratingN) {
        Rating rating = new Rating();
        rating.setRating(ratingN);
        rating.setUser(user);
        rating.setPhoto(photo);

        if (ratingService.save(rating)) {
            recalculateImageRating(photo, ratingN);
            recalculateUserRating(photo.getUser(), ratingN);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Rating Saved", "Your rating has been saved"));
        }
    }

    private void adjustRatingsForReRating(Photo photo, User user, Double oldRating) {
        // Adjust photo rating by removing the old rating
        List<Rating> ratings = ratingService.getRatingByPhoto(photo);
        int total = ratings.size();
        if (total > 0) {
            double totalRatings = 0.0;
            for (Rating rate : ratings) {
                totalRatings += rate.getRating();
            }
            // Remove the old rating from the average
            double newRating = (totalRatings - oldRating) / (total - 1);
            photo.setAveragePhotoRating(newRating);
            ratingService.updatePhotoRating(photo);
        }

        // Adjust user rating by removing the old rating
        reduceUserRating(user, oldRating);
    }

    public void recalculateImageRating(Photo photo, Double ratingN) {
        List<Rating> ratings = ratingService.getRatingByPhoto(photo);
        int total = ratings.size();
        double totalRatings = 0.0;
        for (Rating rate : ratings) {
            totalRatings += rate.getRating();
        }
        double newRating = (totalRatings + ratingN) / (total + 1);

        photo.setAveragePhotoRating(newRating);
        ratingService.updatePhotoRating(photo);
    }

    public void recalculateUserRating(User user, Double ratingN) {
        long userCount = ratingService.getUserCount(user);
        double newAverageUserRating = ((user.getAverageRating() * (userCount - 1)) + ratingN) / userCount;
        user.setAverageRating(newAverageUserRating);
        ratingService.updateUser(user);
        leaderboardService.updateLeaderBoard(user);
    }

    public void reduceUserRating(User user, Double removedRating) {
        long userCount = ratingService.getUserCount(user);

        if (userCount <= 1) {
            user.setAverageRating(0.0);
        } else {
            double newAverageUserRating = ((user.getAverageRating() * userCount) - removedRating) / (userCount - 1);
            user.setAverageRating(newAverageUserRating);
        }
        ratingService.updateUser(user);
    }

    // Getters for the pending rating fields (needed for the UI)
    public Double getPendingRating() {
        return pendingRating;
    }

    public boolean isReRatingPending() {
        return pendingRating != null;
    }
}