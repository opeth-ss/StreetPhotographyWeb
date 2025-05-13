package com.example.dao.impl;

import com.example.dao.TagDao;
import com.example.model.Tag;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class TagDaoImpl extends BaseDaoImpl<Tag, Long> implements TagDao {

    @PersistenceContext(unitName = "StreetPhotography")
    private EntityManager em;

    public TagDaoImpl() {
        super(Tag.class);
    }

    @Override
    public boolean save(Tag tag) {
        boolean status = false;
        try {
            em.persist(tag);
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
            em.merge(tag);
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
                em.remove(tag);
                status = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    public Tag findById(Long id) {
        try {
            return em.find(Tag.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Tag> getAll() {
        TypedQuery<Tag> query = em.createQuery("SELECT t FROM Tag t", Tag.class);
        return query.getResultList();
    }

    @Override
    public List<Tag> getAll(String like) {
        TypedQuery<Tag> query = em.createQuery(
                "SELECT t FROM Tag t WHERE LOWER(t.tagName) LIKE LOWER(:like)", Tag.class);
        query.setParameter("like", "%" + like + "%");
        return query.getResultList();
    }

    @Override
    public List<Tag> getAll(Integer limit) {
        TypedQuery<Tag> query = em.createQuery("SELECT t FROM Tag t", Tag.class);
        if (limit != null) {
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }

    @Override
    public List<Tag> getAll(String query, Integer limit) {
        TypedQuery<Tag> queryObj = em.createQuery(
                "SELECT t FROM Tag t WHERE LOWER(t.tagName) LIKE LOWER(:query)", Tag.class);
        queryObj.setParameter("query", "%" + query + "%");
        if (limit != null) {
            queryObj.setMaxResults(limit);
        }
        return queryObj.getResultList();
    }

    @Override
    public List<Tag> getAll(Integer limit, Integer offset) {
        TypedQuery<Tag> query = em.createQuery("SELECT t FROM Tag t", Tag.class);
        if (limit != null) {
            query.setMaxResults(limit);
        }
        if (offset != null) {
            query.setFirstResult(offset);
        }
        return query.getResultList();
    }

    @Override
    public List<Tag> getAll(String query, Integer limit, Integer offset) {
        TypedQuery<Tag> queryObj = em.createQuery(
                "SELECT t FROM Tag t WHERE LOWER(t.tagName) LIKE LOWER(:query)", Tag.class);
        queryObj.setParameter("query", "%" + query + "%");
        if (limit != null) {
            queryObj.setMaxResults(limit);
        }
        if (offset != null) {
            queryObj.setFirstResult(offset);
        }
        return queryObj.getResultList();
    }

    @Override
    public Tag findByName(String tagName) {
        TypedQuery<Tag> query = em.createQuery(
                "SELECT t FROM Tag t WHERE t.tagName = :tagName", Tag.class);
        query.setParameter("tagName", tagName);
        try {
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
}