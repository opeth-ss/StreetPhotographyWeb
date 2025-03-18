package com.example.dao.impl;

import com.example.dao.PhotoDao;
import com.example.model.Photo;
import com.example.model.User;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class PhotoDaoImpl implements PhotoDao {
    @PersistenceContext(unitName = "StreetPhotography") // This injects the EntityManager
    private EntityManager em;

    @Override
    public boolean save(Photo photo) {
        boolean status = false;

        try {
            em.persist(photo); // EntityManager automatically handles the transaction
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
            em.merge(photo); // EntityManager automatically handles the transaction
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
                em.remove(photo); // EntityManager automatically handles the transaction
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
    public List<Photo> findByDescriptionContaining(String keyword) {
        TypedQuery<Photo> query = em.createQuery(
                "SELECT p FROM Photo p WHERE p.description LIKE :keyword", Photo.class);
        query.setParameter("keyword", "%" + keyword + "%");
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
    public List<Photo> findRecentPhotos() {
        TypedQuery<Photo> query = em.createQuery(
                "SELECT p FROM Photo p ORDER BY p.uploadDate DESC", Photo.class);
        return query.getResultList();
    }
}