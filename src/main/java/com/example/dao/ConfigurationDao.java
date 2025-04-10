package com.example.dao;

import com.example.model.Configuration;

public interface ConfigurationDao extends BaseDao<Configuration, Long>{
     Configuration getConfiguration();
}
