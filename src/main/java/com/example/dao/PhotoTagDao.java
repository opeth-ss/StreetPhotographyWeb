package com.example.dao;

import com.example.model.Photo;
import com.example.model.PhotoTag;
import org.primefaces.model.FilterMeta;

import java.util.List;
import java.util.Map;

public interface PhotoTagDao extends BaseDao<PhotoTag, Long> {
    boolean save(PhotoTag photoTag);
    PhotoTag findById(Long id);
    List<PhotoTag> findAll();
    boolean update(PhotoTag photoTag);
    boolean deleteById(Long id);
    List<PhotoTag> findPaginatedEntities(
            Map<String, FilterMeta> filters,
            Map<String, Object> exactMatchFilters,
            int first,
            int pageSize
    );
    int getTotalEntityCount(Map<String, FilterMeta> filters, Map<String, Object> exactMatchFilters);
    List<javax.persistence.criteria.Predicate> buildFilters(
            javax.persistence.criteria.CriteriaBuilder cb,
            javax.persistence.criteria.Root<PhotoTag> root,
            Map<String, FilterMeta> filters
    );
    List<PhotoTag> findByPhotoId(Long photoId);
    List<PhotoTag> findByTagId(Long tagId);
}
