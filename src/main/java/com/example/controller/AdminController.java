package com.example.controller;


import com.example.model.Configuration;
import com.example.model.Photo;
import com.example.model.Tag;
import com.example.model.User;
import com.example.services.AuthenticationService;
import com.example.services.ConfigurationService;
import com.example.services.PhotoService;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("adminController")
@SessionScoped
public class AdminController implements Serializable {
    private static final long serialVersionUID = 1L;
    private User selectedUser;
    private Double rating;
    private Integer totalPost;
    @Inject
    private AuthenticationService authenticationService;

    @Inject
    private PhotoService photoService;

    @Inject
    private UserController userController;

    @Inject
    private  PhotoController photoController;

    @Inject
    private ConfigurationService configurationService;

    public List<Photo> getAllPicturesForAdmin(){
        try {
            if (userController.getUser() == null) {
                return new ArrayList<>();
            }
            return photoService.getAll();
        }catch (Exception e) {
            return new ArrayList<>();
        }
    }
    public List<String> getPhotoTagNames(Photo photo) {
        List<Tag> tags = photoService.getPhotoTags(photo);
        List<String> tagNames = new ArrayList<>();
        for (Tag tag : tags) {
            tagNames.add(tag.getTagName());
        }
        return tagNames;
    }

    public List<User> getUsers(){
        return authenticationService.findAll();
    }

    public void saveOrUpdateConfig(){
        if(configurationService.saveOrUpdateConfig(rating, totalPost)){
            totalPost= null;
            rating= null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Configuration Updated", "Configuration Updated UpdSuccessfully"));
        }else{
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Update Failed", "Couldn't Update Configuration"));
        }
    }

    public void deleteUser(){
        try {
            if (selectedUser != null && userController.hasRole("admin")) {
                authenticationService.deleteUser(selectedUser);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "User Deleted", "User has been deleted successfully"));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_FATAL,
                            "Error", "An error occurred while deleting the photo"));
        }
    }

    public void updateUser() {
        try {
            if (selectedUser != null && userController.hasRole("admin")) {
                // Get the current user from database
                User existingUser = authenticationService.findUserById(selectedUser.getId());

                // Only update fields that have new values
                if (selectedUser.getUserName() != null && !selectedUser.getUserName().isEmpty()) {
                    existingUser.setUserName(selectedUser.getUserName());
                }

                if (selectedUser.getEmail() != null && !selectedUser.getEmail().isEmpty()) {
                    existingUser.setEmail(selectedUser.getEmail());
                }

                // Password is optional - only update if provided
                if (selectedUser.getPassword() != null && !selectedUser.getPassword().isEmpty()) {
                    existingUser.setPassword(selectedUser.getPassword());
                }

                // Save the updated user
                authenticationService.updateUser(existingUser);

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "User Updated", "User has been updated successfully"));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", "An error occurred while updating the user"));
            FacesContext.getCurrentInstance().validationFailed();
        }
    }

    public User getSelectedUser() {
        return selectedUser;
    }

    public void setSelectedUser(User selectedUser) {
        this.selectedUser = selectedUser;
    }

    public void prepareUpdateUser(User user) {
        this.selectedUser = user;
    }

    public void prepareDeleteUser(User user) {
        this.selectedUser = user;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getTotalPost() {
        return totalPost;
    }

    public void setTotalPost(Integer totalPost) {
        this.totalPost = totalPost;
    }
}
