package com.example.controller;

import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.Tag;
import com.example.model.User;
import com.example.services.LeaderboardService;
import com.example.services.PhotoService;
import com.example.services.PhotoTagService;
import com.example.services.RatingService;
import com.example.utils.MessageHandler;
import com.example.utils.PhotoNavigationManager;
import org.primefaces.PrimeFaces;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.primefaces.model.file.UploadedFile;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Named("photoController")
@SessionScoped
public class PhotoController implements Serializable {
    private static final long serialVersionUID = 1L;
    private Photo photo = new Photo();
    private UploadedFile uploadedImage;
    private List<Tag> tags;
    private Photo selectedPhoto;
    private Integer ratingValue;
    private static final String IMAGE_DIRECTORY = "/home/opeth-ss/image";
    private static final long MAX_FILE_SIZE = 1048576; // 1MB
    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif");
    private List<String> tagInput = new ArrayList<>();
    private String filterLocation;
    private List<String> filterTags = new ArrayList<>();
    private Double filterMinRating;
    private String searchText;
    private Map<Long, Integer> ratingMap = new HashMap<>();
    private LazyDataModel<Photo> lazyPhotos;

    @Inject
    private PhotoService photoService;

    @Inject
    private UserController userController;

    @Inject
    private PhotoTagController photoTagController;

    @Inject
    private PhotoTagService photoTagService;

    @Inject
    private RatingService ratingService;

    @Inject
    private LeaderboardService leaderboardService;

    @Inject
    private PhotoNavigationManager photoNavigationManager;

    @Inject
    private AdminController adminController;

    @Inject
    private MessageHandler messageHandler;

    public PhotoController() {
    }

