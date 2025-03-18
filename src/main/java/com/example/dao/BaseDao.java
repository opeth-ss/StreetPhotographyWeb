package com.example.dao;

import java.io.Serializable;
import java.util.List;

public interface BaseDao<T, ID extends Serializable> {
    boolean save(T entity);
    boolean update(T entity);
    boolean deleteById(ID id);
    T findById(ID id);
}