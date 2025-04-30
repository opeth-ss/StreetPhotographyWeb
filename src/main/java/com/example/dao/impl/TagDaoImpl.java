package com.example.dao.impl;

import com.example.dao.TagDao;
import com.example.model.Tag;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class TagDaoImpl extends BaseDaoImpl<Tag, Long> implements TagDao {
    @PersistenceContext(unitName = "StreetPhotography") // This injects the EntityManager
    private EntityManager em;

    public TagDaoImpl(){
        super(Tag.class);
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

    public List<Tag> getAll(){
        return em.createQuery("SELECT t FROM Tag t ", Tag.class)
                .getResultList();
    }

    @Override
    public List<Tag> getAll(String query, Integer limit) {
        TypedQuery<Tag> typedQuery = em.createQuery(
                        "SELECT t FROM Tag t WHERE t.tagName LIKE :searchTerm ORDER BY t.tagName", Tag.class)
                .setParameter("searchTerm", "%" + query + "%");

        if (limit != null) {
            typedQuery.setMaxResults(limit);
        }

        return typedQuery.getResultList();
    }

    @Override
    public List<Tag> getAll(Integer limit) {
        TypedQuery<Tag> typedQuery = em.createQuery(
                "SELECT t FROM Tag t ORDER BY t.tagName", Tag.class);

        if (limit != null) {
            typedQuery.setMaxResults(limit);
        }

        return typedQuery.getResultList();
    }
}
