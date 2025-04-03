package com.example.dao;

import com.example.model.Tag;

import java.util.List;
import java.util.Optional;

public interface TagDao extends BaseDao<Tag, Long> {
    Tag findByName(String name);
    List<Tag> getAll(String like);
}
