package com.example.services;

import com.example.dao.PhotoDao;
import com.example.model.Photo;
import com.example.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class PhotoService  {

    @Inject
    private PhotoDao photoDao;

    @Transactional
    public boolean savePhoto(Photo photo){
        return photoDao.save(photo);
    }

    @Transactional
    public void deletePhoto(Photo photo) {
        photoDao.deleteById(photo.getId());
    }


    public List<Photo> getPhotosByUser(User user) {
        return photoDao.findByUser(user);
    }
}
