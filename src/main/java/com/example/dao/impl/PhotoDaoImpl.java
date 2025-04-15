package com.example.dao.impl;

import com.example.dao.PhotoDao;
import com.example.model.Photo;
import com.example.model.PhotoTag;
import com.example.model.Tag;
import com.example.model.User;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class PhotoDaoImpl implements PhotoDao {
    @PersistenceContext(unitName = "StreetPhotography")
    private EntityManager em;

    @Override
    public boolean save(Photo photo) {
        boolean status = false;
        try {
            em.persist(photo);
            status = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    public boolean update(Photo photo) {
        boolean status = false;
        try {
            em.merge(photo);
            status = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    public boolean deleteById(Long id) {
        boolean status = false;
        try {
            Photo photo = em.find(Photo.class, id);
            if (photo != null) {
                em.remove(photo);
                status = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    public Photo findById(Long id) {
        Photo photo = null;
        try {
            photo = em.find(Photo.class, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return photo;
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
    public List<Photo> findRecentPhotos(Integer minPhotos, Double minRating, int first, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        if (first < 0) {
            throw new IllegalArgumentException("first must be non-negative");
        }

        StringBuilder queryStr = new StringBuilder("SELECT p FROM Photo p");
        if (minRating != null) {
            queryStr.append(" WHERE p.averagePhotoRating >= :minRating");
        }
        queryStr.append(" ORDER BY p.uploadDate DESC");

        TypedQuery<Photo> query = em.createQuery(queryStr.toString(), Photo.class);
        if (minRating != null) {
            query.setParameter("minRating", minRating);
        }

        query.setFirstResult(first);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }

    @Override
    public List<Photo> searchPhotosList(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return em.createQuery("SELECT p FROM Photo p ORDER BY p.uploadDate DESC", Photo.class)
                    .getResultList();
        }
        String queryStr = "SELECT DISTINCT p FROM Photo p " +
                "LEFT JOIN p.photoTags pt " +
                "LEFT JOIN pt.tag t " +
                "WHERE LOWER(p.description) LIKE :searchText " +
                "OR LOWER(p.pinPoint) LIKE :searchText " +
                "OR LOWER(t.tagName) LIKE :searchText " +
                "ORDER BY p.uploadDate DESC";
        return em.createQuery(queryStr, Photo.class)
                .setParameter("searchText", "%" + searchText.toLowerCase() + "%")
                .getResultList();
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
    public List<String> getAllPinPoints(){
        TypedQuery<String> query = em.createQuery("SELECT DISTINCT p.pinPoint FROM Photo p WHERE p.pinPoint IS NOT NULL", String.class);
        return query.getResultList();
    }

    @Override
    public List<Photo> findFilteredPhotos(String filterLocation, List<String> filterTags, Double filterMinRating,
                                          String searchText, User currentUser, int first, int pageSize) {
        if (pageSize <= 0) throw new IllegalArgumentException("pageSize must be positive");
        if (first < 0) throw new IllegalArgumentException("first must be non-negative");

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Photo> cq = cb.createQuery(Photo.class);
        Root<Photo> photo = cq.from(Photo.class);

        // Initialize list for predicates
        List<Predicate> predicates = new ArrayList<>();

        // Handle joins (only when needed)
        Join<Photo, PhotoTag> photoTagJoin = null;
        Join<PhotoTag, Tag> tagJoin = null;

        // Location filter
        if (filterLocation != null && !filterLocation.isEmpty()) {
            predicates.add(cb.equal(photo.get("pinPoint"), filterLocation));
        }

        // Tags filter (requires joins)
        if (filterTags != null && !filterTags.isEmpty()) {
            photoTagJoin = photo.join("photoTags", JoinType.LEFT);
            tagJoin = photoTagJoin.join("tag", JoinType.LEFT);
            predicates.add(tagJoin.get("tagName").in(filterTags));
        }

        // Rating filter
        if (filterMinRating != null && filterMinRating > 0) {
            predicates.add(cb.ge(photo.get("averagePhotoRating"), filterMinRating));
        }

        // Search text (requires joins if searching tags)
        if (searchText != null && !searchText.trim().isEmpty()) {
            String searchPattern = "%" + searchText.toLowerCase() + "%";

            // Ensure joins exist if needed for tag search
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

        // Exclude current user's photos if no other filters
        if (currentUser != null && !hasActiveFilters(filterLocation, filterTags, filterMinRating, searchText)) {
            predicates.add(cb.notEqual(photo.get("user"), currentUser));
        }

        // Apply all predicates
        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        // Ordering
        cq.orderBy(cb.desc(photo.get("uploadDate")));

        // Distinct results
        cq.distinct(true);

        // Pagination
        TypedQuery<Photo> query = em.createQuery(cq);
        query.setFirstResult(first);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }

    @Override
    public int getFilteredCount(String filterLocation, List<String> filterTags, Double filterMinRating,
                                String searchText, User currentUser) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Photo> photo = cq.from(Photo.class);

        // Count distinct photos
        cq.select(cb.countDistinct(photo));

        // Initialize list for predicates
        List<Predicate> predicates = new ArrayList<>();

        // Handle joins (only when needed)
        Join<Photo, PhotoTag> photoTagJoin = null;
        Join<PhotoTag, Tag> tagJoin = null;

        // Location filter
        if (filterLocation != null && !filterLocation.isEmpty()) {
            predicates.add(cb.equal(photo.get("pinPoint"), filterLocation));
        }

        // Tags filter (requires joins)
        if (filterTags != null && !filterTags.isEmpty()) {
            photoTagJoin = photo.join("photoTags", JoinType.LEFT);
            tagJoin = photoTagJoin.join("tag", JoinType.LEFT);
            predicates.add(tagJoin.get("tagName").in(filterTags));
        }

        // Rating filter
        if (filterMinRating != null && filterMinRating > 0) {
            predicates.add(cb.ge(photo.get("averagePhotoRating"), filterMinRating));
        }

        // Search text (requires joins if searching tags)
        if (searchText != null && !searchText.trim().isEmpty()) {
            String searchPattern = "%" + searchText.toLowerCase() + "%";

            // Ensure joins exist if needed for tag search
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

        // Exclude current user's photos if no other filters
        if (currentUser != null && !hasActiveFilters(filterLocation, filterTags, filterMinRating, searchText)) {
            predicates.add(cb.notEqual(photo.get("user"), currentUser));
        }

        // Apply all predicates
        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        return em.createQuery(cq).getSingleResult().intValue();
    }

    // Helper method to check if any filters are active
    private boolean hasActiveFilters(String filterLocation, List<String> filterTags, Double filterMinRating, String searchText) {
        return (filterLocation != null && !filterLocation.isEmpty()) ||
                (filterTags != null && !filterTags.isEmpty()) ||
                (filterMinRating != null && filterMinRating > 0) ||
                (searchText != null && !searchText.trim().isEmpty());
    }
}