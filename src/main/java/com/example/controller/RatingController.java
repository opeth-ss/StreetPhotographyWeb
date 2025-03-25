package com.example.controller;

import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.User;
import com.example.services.LeaderboardService;
import com.example.services.RatingService;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@Named("ratingController")
@SessionScoped
public class RatingController  implements Serializable {
    private static final long serialVersionUID = 1L;
    private Rating rating = new Rating();

    @Inject
    private RatingService ratingService;

    @Inject
    private LeaderboardService leaderboardService;

    public void addRating(User user, Photo photo, Double ratingN){
        if(!ratingService.hasRating(user, photo)){
            rating.setRating(ratingN);
            rating.setUser(user);
            rating.setPhoto(photo);
            if(ratingService.save(rating)){
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Rating Saved", "Your rating has been saved"));
            }
            recalculateImageRating(photo, ratingN);
            recalculateUserRating(photo.getUser(), ratingN);
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

    public void recalculateUserRating(User user, Double ratingN) {
        long userCount = ratingService.getUserCount(user);
        double newAverageUserRating = ((user.getAverageRating() * (userCount - 1)) + ratingN) / userCount;
        user.setAverageRating(newAverageUserRating);
        ratingService.updateUser(user);
//        leaderboardService.updateLeaderBoard(user);
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

}
