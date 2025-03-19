package com.example.dao;

import com.example.model.Tag;

import java.util.Optional;

public interface TagDao extends BaseDao<Tag, Long> {
    Optional<Tag> findByName(String name);
}
