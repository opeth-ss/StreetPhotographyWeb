package com.example.dao;

import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.User;

import java.util.List;

public interface RatingDao extends BaseDao<Rating, Long>{
    List<Rating> findByPhotoOwner(User user);
    List<Rating> findByPhoto(Photo photo);
    boolean updatePhotoAndUser(Photo photo, User user);
    long countByPhoto(Photo photo);
    Rating findByPhotoAndUser(Photo photo, User user);
    boolean ratingExists(Photo photo, User user);

    List<Rating> findUserRating(User user);
}
