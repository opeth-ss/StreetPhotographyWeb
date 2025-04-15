package com.example.dao;

import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.User;
import org.primefaces.model.FilterMeta;

import java.util.List;
import java.util.Map;

public interface RatingDao extends BaseDao<Rating, Long>{
    boolean save(Rating rating);
    Rating findById(Long id);
    List<Rating> findAll();
    boolean update(Rating rating);
    boolean deleteById(Long id);
    List<Rating> findPaginatedEntities(
            Map<String, FilterMeta> filters,
            Map<String, Object> exactMatchFilters,
            int first,
            int pageSize
    );
    int getTotalEntityCount(Map<String, FilterMeta> filters, Map<String, Object> exactMatchFilters);
    List<javax.persistence.criteria.Predicate> buildFilters(
            javax.persistence.criteria.CriteriaBuilder cb,
            javax.persistence.criteria.Root<Rating> root,
            Map<String, FilterMeta> filters
    );
    List<Rating> findByPhotoOwner(User user);
    List<Rating> findByPhoto(Photo photo);
    boolean updatePhotoAndUser(Photo photo, User user);
    long countByPhoto(Photo photo);
    Rating findByPhotoAndUser(Photo photo, User user);
    boolean ratingExists(Photo photo, User user);
    List<Rating> findUserRating(User user);
}
