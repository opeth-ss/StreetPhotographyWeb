package com.example.services;


import com.example.dao.PhotoTagDao;
import com.example.dao.TagDao;
import com.example.model.Photo;
import com.example.model.PhotoTag;
import com.example.model.Tag;
import com.example.model.User;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PhotoTagService {

    @Inject
    private PhotoTagDao photoTagDao;

    @Inject
    private TagDao tagDao;

    @Transactional
    public void addTagsToPhoto(Photo photo, List<Tag> tags, User user) {
        for (Tag tagName : tags) {
            PhotoTag photoTag = new PhotoTag();
            photoTag.setPhoto(photo);
            photoTag.setUser(user);
            photoTag.setTag(tagName);
            photoTagDao.save(photoTag);
        }
    }

    public List<Tag> getAllTags() {
        return tagDao.getAll();
    }
    public List<Tag> getAllTags(String like) {
        return tagDao.getAll(like);
    }

    public List<Tag> getTags(String query, Integer limit) {
        if (query == null || query.isEmpty()) {
            // Return limited results for initial load
            return tagDao.getAll(limit);
        } else {
            // Return filtered results for search
            return tagDao.getAll(query, limit);
        }
    }


    public Tag findTagByName(String tagName) {
        return tagDao.findByName(tagName);
    }
}
