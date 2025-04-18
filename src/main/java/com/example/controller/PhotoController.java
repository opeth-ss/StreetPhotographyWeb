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
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
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
    private RatingController ratingController;

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
        // Add filter to only include photos with status "APPROVED"
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

    // Handle search action
    public void handleSearch() {
        // No need for explicit action here; the lazy loading will handle it via AJAX update
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
            // Set the user for the photo
            photo.setUser(userController.getUser());

            // Validate that an image is uploaded
            if (uploadedImage == null) {
                messageHandler.addErrorMessage("No image selected", "Please select an image to upload", ":growl");
                return null;
            }

            // Check if the uploaded file is empty
            if (uploadedImage.getSize() == 0) {
                messageHandler.addErrorMessage("Empty image file", "The selected file appears to be empty", ":growl");
                return null;
            }

            // Validate file size (max 1MB)
            if (uploadedImage.getSize() > MAX_FILE_SIZE) {
                messageHandler.addErrorMessage("File too large", "Maximum file size is 1MB", ":growl");
                return null;
            }

            // Validate file type
            String contentType = uploadedImage.getContentType();
            if (!ALLOWED_TYPES.contains(contentType)) {
                messageHandler.addErrorMessage("Invalid file type", "Only JPEG, PNG, and GIF files are allowed", ":growl");
                return null;
            }

            // Save the uploaded file and set the image path
            String imagePath = saveFile(uploadedImage);
            photo.setImagePath(imagePath);

            // Persist the photo to the database
            photoService.savePhoto(photo);

            // Handle tags
            if (tagInput != null && !tagInput.isEmpty()) {
                tags = tagInput.stream()
                        .map(tagName -> photoTagService.findTagByName(tagName))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                if (tags.isEmpty()) {
                    messageHandler.addErrorMessage("Invalid Tags", "No valid tags were selected.", ":growl");
                    photoService.deletePhoto(photo);
                    new File(imagePath).delete();
                    return null;
                }

                // Save tags (single call)
                if (!photoTagController.saveTag(photo, tags, userController.getUser())) {
                    // Rollback if tag saving fails
                    photoService.deletePhoto(photo);
                    new File(imagePath).delete();
                    messageHandler.addErrorMessage("Failed", "Photo upload failed (Tags couldn't be saved)!", ":growl");
                    return null;
                }
            }

            // Success message and UI updates
            messageHandler.addInfoMessage("Success", "Photo uploaded successfully!", "photosGrid", ":growl");
            PrimeFaces.current().executeScript("PF('createPostDialog').hide()");

            // Reset fields for the next upload
            photo = new Photo();
            uploadedImage = null;
            tags = null;
            tagInput = new ArrayList<>();

            return "success";

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
        return tags.stream()
                .map(Tag::getTagName)
                .collect(Collectors.toList());
    }

    public List<String> getPhotoTagNames(Photo photo) {
        List<Tag> tags = photoService.getPhotoTags(photo);
        List<String> tagNames = new ArrayList<>();
        for (Tag tag : tags) {
            tagNames.add(tag.getTagName());  // Use the toString() method to get the tag's name or description
        }
        return tagNames;
    }

    private String saveFile(UploadedFile uploadedFile) throws IOException {
        String extension = getFileExtension(uploadedFile.getFileName());
        String fileName = UUID.randomUUID().toString() + extension;
        String filePath = IMAGE_DIRECTORY + File.separator + fileName;

        File directory = new File(IMAGE_DIRECTORY);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create directory: " + IMAGE_DIRECTORY);
        }
        if (!directory.canWrite()) {
            throw new SecurityException("No write permission for directory: " + IMAGE_DIRECTORY);
        }

        File file = new File(filePath);
        try (InputStream is = uploadedFile.getInputStream();
             OutputStream os = Files.newOutputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }

        if (!file.exists() || file.length() == 0) {
            file.delete();
            throw new IOException("Failed to write file or file is empty: " + filePath);
        }

        return filePath;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    public void ratingMethod(Photo photo) {
        try {
            User currentUser = userController.getUser();

            if (currentUser == null) {
                messageHandler.addErrorMessage("Error", "Please login to rate photos", ":growl");
                return;
            }

            // Get the rating value as Integer (could be null)
            Integer currentRating = ratingMap.get(photo.getId());

            // Use the general ratingValue if no mapped value exists
            if (currentRating == null && ratingValue != null) {
                currentRating = ratingValue;
            }

            // Skip if no rating is provided
            if (currentRating == null) {
                return; // Silently return instead of showing an error
            }

            // Validate rating (1-5)
            if (currentRating < 1 || currentRating > 5) {
                messageHandler.addErrorMessage("Error", "Invalid rating value", ":growl");
                return;
            }

            // Check if the user has already rated this photo
            Rating existingRating = ratingService.userRatingExists(currentUser, photo);
            if (existingRating != null) {
                ratingController.updateExistingRating(photo, (double) currentRating);
            } else {
                ratingController.addRating(currentUser, photo, (double) currentRating);
            }

            // Update the map with the new rating
            ratingMap.put(photo.getId(), currentRating);

            // Refresh photo data
            photo = photoService.refreshPhoto(photo);
            if (selectedPhoto != null && selectedPhoto.getId().equals(photo.getId())) {
                selectedPhoto = photoService.refreshPhoto(selectedPhoto);
            }

            messageHandler.addInfoMessage("Success", "Rating submitted successfully", ":photoDetailForm", ":photosGrid", ":growl");
            ratingValue = null;

        } catch (Exception e) {
            messageHandler.addExceptionMessage("Error", e, ":growl");
        }
    }

    @Transactional
    public void deletePhoto(Photo photo) {
        try {
            if (photo.getUser().getUserName().equals(userController.getUser().getUserName())
                    || userController.hasRole("admin")) {

                // Get the photo owner
                User photoOwner = photo.getUser();

                // Delete the photo
                photoService.deletePhoto(photo);

                // Recalculate the photo owner's rating based on remaining photos
                ratingController.recalculateUserRating(photoOwner);

                // Update the leaderboard for the photo owner
                leaderboardService.updateLeaderBoard(photoOwner);

                messageHandler.addInfoMessage("Photo Deleted", "Photo was deleted successfully", ":growl");

                // Reload the page
                ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
                ec.redirect(ec.getRequestContextPath() + ec.getRequestServletPath());
            } else {
                messageHandler.addErrorMessage("Delete Failed", "You can only delete your own photos", ":growl");
            }
        } catch (Exception e) {
            messageHandler.addExceptionMessage("Error", e, ":growl");
        }
    }

    public void updatePhoto(Photo photo) {
        try {
            if (photo.getUser().getUserName().equals(userController.getUser().getUserName()) ||
                    userController.hasRole("admin")) {
                // Only update image if a new one was uploaded
                if (uploadedImage != null && uploadedImage.getSize() > 0) {
                    // Validate the new image
                    if (uploadedImage.getSize() > MAX_FILE_SIZE) {
                        messageHandler.addErrorMessage("File too large", "Maximum file size is 1MB", ":growl");
                        return;
                    }

                    String contentType = uploadedImage.getContentType();
                    if (!ALLOWED_TYPES.contains(contentType)) {
                        messageHandler.addErrorMessage("Invalid file type", "Only JPEG, PNG, and GIF files are allowed", ":growl");
                        return;
                    }

                    // Save the new image
                    String imagePath = saveFile(uploadedImage);
                    photo.setImagePath(imagePath);
                    photo.setStatus("PENDING");
                }

                // Update the photo details
                photoService.updatePhoto(photo);

                messageHandler.addInfoMessage("Photo Updated", "Photo was updated successfully", "photosGrid", ":growl");

                // Reset the uploaded file
                uploadedImage = null;
            } else {
                messageHandler.addErrorMessage("Update Failed", "You can only update your own photos", ":growl");
            }
        } catch (Exception e) {
            messageHandler.addExceptionMessage("Error", e, ":growl");
        }
    }

    public boolean deleteAllbyUser(User user) {
        List<Photo> userPhotos = photoService.getPhotosByUser(user);
        for (Photo photo : userPhotos) {
            deletePhoto(photo);
        }
        return true;
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
        if (photo != null && userController.hasRole("ADMIN")) {
            photo.setStatus("APPROVED");
            photo.setApprovedBy(userController.getUser());
            photo.setApprovedDate(LocalDateTime.now());
            photoService.updatePhoto(photo);
            adminController.refreshLazyApprovalPhotoModel(); // Refresh the model
            messageHandler.addInfoMessage("Approved", "The photo has been approved", ":growl");
        } else {
            messageHandler.addErrorMessage("Empty Photo", "There is no photo to approve", ":growl");
        }
    }

    public void rejectPhoto(Photo photo) {
        if (photo != null && userController.hasRole("ADMIN")) {
            photo.setStatus("REJECTED");
            photo.setApprovedBy(userController.getUser());
            photo.setApprovedDate(LocalDateTime.now());
            photoService.updatePhoto(photo);
            adminController.refreshLazyApprovalPhotoModel(); // Refresh the model
            messageHandler.addErrorMessage("Rejected", "The photo has been rejected", ":growl");
        } else {
            messageHandler.addErrorMessage("Empty Photo", "There is no photo to approve", ":growl");
        }
    }

    public void pendingPhoto(Photo photo) {
        if (photo != null && userController.hasRole("ADMIN")) {
            photo.setStatus("PENDING");
            photo.setApprovedBy(userController.getUser());
            photo.setApprovedDate(LocalDateTime.now());
            photoService.updatePhoto(photo);
            adminController.refreshLazyApprovalPhotoModel(); // Refresh the model
            messageHandler.addWarnMessage("Pending", "The status has been updated", ":growl");
        } else {
            messageHandler.addErrorMessage("Empty Photo", "There is no photo to approve", ":growl");
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