    @PostConstruct
    public void init() {
        Map<String, Object> exactMatchFilters = new HashMap<>();
        exactMatchFilters.put("status", "APPROVED");
        lazyPhotos = new GenericLazyDataModel<Photo, Long>(photoService.getPhotoDao(), exactMatchFilters) {
            @Override
            public List<Photo> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, FilterMeta> filters) {
                initRatingMap();
                exactMatchFilters.clear();
                exactMatchFilters.put("filterLocation", filterLocation);
                exactMatchFilters.put("filterTags", filterTags);
                exactMatchFilters.put("filterMinRating", filterMinRating);
                exactMatchFilters.put("searchText", searchText);
                exactMatchFilters.put("currentUser", userController.getUser());
                exactMatchFilters.put("status", "APPROVED");
                List<Photo> photos = super.load(first, pageSize, sortField, sortOrder, filters);
                photoNavigationManager.updateCurrentPhotoPage(photos);
                return photos;
            }
        };
    }

    public void handleSearch() {
        // No action needed; lazy loading handles it
    }

    public void clearSearch() {
        searchText = null;
        filterLocation = null;
        filterTags.clear();
        filterMinRating = null;
    }

    public void initRatingMap() {
        User currentUser = userController.getUser();
        if (currentUser != null) {
            List<Rating> ratings = ratingService.getRating(currentUser);
            for (Rating r : ratings) {
                ratingMap.put(r.getPhoto().getId(), (int) Math.round(r.getRating()));
            }
        }
    }

    public String savePhoto() {
        try {
            photoService.savePhoto(photo, uploadedImage, tagInput, userController.getUser(), IMAGE_DIRECTORY, MAX_FILE_SIZE, ALLOWED_TYPES);
            messageHandler.addInfoMessage("Success", "Photo uploaded successfully!", "photosGrid", ":growl");
            PrimeFaces.current().executeScript("PF('createPostDialog').hide()");
            photo = new Photo();
            uploadedImage = null;
            tags = null;
            tagInput = new ArrayList<>();
            return "success";
        } catch (IllegalArgumentException e) {
            messageHandler.addErrorMessage("Error", e.getMessage(), ":growl");
            return null;
        } catch (IOException e) {
            messageHandler.addExceptionMessage("Upload Error", e, ":growl");
            return null;
        } catch (SecurityException e) {
            messageHandler.addExceptionMessage("Permission Error", e, ":growl");
            return null;
        } catch (Exception e) {
            messageHandler.addExceptionMessage("Unexpected Error", e, ":growl");
            return null;
        }
    }

    public List<Photo> getUserPhotos() {
        try {
            if (userController.getUser() == null) {
                return new ArrayList<>();
            }
            return photoService.getPhotosByUser(userController.getUser());
        } catch (Exception e) {
            messageHandler.addExceptionMessage("Error", e, ":growl");
            return new ArrayList<>();
        }
    }

    public List<String> completeTag(String query) {
        List<Tag> tags = photoTagService.getAllTags(query);
        return tags.stream().map(Tag::getTagName).collect(Collectors.toList());
    }

    public List<String> getPhotoTagNames(Photo photo) {
        List<Tag> tags = photoService.getPhotoTags(photo);
        return tags.stream().map(Tag::getTagName).collect(Collectors.toList());
    }

    public void ratingMethod(Photo photo) {
        try {
            Integer currentRating = ratingMap.get(photo.getId());
            if (currentRating == null && ratingValue != null) {
                currentRating = ratingValue;
            }
            if (currentRating == null) {
                return;
            }

            Photo updatedPhoto = photoService.ratePhoto(photo, userController.getUser(), currentRating, ratingMap);
            if (selectedPhoto != null && selectedPhoto.getId().equals(photo.getId())) {
                selectedPhoto = updatedPhoto;
            }

            messageHandler.addInfoMessage("Success", "Rating submitted successfully", ":photoDetailForm", ":photosGrid", ":growl");
            ratingValue = null;
        } catch (IllegalStateException | IllegalArgumentException e) {
            messageHandler.addErrorMessage("Error", e.getMessage(), ":growl");
        } catch (Exception e) {
            messageHandler.addExceptionMessage("Error", e, ":growl");
        }
    }

    @Transactional
    public void deletePhoto(Photo photo) {
        try {
            photoService.deletePhoto(photo, userController.getUser());
            messageHandler.addInfoMessage("Photo Deleted", "Photo was deleted successfully", ":growl");
            ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
            ec.redirect(ec.getRequestContextPath() + ec.getRequestServletPath());
        } catch (SecurityException e) {
            messageHandler.addErrorMessage("Delete Failed", e.getMessage(), ":growl");
        } catch (Exception e) {
            messageHandler.addExceptionMessage("Error", e, ":growl");
        }
    }

    public void updatePhoto(Photo photo) {
        try {
            photoService.updatePhoto(photo, uploadedImage, userController.getUser(), IMAGE_DIRECTORY, MAX_FILE_SIZE, ALLOWED_TYPES);
            messageHandler.addInfoMessage("Photo Updated", "Photo was updated successfully", "photosGrid", ":growl");
            uploadedImage = null;
        } catch (IllegalArgumentException | SecurityException e) {
            messageHandler.addErrorMessage("Error", e.getMessage(), ":growl");
        } catch (IOException e) {
            messageHandler.addExceptionMessage("Error", e, ":growl");
        }
    }

    public boolean deleteAllbyUser(User user) {
        try {
            return photoService.deleteAllPhotosByUser(user, userController.getUser());
        } catch (Exception e) {
            messageHandler.addExceptionMessage("Error", e, ":growl");
            return false;
        }
    }

    public void navigateToNextPhoto() {
        photoNavigationManager.navigateToNextPhoto();
        this.selectedPhoto = photoNavigationManager.getSelectedPhoto();
    }

    public void navigateToPreviousPhoto() {
        photoNavigationManager.navigateToPreviousPhoto();
        this.selectedPhoto = photoNavigationManager.getSelectedPhoto();
    }

    public boolean hasNextPhoto() {
        return photoNavigationManager.hasNextPhoto();
    }

    public boolean hasPreviousPhoto() {
        return photoNavigationManager.hasPreviousPhoto();
    }

    public Rating hasUserRated(Photo photo) {
        if (photo == null || userController.getUser() == null) {
            return null;
        }
        return ratingService.userRatingExists(userController.getUser(), photo);
    }

    public void approvePhoto(Photo photo) {
        try {
            photoService.approvePhoto(photo, userController.getUser());
            adminController.refreshLazyApprovalPhotoModel();
            messageHandler.addInfoMessage("Approved", "The photo has been approved", ":growl");
        } catch (IllegalArgumentException | SecurityException e) {
            messageHandler.addErrorMessage("Error", e.getMessage(), ":growl");
        }
    }

    public void rejectPhoto(Photo photo) {
        try {
            photoService.rejectPhoto(photo, userController.getUser());
            adminController.refreshLazyApprovalPhotoModel();
            messageHandler.addErrorMessage("Rejected", "The photo has been rejected", ":growl");
        } catch (IllegalArgumentException | SecurityException e) {
            messageHandler.addErrorMessage("Error", e.getMessage(), ":growl");
        }
    }

    public void pendingPhoto(Photo photo) {
        try {
            photoService.pendingPhoto(photo, userController.getUser());
            adminController.refreshLazyApprovalPhotoModel();
            messageHandler.addWarnMessage("Pending", "The status has been updated", ":growl");
        } catch (IllegalArgumentException | SecurityException e) {
            messageHandler.addErrorMessage("Error", e.getMessage(), ":growl");
        }
    }

    public void reSetRating() {
        ratingValue = null;
    }

    public boolean isPhotoOfCurrentUser(Photo photo) {
        User currentUser = userController.getUser();
        return currentUser != null && currentUser.getId().equals(photo.getUser().getId());
    }

    public Photo getPhoto() {
        return photo;
    }

    public void setPhoto(Photo photo) {
        this.photo = photo;
    }

    public UploadedFile getUploadedImage() {
        return uploadedImage;
    }

    public void setUploadedImage(UploadedFile uploadedImage) {
        this.uploadedImage = uploadedImage;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public Photo getSelectedPhoto() {
        return selectedPhoto;
    }

    public void setSelectedPhoto(Photo selectedPhoto) {
        this.selectedPhoto = selectedPhoto;
        photoNavigationManager.setSelectedPhoto(selectedPhoto);
    }

    public LazyDataModel<Photo> getLazyPhotos() {
        return lazyPhotos;
    }

    public List<String> getAvailableLocations() {
        return photoService.getAllPinPoints();
    }

    public Integer getRatingValue() {
        return ratingValue;
    }

    public void setRatingValue(Integer ratingValue) {
        this.ratingValue = ratingValue;
    }

    public List<String> getTagInput() {
        return tagInput;
    }

    public void setTagInput(List<String> tagInput) {
        this.tagInput = tagInput;
    }

    public String getFilterLocation() {
        return filterLocation;
    }

    public void setFilterLocation(String filterLocation) {
        this.filterLocation = filterLocation;
    }

    public List<String> getFilterTags() {
        return filterTags;
    }

    public void setFilterTags(List<String> filterTags) {
        this.filterTags = filterTags;
    }

    public Double getFilterMinRating() {
        return filterMinRating;
    }

    public void setFilterMinRating(Double filterMinRating) {
        this.filterMinRating = filterMinRating;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public Map<Long, Integer> getRatingMap() {
        return ratingMap;
    }

    public void setRatingMap(Map<Long, Integer> ratingMap) {
        this.ratingMap = ratingMap;
    }
}