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
import java.util.Optional;

@ApplicationScoped
public class PhotoTagService {

    @Inject
    private PhotoTagDao photoTagDao;

    @Inject
    private TagDao tagDao;

    @Transactional
    public void addTagsToPhoto(Photo photo, String tagsCsv, User user){
        String[] tagNames = tagsCsv.split(",");
        for(String tagName : tagNames){
            String trimmedTagName = tagName.trim();
            Optional<Tag> existingTag = tagDao.findByName(trimmedTagName);

            Tag tag;
            if(existingTag.isPresent()) {
                tag = existingTag.get();
            } else{
                tag = new Tag();
                tag.setTagName((trimmedTagName));
                tagDao.save(tag);
            }

            PhotoTag photoTag = new PhotoTag();
            photoTag.setPhoto(photo);
            photoTag.setUser(user);
            photoTag.setTag(tag);

            photoTagDao.save(photoTag);
        }
    }
}
