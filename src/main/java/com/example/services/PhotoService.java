package com.example.services;

import com.example.dao.PhotoDao;
import com.example.dao.PhotoTagDao;
import com.example.dao.TagDao;
import com.example.model.Photo;
import com.example.model.PhotoTag;
import com.example.model.Tag;
import com.example.model.User;
import sun.awt.X11.XSystemTrayPeer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class PhotoService  {

    @Inject
    private PhotoDao photoDao;

    @Inject
    private PhotoTagDao photoTagDao;

    @Inject
    private TagDao tagDao;

    @Transactional
    public boolean savePhoto(Photo photo){
        return photoDao.save(photo);
    }

    @Transactional
    public void deletePhoto(Photo photo) {
        photoDao.deleteById(photo.getId());
    }

    @Transactional
    public void updatePhoto(Photo photo){ photoDao.update(photo);}


    public List<Photo> getPhotosByUser(User user) {
        return photoDao.findByUser(user);
    }

    public List<Photo> searchByLocation(String searchCriteria) {
        return photoDao.findByPinPoint(searchCriteria);
    }

    public List<Photo> searchByDescription(String searchCriteria) {
        return photoDao.findByDescriptionContaining(searchCriteria);
    }

    public List<Photo> searchByTag(String searchText) {
        Optional<Tag> tagOptional = tagDao.findByName(searchText);
        if (!tagOptional.isPresent()) {
            return new ArrayList<>();
        }

        Tag tag = tagOptional.get();
        List<PhotoTag> photoTags = photoTagDao.findByTagId(tag.getId());

        if (photoTags.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> photoIds = photoTags.stream()
                .map(photoTag -> photoTag.getPhoto().getId())
                .collect(Collectors.toList());

        List<Photo> photos = new ArrayList<>();
        for (Long photoId : photoIds) {
            Photo photo = photoDao.findById(photoId);
            if (photo != null) {
                photos.add(photo);
            }
        }
        return photos;
    }

    public List<Tag> getPhotoTags(Photo photo) {
        List<PhotoTag> photoTags = photoTagDao.findByPhotoId(photo.getId());
        List<Tag> tags = new ArrayList<>();

        for (PhotoTag photoTag : photoTags) {
            tags.add(photoTag.getTag());
        }
        return tags;
    }

    public List<Photo> getLatestPosts() {
        return photoDao.findRecentPhotos();
    }
}
