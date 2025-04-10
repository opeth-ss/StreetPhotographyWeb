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

    @Transactional
    public boolean save(Rating rating){
        return ratingDao.save(rating);
    }

    public boolean hasRating(User user, Photo photo){
        return ratingDao.ratingExists(photo, user);
    }

    public List<Rating> getRatingByPhoto(Photo photo){
       return ratingDao.findByPhoto(photo);
    }

    public void updatePhotoRating(Photo photo) {
        photoDao.update(photo);
    }

    public Rating userRatingExists(User user , Photo photo){ return ratingDao.findByPhotoAndUser(photo, user);}

    public long getUserCount(User user) {
        return photoDao.countByUser(user);

    }

    public void updateUser(User user) {
        userDao.update(user);
    }

    public void deleteRating(Rating oldRating) {
        ratingDao.deleteById(oldRating.getId());
    }

    public Rating getRatingByUserAndPhoto(User pendingUser, Photo pendingPhoto) {
        return ratingDao.findByPhotoAndUser(pendingPhoto, pendingUser);
    }

    @Transactional
    public void update(Rating existingRating) {
        ratingDao.update(existingRating);
    }

    public List<Rating> getRating(User user){
        return ratingDao.findUserRating(user);
    }

    public List<Rating> getRatingsByUser(User user) {
        return ratingDao.findByPhotoOwner(user);
    }
}
