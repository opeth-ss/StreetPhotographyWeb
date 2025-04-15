package com.example.dao.impl;

import com.example.dao.PhotoTagDao;
import com.example.model.Photo;
import com.example.model.PhotoTag;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class PhotoTagDaoImpl extends BaseDaoImpl<PhotoTag, Long> implements PhotoTagDao {
    @PersistenceContext(unitName = "StreetPhotography") // This injects the EntityManager
    private EntityManager em;

    public PhotoTagDaoImpl(){
        super( PhotoTag.class);
    }
    @Override
    public boolean save(PhotoTag photoTag) {
        boolean status = false;

        try {
            em.persist(photoTag); // EntityManager automatically handles the transaction
            status = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    public boolean update(PhotoTag photoTag) {
        boolean status = false;

        try {
            em.merge(photoTag); // EntityManager automatically handles the transaction
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
            PhotoTag photoTag = em.find(PhotoTag.class, id);
            if (photoTag != null) {
                em.remove(photoTag); // EntityManager automatically handles the transaction
                status = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    public PhotoTag findById(Long id) {
        PhotoTag photoTag = null;
        try {
            photoTag = em.find(PhotoTag.class, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return photoTag;
    }

    @Override
    public List<PhotoTag> findByPhotoId(Long photoId) {
        TypedQuery<PhotoTag> query = em.createQuery(
                "SELECT pt FROM PhotoTag pt WHERE pt.photo.id = :photoId", PhotoTag.class);
        query.setParameter("photoId", photoId);
        return query.getResultList();
    }

    @Override
    public List<PhotoTag> findByTagId(Long tagId) {
        TypedQuery<PhotoTag> query = em.createQuery(
                "SELECT pt FROM PhotoTag pt WHERE pt.tag.id = :tagId", PhotoTag.class);
        query.setParameter("tagId", tagId);
        return query.getResultList();
    }
}
