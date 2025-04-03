package com.example.controller;

import com.example.model.Photo;
import com.example.model.Tag;
import com.example.model.User;
import com.example.services.PhotoTagService;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named("photoTagController")
@SessionScoped
public class PhotoTagController implements Serializable {
    private static final long serialVersionUID = 1L;
    @Inject
    private PhotoTagService photoTagService;

    public boolean saveTag(Photo photo, List<Tag> tags, User user){
        try{
            for(Tag tag : tags){
                System.out.print(tag.getTagName());
            }
            photoTagService.addTagsToPhoto(photo, tags, user);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
