package com.example.dao.impl;

import com.example.dao.PhotoDao;
import com.example.dao.RatingDao;
import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.User;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;

public class RatingDaoImpl extends BaseDaoImpl<Rating, Long> implements RatingDao {
    @PersistenceContext(unitName = "StreetPhotography") // This injects the EntityManager
    private EntityManager em;

    public RatingDaoImpl(){
        super(Rating.class);
    }
    @Override
    public boolean updatePhotoAndUser(Photo photo, User user) {
        try {
            em.getTransaction().begin();
            em.merge(photo);
            em.merge(user);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Rating> findByPhotoOwner(User photoOwnerUser) {
        TypedQuery<Rating> query = em.createQuery(
                "SELECT r FROM Rating r JOIN r.photo p WHERE p.user = :photoOwnerUser",
                Rating.class);
        query.setParameter("photoOwnerUser", photoOwnerUser);
        return query.getResultList();
    }

    @Override
    public List<Rating> findByPhoto(Photo photo) {
        TypedQuery<Rating> query = em.createQuery("SELECT r FROM Rating r WHERE r.photo = :photo", Rating.class);
        query.setParameter("photo", photo);
        return query.getResultList();
    }

    @Override
    public long countByPhoto(Photo photo) {
        TypedQuery<Long> query = em.createQuery("SELECT COUNT(r) FROM Rating r WHERE r.photo = :photo", Long.class);
        query.setParameter("photo", photo);
        return query.getSingleResult();
    }


    @Override
    public Rating findByPhotoAndUser(Photo photo, User user) {
        TypedQuery<Rating> query = em.createQuery(
                "SELECT r FROM Rating r WHERE r.photo = :photo AND r.user = :user", Rating.class);
        query.setParameter("photo", photo);
        query.setParameter("user", user);

        return query.getResultList().stream().findFirst().orElse(null);
    }

    @Override
    public boolean ratingExists(Photo photo, User user) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(r) FROM Rating r WHERE r.user = :user AND r.photo = :photo", Long.class);
        query.setParameter("user", user);
        query.setParameter("photo", photo);
        return query.getSingleResult() > 0;
    }

    @Override
    public List<Rating> findUserRating(User user) {
        TypedQuery<Rating> query = em.createQuery(
                "SELECT r FROM Rating r WHERE r.user = :user " , Rating.class);
        query.setParameter("user", user);
        return query.getResultList();
    }
}
