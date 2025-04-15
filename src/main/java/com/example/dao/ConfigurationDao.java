package com.example.dao;

import com.example.model.Configuration;
import org.primefaces.model.FilterMeta;

import java.util.List;
import java.util.Map;

public interface ConfigurationDao extends BaseDao<Configuration, Long>{
     boolean save(Configuration configuration);
     Configuration findById(Long id);
     List<Configuration> findAll();
     boolean update(Configuration configuration);
     boolean deleteById(Long id);
     List<Configuration> findPaginatedEntities(
             Map<String, FilterMeta> filters,
             Map<String, Object> exactMatchFilters,
             int first,
             int pageSize
     );
     int getTotalEntityCount(Map<String, FilterMeta> filters, Map<String, Object> exactMatchFilters);
     List<javax.persistence.criteria.Predicate> buildFilters(
             javax.persistence.criteria.CriteriaBuilder cb,
             javax.persistence.criteria.Root<Configuration> root,
             Map<String, FilterMeta> filters
     );
     Configuration getConfiguration();
}
