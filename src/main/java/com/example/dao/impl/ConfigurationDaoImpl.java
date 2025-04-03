package com.example.dao.impl;

import com.example.dao.ConfigurationDao;
import com.example.model.Configuration;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

public class ConfigurationDaoImpl implements ConfigurationDao {
    @PersistenceContext(unitName = "StreetPhotography")
    private EntityManager em;

    @Override
    public boolean save(Configuration configuration) {
        try {
            em.persist(configuration);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(Configuration configuration) {
        try {
            em.merge(configuration);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try {
            Configuration configuration = em.find(Configuration.class, id);
            if (configuration != null) {
                em.remove(configuration);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Configuration findById(Long id) {
        return em.find(Configuration.class, id);
    }

    @Override
    public Configuration getConfiguration() {
        List<Configuration> configs = em.createQuery("SELECT c FROM Configuration c", Configuration.class)
                .getResultList();
        return configs.isEmpty() ? null : configs.get(0);
    }
}
