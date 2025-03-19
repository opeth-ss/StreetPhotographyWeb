package com.example.dao;

import com.example.model.PhotoTag;

import java.util.List;

public interface PhotoTagDao extends BaseDao<PhotoTag, Long> {
    List<PhotoTag> findByPhotoId(Long photoId);
    List<PhotoTag> findByTagId(Long tagId);
}
