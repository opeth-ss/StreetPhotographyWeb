package com.example.dao.impl;

import com.example.dao.UserDao;
import com.example.model.User;
import org.primefaces.model.FilterMeta;

import javax.ejb.Stateless;
import javax.persistence.*;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;

@Stateless
public class UserDaoImpl extends BaseDaoImpl<User, Long> implements UserDao {

    @PersistenceContext(unitName = "StreetPhotography") // This injects the EntityManager
    private EntityManager em;

    public UserDaoImpl(){
        super(User.class);
    }

    @Override
    public Optional<User> findByUserName(String userName) {
        try {
            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.userName = :userName", User.class);
            query.setParameter("userName", userName);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try {
            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.email = :email", User.class);
            query.setParameter("email", email);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<User> findAll() {
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u", User.class);
        return query.getResultList();
    }

    @Override
    public List<User> findPaginatedEntities(
            Map<String, FilterMeta> filters,
            Map<String, Object> exactMatchFilters,
            int first,
            int pageSize,
            String sortField,
            String sortOrder,
            String globalFilter
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> root = cq.from(User.class);

        // Apply filters
        List<Predicate> predicates = new ArrayList<>();
        predicates.addAll(buildExactFilters(cb, root, exactMatchFilters));

        // Apply global filter (search across username, email, role)
        if (globalFilter != null && !globalFilter.trim().isEmpty()) {
            String searchPattern = "%" + globalFilter.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("userName")), searchPattern),
                    cb.like(cb.lower(root.get("email")), searchPattern),
                    cb.like(cb.lower(root.get("role")), searchPattern)
            ));
        }

        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        // Apply sorting
        if (sortField != null && sortOrder != null) {
            if ("ASC".equalsIgnoreCase(sortOrder)) {
                cq.orderBy(cb.asc(root.get(sortField)));
            } else if ("DESC".equalsIgnoreCase(sortOrder)) {
                cq.orderBy(cb.desc(root.get(sortField)));
            }
        }

        TypedQuery<User> query = em.createQuery(cq);
        query.setFirstResult(first);
        query.setMaxResults(pageSize);
        return query.getResultList();
    }

    @Override
    public int getTotalEntityCount(Map<String, FilterMeta> filters, Map<String, Object> exactMatchFilters, String globalFilter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<User> root = countQuery.from(User.class);
        countQuery.select(cb.count(root));

        // Apply filters
        List<Predicate> predicates = new ArrayList<>();
        predicates.addAll(buildExactFilters(cb, root, exactMatchFilters));

        // Apply global filter
        if (globalFilter != null && !globalFilter.trim().isEmpty()) {
            String searchPattern = "%" + globalFilter.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("userName")), searchPattern),
                    cb.like(cb.lower(root.get("email")), searchPattern),
                    cb.like(cb.lower(root.get("role")), searchPattern)
            ));
        }

        if (!predicates.isEmpty()) {
            countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        return Optional.ofNullable(em.createQuery(countQuery).getSingleResult())
                .map(Long::intValue)
                .orElse(0);
    }

    @Override
    public List<Predicate> buildFilters(CriteriaBuilder cb, Root<User> root, Map<String, FilterMeta> filters) {
        // Not used in this context, as we rely on exactMatchFilters
        return Collections.emptyList();
    }

    @Override
    public List<Predicate> buildExactFilters(CriteriaBuilder cb, Root<User> root, Map<String, Object> filters) {
        if (filters == null) {
            return Collections.emptyList();
        }
        return filters.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getKey().equals("global"))
                .map(entry -> {
                    try {
                        Path<?> path = root.get(entry.getKey());
                        if (entry.getKey().equals("id")) {
                            // Handle ID as Long
                            return cb.equal(path, Long.valueOf(entry.getValue().toString()));
                        } else {
                            // Handle other fields as strings with LIKE
                            return cb.like(
                                    cb.lower(path.as(String.class)),
                                    "%" + entry.getValue().toString().toLowerCase() + "%"
                            );
                        }
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Path<?> getPath(Root<User> root, String fieldPath) {
        Path<?> path = root;
        for (String part : fieldPath.split("\\.")) {
            path = path.get(part);
        }
        return path;
    }
}
