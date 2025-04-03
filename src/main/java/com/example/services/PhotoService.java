package com.example.services;

import com.example.dao.PhotoDao;
import com.example.dao.PhotoTagDao;
import com.example.dao.TagDao;
import com.example.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class PhotoService  {

    @Inject
    private PhotoDao photoDao;

    @Inject
    private PhotoTagDao photoTagDao;

    @Inject
    private TagDao tagDao;

    @Inject
    private ConfigurationService configurationService;

    @Transactional
    public void savePhoto(Photo photo){
        photoDao.save(photo);
    }

    @Transactional
    public void deletePhoto(Photo photo) {
        photoDao.deleteById(photo.getId());
    }

    @Transactional
    public void updatePhoto(Photo photo){ photoDao.update(photo);}

    @PersistenceContext(unitName = "StreetPhotography")
    private EntityManager em;

    public List<Photo> getPhotosByUser(User user) {
        return photoDao.findByUser(user);
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
        Configuration config = getConfig();
        return photoDao.findRecentPhotos(config.getMinPhotos(), config.getMinRating());
    }

    public List<Photo> searchPhotos(String searchText) {
        return photoDao.searchPhotosList(searchText);
    }

    public List<Photo> getAll(){
        return photoDao.getAll();
    }

    public Photo refreshPhoto(Photo photo) {
        if (photo == null || photo.getId() == null) {
            return photo;
        }
        return em.find(Photo.class, photo.getId());
    }

    private Configuration getConfig(){
        return configurationService.getConfiguration();
    }
}
