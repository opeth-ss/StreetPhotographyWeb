package com.example.dao;

import com.example.model.Photo;
import com.example.model.User;

import java.util.List;

public interface PhotoDao extends BaseDao<Photo, Long> {
    List<Photo> findByUser(User user);
    List<Photo> findByUserId(Long userId);
    List<Photo> findByPinPoint(String pinPoint);
    long countByUser(User user);
    List<Photo> findRecentPhotos();
    List<Photo> searchPhotosList(String searchText);
    List<Photo> getAll();
}
