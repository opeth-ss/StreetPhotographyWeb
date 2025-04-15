package com.example.dao;

import org.primefaces.model.FilterMeta;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface BaseDao<T, ID extends Serializable> {
    boolean save(T entity);
    T findById(ID id);
    List<T> findAll();
    boolean update(T entity);
    boolean deleteById(ID id);
    List<T> findPaginatedEntities(
            Map<String, FilterMeta> filters,
            Map<String, Object> exactMatchFilters,
            int first,
            int pageSize
    );
    int getTotalEntityCount(Map<String, FilterMeta> filters, Map<String, Object> exactMatchFilters);
    List<javax.persistence.criteria.Predicate> buildFilters(
            javax.persistence.criteria.CriteriaBuilder cb,
            javax.persistence.criteria.Root<T> root,
            Map<String, FilterMeta> filters
    );
}