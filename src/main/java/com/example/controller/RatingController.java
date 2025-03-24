package com.example.controller;

import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.User;
import com.example.services.RatingService;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named("ratingController")
@SessionScoped
public class RatingController  implements Serializable {
    private static final long serialVersionUID = 1L;
    private Rating rating = new Rating();

    @Inject
    private RatingService ratingService;

    public void addRating(User user, Photo photo, Double ratingN){
        if(ratingService.hasRating(user, photo)){
            rating.setRating(ratingN);
            rating.setUser(user);
            rating.setPhoto(photo);
            recalculateImageRating(photo, ratingN);
            recalculateUserRating(user);
        }
        else{
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Rating exists", "You cannot rate an image twice"));
        }
    }

    public void recalculateImageRating(Photo photo, Double ratingN) {
        List<Rating> ratings = ratingService.getRatingByPhoto(photo);
        int total = ratings.size();
        double totalRatings = 0.0;
        for (Rating rate : ratings) {
            totalRatings += rate.getRating();
        }
        double newRating = (totalRatings+ ratingN)/(total+1);

        photo.setAveragePhotoRating(newRating);
        ratingService.updatePhotoRating(photo);
    }

    public void recalculateUserRating(User user) {
        List<Rating> userRatings = ratingService.getRatingsByUser(user);
        int totalRatingsCount = userRatings.size();
        double totalRatings = 0.0;

        for (Rating userRating : userRatings) {
            totalRatings += userRating.getRating(); // Add each rating the user has given
        }

        if (totalRatingsCount > 0) {
            double userAverageRating = totalRatings / totalRatingsCount; // Calculate the average
            user.setAverageRating(userAverageRating); // Set the user's average rating
            ratingService.updateUserRating(user); // Update user rating in the database
        }
    }
}
