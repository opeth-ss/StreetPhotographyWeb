package com.example.controller;

import com.example.model.Photo;
import com.example.model.Tag;
import com.example.model.User;
import com.example.services.AuthenticationService;
import com.example.services.ConfigurationService;
import com.example.services.PhotoService;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.primefaces.model.FilterMeta;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private PhotoController photoController;

    @Inject
    private ConfigurationService configurationService;

    private LazyDataModel<Photo> lazyPhotoModel;
    private LazyDataModel<User> lazyUserModel;


    @PostConstruct
    public void init() {
        Map<String, Object> exactMatchFilters = new HashMap<>();

        lazyPhotoModel = new GenericLazyDataModel<>(
                photoService.getPhotoDao(),
                exactMatchFilters
        );

        lazyUserModel = new GenericLazyDataModel<>(
                authenticationService.getUserDao(),
                exactMatchFilters
        );
    }

    public LazyDataModel<Photo> getLazyPhotoModel() {
        return lazyPhotoModel;
    }
    public LazyDataModel<User> getLazyUserModel() {
        return lazyUserModel;
    }


    public List<String> getPhotoTagNames(Photo photo) {
        List<Tag> tags = photoService.getPhotoTags(photo);
        List<String> tagNames = new ArrayList<>();
        for (Tag tag : tags) {
            tagNames.add(tag.getTagName());
        }
        return tagNames;
    }

    public List<User> getUsers() {
        return authenticationService.findAll();
    }

    public void saveOrUpdateConfig() {
        if (configurationService.saveOrUpdateConfig(rating, totalPost)) {
            totalPost = null;
            rating = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Configuration Updated", "Configuration Updated Successfully"));
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Update Failed", "Couldn't Update Configuration"));
        }
    }

    public void deleteUser() {
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
                User existingUser = authenticationService.findUserById(selectedUser.getId());
                if (selectedUser.getUserName() != null && !selectedUser.getUserName().isEmpty()) {
                    existingUser.setUserName(selectedUser.getUserName());
                }
                if (selectedUser.getEmail() != null && !selectedUser.getEmail().isEmpty()) {
                    existingUser.setEmail(selectedUser.getEmail());
                }
                if (selectedUser.getPassword() != null && !selectedUser.getPassword().isEmpty()) {
                    existingUser.setPassword(selectedUser.getPassword());
                }
                // Add this line to update the role
                if (selectedUser.getRole() != null) {
                    existingUser.setRole(selectedUser.getRole());
                }

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

    public User getSelectedUser() { return selectedUser; }
    public void setSelectedUser(User selectedUser) { this.selectedUser = selectedUser; }
    public void prepareUpdateUser(User user) { this.selectedUser = user; }
    public void prepareDeleteUser(User user) { this.selectedUser = user; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getTotalPost() { return totalPost; }
    public void setTotalPost(Integer totalPost) { this.totalPost = totalPost; }
}