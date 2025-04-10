package com.example.dao.impl;

import com.example.dao.TagDao;
import com.example.model.Tag;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

public class TagDaoImpl implements TagDao {
    @PersistenceContext(unitName = "StreetPhotography") // This injects the EntityManager
    private EntityManager em;

    @Override
    public boolean save(Tag tag) {
        boolean status = false;

        try {
            em.persist(tag); // EntityManager automatically handles the transaction
            status = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    public boolean update(Tag tag) {
        boolean status = false;

        try {
            em.merge(tag); // EntityManager automatically handles the transaction
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
            Tag tag = em.find(Tag.class, id);
            if (tag != null) {
                em.remove(tag); // EntityManager automatically handles the transaction
                status = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    public Tag findById(Long id) {
        Tag tag = null;
        try {
            tag = em.find(Tag.class, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tag;
    }

    @Override
    public Tag findByName(String name) {
        return em.createQuery("SELECT t FROM Tag t WHERE t.tagName = :name", Tag.class)
                .setParameter("name", name).getSingleResult();
    }

    @Override
    public List<Tag> getAll(String like){
        return em.createQuery("SELECT t FROM Tag t WHERE t.tagName LIKE :searchTerm", Tag.class)
                .setParameter("searchTerm", "%" + like + "%")
                .getResultList();
    }

    @Override
    public List<Tag> getAll(){
        return em.createQuery("SELECT t FROM Tag t ", Tag.class)
                .getResultList();
    }
}
