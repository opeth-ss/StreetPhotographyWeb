package com.example.dao;

import com.example.model.User;
import org.primefaces.model.FilterMeta;

import javax.faces.bean.ApplicationScoped;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Named
@ApplicationScoped
public interface UserDao extends BaseDao<User, Long>{
    boolean save(User user);
    User findById(Long id);
    List<User> findAll();
    boolean update(User user);
    boolean deleteById(Long id);
    List<User> findPaginatedEntities(
            Map<String, FilterMeta> filters,
            Map<String, Object> exactMatchFilters,
            int first,
            int pageSize
    );
    int getTotalEntityCount(Map<String, FilterMeta> filters, Map<String, Object> exactMatchFilters);

    List<User> findPaginatedEntities(
            Map<String, FilterMeta> filters,
            Map<String, Object> exactMatchFilters,
            int first,
            int pageSize,
            String sortField,
            String sortOrder,
            String filter
    );

    int getTotalEntityCount(Map<String, FilterMeta> filters, Map<String, Object> exactMatchFilters, String filter);

    List<javax.persistence.criteria.Predicate> buildFilters(
            javax.persistence.criteria.CriteriaBuilder cb,
            javax.persistence.criteria.Root<User> root,
            Map<String, FilterMeta> filters
    );
    Optional<User> findByUserName(String userName);
    Optional<User> findByEmail(String email);
}
