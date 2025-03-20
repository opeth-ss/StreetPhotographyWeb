package com.example.controller;

import com.example.model.Photo;
import com.example.model.User;
import com.example.services.PhotoTagService;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@Named("photoTagController")
@SessionScoped
public class PhotoTagController implements Serializable {
    private static final long serialVersionUID = 1L;
    @Inject
    private PhotoTagService photoTagService;

    public boolean saveTag(Photo photo, String csvTag, User user){
        try{
            photoTagService.addTagsToPhoto(photo, csvTag, user);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
