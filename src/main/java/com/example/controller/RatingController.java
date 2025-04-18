package com.example.controller;

import com.example.model.Photo;
import com.example.model.User;
import com.example.services.RatingService;
import com.example.utils.MessageHandler;

import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@Named("ratingController")
@ViewScoped
public class RatingController implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private RatingService ratingService;

    @Inject
    private UserController userController;

    @Inject
    private MessageHandler messageHandler;

    public void addRating(User user, Photo photo, Double ratingN) {
        try {
            ratingService.ratePhoto(user, photo, ratingN);
            messageHandler.addInfoMessage("Rating Saved", "Your rating has been saved", ":growl");
        } catch (IllegalArgumentException e) {
            messageHandler.addErrorMessage("Error", e.getMessage(), ":growl");
        }
    }

    public void updateExistingRating(Photo photo, Double reRatingValue) {
        try {
            User currentUser = userController.getUser();
            ratingService.ratePhoto(currentUser, photo, reRatingValue);
            messageHandler.addInfoMessage("Success", "Rating updated successfully", ":growl");
        } catch (IllegalArgumentException e) {
            messageHandler.addErrorMessage("Error", e.getMessage(), ":growl");
        }
    }
}