package com.example.services;

import com.example.dao.PhotoDao;
import com.example.dao.RatingDao;
import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class RatingService {

    @Inject
    private RatingDao ratingDao;

    @Inject
    private PhotoDao photoDao;

    public boolean hasRating(User user, Photo photo){
        return ratingDao.ratingExists(photo, user);
    }

    public List<Rating> getRatingByPhoto(Photo photo){
       return ratingDao.findByPhoto(photo);
    }

    public void updatePhotoRating(Photo photo) {
        photoDao.update(photo);
    }

    public List<Rating> getRatingsByUser(User user) {
        return null;
    }

    public void updateUserRating(User user) {

    }
}
