package com.example.controller;

import com.example.model.Photo;
import com.example.model.Tag;
import com.example.model.User;
import com.example.services.AuthenticationService;
import com.example.services.ConfigurationService;
import com.example.services.PhotoService;
import com.example.utils.BlockedUserManager;
import com.example.utils.SessionUtil;
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
import java.util.stream.Collectors;

@Named("adminController")
@SessionScoped
public class AdminController implements Serializable {
    private static final long serialVersionUID = 1L;
    private User selectedUser;
    private Double rating;
    private Integer totalPost;
    private Integer duration = 1;
    private List<Photo> currentPhotoPage = new ArrayList<>();

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

        // Register callback to update currentPhotoPage
        ((GenericLazyDataModel<Photo, Long>) lazyPhotoModel).setOnDataLoadCallback(this::updateCurrentPhotoPage);

        lazyUserModel = new GenericLazyDataModel<>(
                authenticationService.getUserDao(),
                exactMatchFilters
        );
    }

    // Callback method to update currentPhotoPage
    private void updateCurrentPhotoPage(List<Photo> photos) {
        this.currentPhotoPage = new ArrayList<>(photos);
        System.out.println("Updated currentPhotoPage with " + photos.size() + " photos: " +
                photos.stream().map(p -> String.valueOf(p.getId())).collect(Collectors.joining(", ")));
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
                new FacesMessage(FacesMessage.SEVERITY_INFO, "User " + username + " has been logged out for " + duration + " minute.", null));
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
            BlockedUserManager.blockUser(username, duration);
            SessionUtil.invalidateSessionByUser(username);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Session Ended",
                            "User " + username + " has been logged out and blocked for " + duration + " minutes."));
        }
    }

    public List<Photo> getCurrentPhotoPage() {
        return currentPhotoPage;
    }

    public void setCurrentPhotoPage(List<Photo> currentPhotoPage) {
        this.currentPhotoPage = currentPhotoPage;
    }

    public void navigateToNextPhoto() {
        if (photoController.getSelectedPhoto() != null) {
            int index = currentPhotoPage.indexOf(photoController.getSelectedPhoto());
            if (index >= 0 && index < currentPhotoPage.size() - 1) {
                photoController.setSelectedPhoto(currentPhotoPage.get(index + 1));
            }
        }
    }

    public void navigateToPreviousPhoto() {
        if (photoController.getSelectedPhoto() != null) {
            int index = currentPhotoPage.indexOf(photoController.getSelectedPhoto());
            if (index > 0) {
                photoController.setSelectedPhoto(currentPhotoPage.get(index - 1));
            }
        }
    }

    public boolean hasNextPhoto() {
        if (photoController.getSelectedPhoto() != null) {
            int index = currentPhotoPage.indexOf(photoController.getSelectedPhoto());
            System.out.println("hasNextPhoto: selectedPhoto=" +
                    (photoController.getSelectedPhoto() != null ? photoController.getSelectedPhoto().getId() : "null") +
                    ", index=" + index + ", currentPhotoPageSize=" + currentPhotoPage.size());
            return index >= 0 && index < currentPhotoPage.size() - 1;
        }
        System.out.println("hasNextPhoto: selectedPhoto is null");
        return false;
    }

    public boolean hasPreviousPhoto() {
        if (photoController.getSelectedPhoto() != null) {
            int index = currentPhotoPage.indexOf(photoController.getSelectedPhoto());
            System.out.println("hasPreviousPhoto: selectedPhoto=" +
                    (photoController.getSelectedPhoto() != null ? photoController.getSelectedPhoto().getId() : "null") +
                    ", index=" + index);
            return index > 0;
        }
        System.out.println("hasPreviousPhoto: selectedPhoto is null");
        return false;
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