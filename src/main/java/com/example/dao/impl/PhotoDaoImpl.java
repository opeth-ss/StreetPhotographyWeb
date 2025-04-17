package com.example.dao.impl;

import com.example.dao.PhotoDao;
import com.example.model.Photo;
import com.example.model.PhotoTag;
import com.example.model.Tag;
import com.example.model.User;
import org.primefaces.model.FilterMeta;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Stateless
public class PhotoDaoImpl extends BaseDaoImpl<Photo, Long> implements PhotoDao {
    @PersistenceContext(unitName = "StreetPhotography")
    private EntityManager em;

    public PhotoDaoImpl() {
        super(Photo.class);
    }

    @Override
    public List<Photo> findByUser(User user) {
        TypedQuery<Photo> query = em.createQuery(
                "SELECT p FROM Photo p WHERE p.user = :user", Photo.class);
        query.setParameter("user", user);
        return query.getResultList();
    }

    @Override
    public List<Photo> findByUserId(Long userId) {
        TypedQuery<Photo> query = em.createQuery(
                "SELECT p FROM Photo p WHERE p.user.id = :userId", Photo.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    @Override
    public List<Photo> findByPinPoint(String pinPoint) {
        TypedQuery<Photo> query = em.createQuery(
                "SELECT p FROM Photo p WHERE p.pinPoint = :pinPoint", Photo.class);
        query.setParameter("pinPoint", pinPoint);
        return query.getResultList();
    }

    @Override
    public List<Photo> getAll() {
        TypedQuery<Photo> query = em.createQuery("SELECT p FROM Photo p", Photo.class);
        return query.getResultList();
    }

    @Override
    public long countByUser(User user) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(p) FROM Photo p WHERE p.user = :user", Long.class);
        query.setParameter("user", user);
        return query.getSingleResult();
    }

    @Override
    public List<Photo> getPhotosPaginated(int first, int pageSize) {
        TypedQuery<Photo> query = em.createQuery("SELECT p FROM Photo p ORDER BY p.uploadDate DESC", Photo.class);
        query.setFirstResult(first);
        query.setMaxResults(pageSize);
        return query.getResultList();
    }

    @Override
    public int getAllCount() {
        TypedQuery<Long> query = em.createQuery("SELECT COUNT(p) FROM Photo p", Long.class);
        return query.getSingleResult().intValue();
    }

    @Override
    public List<String> getAllPinPoints() {
        TypedQuery<String> query = em.createQuery("SELECT DISTINCT p.pinPoint FROM Photo p WHERE p.pinPoint IS NOT NULL", String.class);
        return query.getResultList();
    }

    @Override
    public List<Photo> findFilteredPhotos(Map<String, Object> filters, int first, int pageSize) {
        if (pageSize <= 0) throw new IllegalArgumentException("pageSize must be positive");
        if (first < 0) throw new IllegalArgumentException("first must be non-negative");

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Photo> cq = cb.createQuery(Photo.class);
        Root<Photo> photo = cq.from(Photo.class);

        List<Predicate> predicates = buildPredicates(cb, photo, filters);

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        cq.orderBy(cb.desc(photo.get("uploadDate")));
        cq.distinct(true);

        TypedQuery<Photo> query = em.createQuery(cq);
        query.setFirstResult(first);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }

    @Override
    public int getFilteredCount(Map<String, Object> filters) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Photo> photo = cq.from(Photo.class);

        cq.select(cb.countDistinct(photo));

        List<Predicate> predicates = buildPredicates(cb, photo, filters);

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        return em.createQuery(cq).getSingleResult().intValue();
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Photo> photo, Map<String, Object> filters) {
        List<Predicate> predicates = new ArrayList<>();
        Join<Photo, PhotoTag> photoTagJoin = null;
        Join<PhotoTag, Tag> tagJoin = null;

        if (filters == null) return predicates;

        // Status filter (e.g., for PENDING, REJECTED)
        Object statusFilter = filters.get("status");
        if (statusFilter instanceof List) {
            predicates.add(photo.get("status").in((List<?>) statusFilter));
        } else if (statusFilter != null) {
            predicates.add(cb.equal(photo.get("status"), statusFilter));
        }

        // Current user exclusion - ALWAYS exclude current user's photos when searching
        User currentUser = (User) filters.get("currentUser");
        if (currentUser != null) {
            predicates.add(cb.notEqual(photo.get("user"), currentUser));
        }

        // Location filter
        String filterLocation = (String) filters.get("filterLocation");
        if (filterLocation != null && !filterLocation.isEmpty()) {
            predicates.add(cb.equal(photo.get("pinPoint"), filterLocation));
        }

        // Tags filter
        @SuppressWarnings("unchecked")
        List<String> filterTags = (List<String>) filters.get("filterTags");
        if (filterTags != null && !filterTags.isEmpty()) {
            photoTagJoin = photo.join("photoTags", JoinType.LEFT);
            tagJoin = photoTagJoin.join("tag", JoinType.LEFT);
            predicates.add(tagJoin.get("tagName").in(filterTags));
        }

        // Rating filter
        Double filterMinRating = (Double) filters.get("filterMinRating");
        if (filterMinRating != null && filterMinRating > 0) {
            predicates.add(cb.ge(photo.get("averagePhotoRating"), filterMinRating));
        }

        // Search text
        String searchText = (String) filters.get("searchText");
        if (searchText != null && !searchText.trim().isEmpty()) {
            String searchPattern = "%" + searchText.toLowerCase() + "%";

            if (tagJoin == null && photoTagJoin == null) {
                photoTagJoin = photo.join("photoTags", JoinType.LEFT);
                tagJoin = photoTagJoin.join("tag", JoinType.LEFT);
            }

            List<Predicate> searchPredicates = new ArrayList<>();
            searchPredicates.add(cb.like(cb.lower(photo.get("description")), searchPattern));
            searchPredicates.add(cb.like(cb.lower(photo.get("pinPoint")), searchPattern));
            if (tagJoin != null) {
                searchPredicates.add(cb.like(cb.lower(tagJoin.get("tagName")), searchPattern));
            }

            predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
        }

        // Current user exclusion (repeated for cases with no active filters)
        if (currentUser != null && !hasActiveFilters(filters)) {
            predicates.add(cb.notEqual(photo.get("user"), currentUser));
        }

        // Log the filters for debugging
        System.out.println("Building predicates with filters: " + filters);

        return predicates;
    }

