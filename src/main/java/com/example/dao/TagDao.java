package com.example.dao;

import com.example.model.Tag;
import org.primefaces.model.FilterMeta;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TagDao extends BaseDao<Tag, Long> {
    boolean save(Tag tag);
    Tag findById(Long id);
    List<Tag> findAll();
    boolean update(Tag tag);
    boolean deleteById(Long id);
    List<Tag> findPaginatedEntities(
            Map<String, FilterMeta> filters,
            Map<String, Object> exactMatchFilters,
            int first,
            int pageSize
    );
    int getTotalEntityCount(Map<String, FilterMeta> filters, Map<String, Object> exactMatchFilters);
    List<javax.persistence.criteria.Predicate> buildFilters(
            javax.persistence.criteria.CriteriaBuilder cb,
            javax.persistence.criteria.Root<Tag> root,
            Map<String, FilterMeta> filters
    );
    Tag findByName(String name);
    List<Tag> getAll(String like);
    List<Tag> getAll();
}
