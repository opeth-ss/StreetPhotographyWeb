package com.example.dao.impl;

import com.example.dao.ConfigurationDao;
import com.example.model.Configuration;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

public class ConfigurationDaoImpl extends BaseDaoImpl<Configuration, Long> implements ConfigurationDao{
    @PersistenceContext(unitName = "StreetPhotography")
    private EntityManager em;

    public ConfigurationDaoImpl() {
        super(Configuration.class);
    }

    @Override
    public Configuration getConfiguration() {
        List<Configuration> configs = em.createQuery("SELECT c FROM Configuration c", Configuration.class)
                .getResultList();
        return configs.isEmpty() ? null : configs.get(0);
    }
}
