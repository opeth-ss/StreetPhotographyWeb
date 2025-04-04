package com.example.controller;

import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.User;
import com.example.services.LeaderboardService;
import com.example.services.PhotoService;
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

    @Inject
    private PhotoController photoController;

    @Inject
    private UserController userController;

    @Inject
    private PhotoService photoService;

    public void addRating(User user, Photo photo, Double ratingN) {
        if (!ratingService.hasRating(user, photo)) {
            saveNewRating(user, photo, ratingN);
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Rating exists",
                            "You've already rated this image. Would you like to update your rating?"));
        }
    }

    private void saveNewRating(User user, Photo photo, Double ratingN) {
        Rating rating = new Rating();
        rating.setRating(ratingN);
        rating.setUser(user);
        rating.setPhoto(photo);

        if (ratingService.save(rating)) {
            recalculateImageRating(photo, ratingN);
            recalculateUserRating(photo.getUser()); // Update the photo owner's rating
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Rating Saved", "Your rating has been saved"));
        }
    }

    public void updateExistingRating(Photo photo, Double reRatingValue) {
        if (photo == null || reRatingValue == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid rating data"));
            return;
        }

        User currentUser = userController.getUser();
        Rating existingRating = ratingService.getRatingByUserAndPhoto(currentUser, photo);

        if (existingRating != null) {
            Double oldRating = existingRating.getRating();
            existingRating.setRating(reRatingValue);
            ratingService.update(existingRating);

            adjustRatingsForReRating(photo, currentUser, oldRating, reRatingValue);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Rating updated successfully"));
        }
    }

    private void adjustRatingsForReRating(Photo photo, User user, Double oldRating, Double newRating) {
        List<Rating> ratings = ratingService.getRatingByPhoto(photo);
        int total = ratings.size();

        if (total > 1) { // Prevent division by zero
            double totalRatings = ratings.stream().mapToDouble(Rating::getRating).sum();
            double adjustedRating = (totalRatings - oldRating + newRating) / total; // Update with new value
            photo.setAveragePhotoRating(adjustedRating);
        } else {
            photo.setAveragePhotoRating(newRating);
        }

        ratingService.updatePhotoRating(photo);
        recalculateUserRating(photo.getUser()); // Update the photo owner's rating
        leaderboardService.updateLeaderBoard(photo.getUser());
        photoController.reSetRating();
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