package com.example.services;

import com.example.dao.PhotoDao;
import com.example.model.Photo;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.Serializable;

@ApplicationScoped
public class PhotoService  {

    @Inject
    private PhotoDao photoDao;

    @Transactional
    public boolean savePhoto(Photo photo){
        return photoDao.save(photo);
    }

}
