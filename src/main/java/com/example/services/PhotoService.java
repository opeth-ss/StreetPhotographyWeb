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
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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

    @PersistenceContext(unitName = "StreetPhotography") // This injects the EntityManager
    private EntityManager em;


    public List<Photo> getPhotosByUser(User user) {
        return photoDao.findByUser(user);
    }

    public List<Photo> searchByLocation(String searchCriteria) {
        return photoDao.findByPinPoint(searchCriteria);
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
}
