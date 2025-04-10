package com.example.dao;

import com.example.model.Photo;
import com.example.model.User;

import java.util.List;

public interface PhotoDao extends BaseDao<Photo, Long> {
    List<Photo> findByUser(User user);
    List<Photo> findByUserId(Long userId);
    List<Photo> findByPinPoint(String pinPoint);
    long countByUser(User user);
    List<Photo> findRecentPhotos(Integer minPhotos, Double minRating, int first, int pageSize);
    List<Photo> searchPhotosList(String searchText);
    List<Photo> getAll();
    List<Photo> getPhotosPaginated(int first, int pageSize);
    int getAllCount();
    List<String> getAllPinPoints();
    List<Photo> findFilteredPhotos(String filterLocation, List<String> filterTags, Double filterMinRating,
                                   String searchText, User currentUser, int first, int pageSize);
    int getFilteredCount(String filterLocation, List<String> filterTags, Double filterMinRating,
                         String searchText, User currentUser);
}
