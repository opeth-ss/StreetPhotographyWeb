package com.example.services;

import com.example.dao.ConfigurationDao;
import com.example.model.Configuration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

@ApplicationScoped
public class ConfigurationService {
    @Inject
    private ConfigurationDao configurationDao;

    @Transactional
    public boolean saveOrUpdateConfig(Double minRating, Integer minPhoto){
        if(getConfiguration() != null){
           Configuration oldConfig =  getConfiguration();
           oldConfig.setMinRating(minRating);
           oldConfig.setMinPhotos(minPhoto);
            configurationDao.update(oldConfig);
        }else{
            Configuration config = new Configuration();
            config.setMinRating(minRating);
            config.setMinPhotos(minPhoto);
            configurationDao.save(config);
        }
        return true;
    }

    public Configuration getConfiguration(){
        return configurationDao.getConfiguration();
    }
}
