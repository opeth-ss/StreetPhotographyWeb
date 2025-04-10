package com.example.dao.impl;

import com.example.dao.BaseDao;

import javax.persistence.*;
import java.io.Serializable;

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
        EntityTransaction transaction = em.getTransaction();
        boolean status = false;

        try {
            transaction.begin();
            em.persist(entity);
            transaction.commit();
            status = true;
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
            e.printStackTrace();
        } finally {
            em.close(); // Close the EntityManager after use
        }
        return status;
    }

    @Override
    public boolean update(T entity) {
        EntityTransaction transaction = em.getTransaction();
        boolean status = false;

        try {
            transaction.begin();
            em.merge(entity);
            transaction.commit();
            status = true; // Set status to true after a successful commit
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
        return status;
    }

    @Override
    public boolean deleteById(ID id) {
        EntityTransaction transaction = em.getTransaction();
        boolean status = false;

        try {
            transaction.begin();
            T entity = em.find(entityClass, id);
            if (entity != null) {
                em.remove(entity);
                status = true;
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
        return status;
    }

    @Override
    public T findById(ID id) {
        T entity = null;
        try {
            entity = em.find(entityClass, id);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
        return entity;
    }
}
