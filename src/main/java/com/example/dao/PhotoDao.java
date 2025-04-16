package com.example.dao;

import com.example.model.Photo;
import com.example.model.User;
import org.primefaces.model.FilterMeta;

import java.util.List;
import java.util.Map;

public interface PhotoDao extends BaseDao<Photo, Long> {
    boolean save(Photo photo);
    Photo findById(Long id);
    List<Photo> findAll();
    boolean update(Photo photo);
    boolean deleteById(Long id);
    List<Photo> findPaginatedEntities(
            Map<String, FilterMeta> filters,
            Map<String, Object> exactMatchFilters,
            int first,
            int pageSize
    );
    int getTotalEntityCount(Map<String, FilterMeta> filters, Map<String, Object> exactMatchFilters);
    List<javax.persistence.criteria.Predicate> buildFilters(
            javax.persistence.criteria.CriteriaBuilder cb,
            javax.persistence.criteria.Root<Photo> root,
            Map<String, FilterMeta> filters
    );
    List<Photo> findByUser(User user);
    List<Photo> findByUserId(Long userId);
    List<Photo> findByPinPoint(String pinPoint);
    long countByUser(User user);
    List<Photo> getAll();
    List<Photo> getPhotosPaginated(int first, int pageSize);
    int getAllCount();
    List<String> getAllPinPoints();
    List<Photo> findFilteredPhotos(Map<String, Object> filters, int first, int pageSize);
    int getFilteredCount(Map<String, Object> filters);
}
