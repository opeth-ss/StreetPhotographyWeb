package com.example.controller;

import com.example.model.Photo;
import com.example.model.Tag;
import com.example.model.User;
import com.example.services.AuthenticationService;
import com.example.services.ConfigurationService;
import com.example.services.PhotoService;
import com.example.utils.BlockedUserManager;
import com.example.utils.PhotoNavigationManager;
import com.example.utils.SessionUtil;
import org.primefaces.model.LazyDataModel;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named("adminController")
@SessionScoped
public class AdminController implements Serializable {
    private static final long serialVersionUID = 1L;
    private User selectedUser;
    private Double rating;
    private Integer totalPost;
    private Integer duration = 1;

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

    @Inject
    private PhotoNavigationManager photoNavigationManager;

    private LazyDataModel<Photo> lazyPhotoModel;
    private LazyDataModel<Photo> lazyApprovalPhotoModel;
    private LazyDataModel<User> lazyUserModel;

    @PostConstruct
    public void init() {
        Map<String, Object> exactMatchFilters = new HashMap<>();
        lazyPhotoModel = new GenericLazyDataModel<Photo, Long>(
                photoService.getPhotoDao(),
                exactMatchFilters
        );
        ((GenericLazyDataModel<Photo, Long>) lazyPhotoModel)
                .setOnDataLoadCallback(photos -> {
                    photoNavigationManager.updateCurrentPhotoPage(photos);
                });

        lazyUserModel = new GenericLazyDataModel<>(
                authenticationService.getUserDao(),
                exactMatchFilters
        );

        Map<String, Object> approvalFilters = new HashMap<>();
        approvalFilters.put("status", Arrays.asList("PENDING", "REJECTED"));

        lazyApprovalPhotoModel = new GenericLazyDataModel<Photo, Long>(
                photoService.getPhotoDao(),
                approvalFilters
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
        return tags.stream().map(Tag::getTagName).collect(Collectors.toList());
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
                            "Error", "An error occurred while deleting the user"));
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

    public String sessionStatus(User user) {
        System.out.println("Checking session for user: " + user.getUserName());
        SessionUtil.getActiveSessions().values().forEach(session -> {
            System.out.println("Session ID: " + session.getId());
            System.out.println("Username in session: " + session.getAttribute("username"));
        });
        return SessionUtil.getActiveSessions().values().stream()
                .anyMatch(session -> user.getUserName().equals(session.getAttribute("username")))
                ? "Active" : "Inactive";
    }

    public void killSessionForOneMinute() {
        String username = selectedUser.getUserName();
        BlockedUserManager.blockUser(username, duration);
        SessionUtil.invalidateSessionByUser(username);

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "User " + username + " has been logged out for " + duration + " minute.", null));
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public void prepareKillSession(User user) {
        this.selectedUser = user;
    }

    public void killSessionAndBlock() {
        if (selectedUser != null && userController.hasRole("admin")) {
            String username = selectedUser.getUserName();
            if (duration == null || duration <= 0) {
                duration = 1; // Default to 1 minute if invalid
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                                "Invalid Duration",
                                "Block duration set to 1 minute by default."));
            }
            System.out.println("Blocking user: " + username + " for " + duration + " minutes");
            BlockedUserManager.blockUser(username, duration);
            SessionUtil.invalidateSessionByUser(username);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Session Ended",
                            "User " + username + " has been logged out and blocked for " + duration + " minutes."));
        }
    }

    public void navigateToNextPhoto() {
        photoNavigationManager.navigateToNextPhoto();
        photoController.setSelectedPhoto(photoNavigationManager.getSelectedPhoto());
    }

    public void navigateToPreviousPhoto() {
        photoNavigationManager.navigateToPreviousPhoto();
        photoController.setSelectedPhoto(photoNavigationManager.getSelectedPhoto());
    }

    public void refreshLazyApprovalPhotoModel() {
        // Reinitialize the lazyApprovalPhotoModel to force a reload
        Map<String, Object> approvalFilters = new HashMap<>();
        approvalFilters.put("status", Arrays.asList("PENDING", "REJECTED"));
        lazyApprovalPhotoModel = new GenericLazyDataModel<Photo, Long>(
                photoService.getPhotoDao(),
                approvalFilters
        );
    }

    public boolean hasNextPhoto() {
        return photoNavigationManager.hasNextPhoto();
    }

    public boolean hasPreviousPhoto() {
        return photoNavigationManager.hasPreviousPhoto();
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

    public LazyDataModel<Photo> getLazyApprovalPhotoModel() {
        return lazyApprovalPhotoModel;
    }

    public void setLazyApprovalPhotoModel(LazyDataModel<Photo> lazyApprovalPhotoModel) {
        this.lazyApprovalPhotoModel = lazyApprovalPhotoModel;
    }
}