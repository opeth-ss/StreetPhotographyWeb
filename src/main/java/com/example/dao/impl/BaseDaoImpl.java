package com.example.dao.impl;

import com.example.dao.BaseDao;
import org.primefaces.model.FilterMeta;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseDaoImpl<T, ID extends Serializable> implements BaseDao<T, ID> {
    @PersistenceContext(unitName = "StreetPhotography") // This injects the EntityManager
    private EntityManager em;

    private final Class<T> entityClass;

    // Constructor injection for the entity class type
    public BaseDaoImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public boolean save(T entity) {
        try {
            em.persist(entity);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(T entity) {
        try {
            em.merge(entity);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteById(ID id) {
        try {
            T entity = em.find(entityClass, id);
            if (entity != null) {
                em.remove(entity);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public T findById(ID id) {
        try {
            return em.find(entityClass, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<T> findAll() {
        try {
            TypedQuery<T> query = em.createQuery(
                    "SELECT e FROM " + entityClass.getSimpleName() + " e",
                    entityClass
            );
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<T> findPaginatedEntities(
            Map<String, FilterMeta> filters,
            Map<String, Object> exactMatchFilters,
            int first,
            int pageSize
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityClass);
        Root<T> root = cq.from(entityClass);

        List<Predicate> allPredicates = new ArrayList<>();
        allPredicates.addAll(buildFilters(cb, root, filters));
        allPredicates.addAll(buildExactFilters(cb, root, exactMatchFilters));

        if (!allPredicates.isEmpty()) {
            cq.where(cb.and(allPredicates.toArray(new Predicate[0])));
        }

        TypedQuery<T> query = em.createQuery(cq);
        query.setFirstResult(first);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }

    @Override
    public int getTotalEntityCount(Map<String, FilterMeta> filters, Map<String, Object> exactMatchFilters) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<T> root = countQuery.from(entityClass);
        countQuery.select(cb.count(root));

        List<Predicate> allPredicates = new ArrayList<>();
        allPredicates.addAll(buildFilters(cb, root, filters));
        allPredicates.addAll(buildExactFilters(cb, root, exactMatchFilters));

        if (!allPredicates.isEmpty()) {
            countQuery.where(cb.and(allPredicates.toArray(new Predicate[0])));
        }

        TypedQuery<Long> query = em.createQuery(countQuery);
        Long result = query.getSingleResult();
        return result != null ? result.intValue() : 0;
    }

    @Override
    public List<Predicate> buildFilters(CriteriaBuilder cb, Root<T> root, Map<String, FilterMeta> filters) {
        List<Predicate> predicates = new ArrayList<>();
        if (filters != null) {
            for (Map.Entry<String, FilterMeta> entry : filters.entrySet()) {
                String field = entry.getKey();
                Object filterValue = entry.getValue().getFilterValue();
                if (filterValue != null && !filterValue.toString().trim().isEmpty()) {
                    try {
                        javax.persistence.criteria.Path<?> path = getPath(root, field);
                        predicates.add(cb.like(
                                cb.lower(path.as(String.class)),
                                "%" + filterValue.toString().toLowerCase() + "%"
                        ));
                    } catch (IllegalArgumentException e) {
                        // Skip invalid attributes
                    }
                }
            }
        }
        return predicates;
    }

    public List<Predicate> buildExactFilters(CriteriaBuilder cb, Root<T> root, Map<String, Object> filters) {
        List<Predicate> predicates = new ArrayList<>();
        if (filters != null) {
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                String field = entry.getKey();
                Object filterValue = entry.getValue();
                // Skip non-attribute filters like currentUser
                if (filterValue != null && !field.equals("currentUser")) {
                    try {
                        javax.persistence.criteria.Path<?> path = getPath(root, field);
                        predicates.add(cb.equal(path, filterValue));
                    } catch (IllegalArgumentException e) {
                        // Skip invalid attributes
                    }
                }
            }
        }
        return predicates;
    }

    private javax.persistence.criteria.Path<?> getPath(Root<T> root, String fieldPath) {
        javax.persistence.criteria.Path<?> path = root;
        for (String part : fieldPath.split("\\.")) {
            path = path.get(part);
        }
        return path;
    }
}