    private boolean hasActiveFilters(Map<String, Object> filters) {
        String filterLocation = (String) filters.get("filterLocation");
        @SuppressWarnings("unchecked")
        List<String> filterTags = (List<String>) filters.get("filterTags");
        Double filterMinRating = (Double) filters.get("filterMinRating");
        String searchText = (String) filters.get("searchText");

        return (filterLocation != null && !filterLocation.isEmpty()) ||
                (filterTags != null && !filterTags.isEmpty()) ||
                (filterMinRating != null && filterMinRating > 0) ||
                (searchText != null && !searchText.trim().isEmpty());
    }

    @Override
    public int getTotalEntityCount(Map<String, FilterMeta> filters, Map<String, Object> exactMatchFilters) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Photo> photo = cq.from(Photo.class);

        cq.select(cb.countDistinct(photo));

        List<Predicate> predicates = buildPredicates(cb, photo, exactMatchFilters);

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        return em.createQuery(cq).getSingleResult().intValue();
    }

    @Override
    public List<Photo> findPaginatedEntities(
            Map<String, FilterMeta> filters,
            Map<String, Object> exactMatchFilters,
            int first,
            int pageSize
    ) {
        if (pageSize <= 0) throw new IllegalArgumentException("pageSize must be positive");
        if (first < 0) throw new IllegalArgumentException("first must be non-negative");

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Photo> cq = cb.createQuery(Photo.class);
        Root<Photo> photo = cq.from(Photo.class);

        List<Predicate> predicates = buildPredicates(cb, photo, exactMatchFilters);

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        cq.orderBy(cb.desc(photo.get("uploadDate")));
        cq.distinct(true);

        TypedQuery<Photo> query = em.createQuery(cq);
        query.setFirstResult(first);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }
}