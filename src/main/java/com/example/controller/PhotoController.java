package com.example.controller;

import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.Tag;
import com.example.model.User;
import com.example.services.LeaderboardService;
import com.example.services.PhotoService;
import com.example.services.PhotoTagService;
import com.example.services.RatingService;
import org.primefaces.PrimeFaces;
import org.primefaces.model.file.UploadedFile;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import javax.faces.context.ExternalContext;
import javax.transaction.Transactional;

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

    public String savePhoto() {
        try {
            // Set the user for the photo
            photo.setUser(userController.getUser());

            // Validate that an image is uploaded
            if (uploadedImage == null) {
                addErrorMessage("No image selected", "Please select an image to upload");
                return null;
            }

            // Check if the uploaded file is empty
            if (uploadedImage.getSize() == 0) {
                addErrorMessage("Empty image file", "The selected file appears to be empty");
                return null;
            }

            // Validate file size (max 1MB)
            if (uploadedImage.getSize() > MAX_FILE_SIZE) {
                addErrorMessage("File too large", "Maximum file size is 1MB");
                return null;
            }

            // Validate file type
            String contentType = uploadedImage.getContentType();
            if (!ALLOWED_TYPES.contains(contentType)) {
                addErrorMessage("Invalid file type", "Only JPEG, PNG, and GIF files are allowed");
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
                    addErrorMessage("Invalid Tags", "No valid tags were selected.");
                    photoService.deletePhoto(photo);
                    new File(imagePath).delete();
                    return null;
                }

                // Save tags (single call)
                if (!photoTagController.saveTag(photo, tags, userController.getUser())) {
                    // Rollback if tag saving fails
                    photoService.deletePhoto(photo);
                    new File(imagePath).delete();
                    addErrorMessage("Failed", "Photo upload failed (Tags couldn't be saved)!");
                    return null;
                }
            }

            // Success message and UI updates
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Photo uploaded successfully!"));
            PrimeFaces.current().ajax().update("photosGrid");
            PrimeFaces.current().executeScript("PF('createPostDialog').hide()");

            // Reset fields for the next upload
            photo = new Photo();
            uploadedImage = null;
            tags = null;
            tagInput = new ArrayList<>();

            return "success";

        } catch (IOException e) {
            addErrorMessage("Upload Error", "Error processing upload: " + e.getMessage());
            return null;
        } catch (SecurityException e) {
            addErrorMessage("Permission Error", "No permission to save file: " + e.getMessage());
            return null;
        } catch (Exception e) {
            addErrorMessage("Unexpected Error", "An unexpected error occurred: " + e.getMessage());
            return null;
        }
    }

    public List<Photo> getLatestPhotos() {
        try {
            if (userController.getUser() == null) {
                return new ArrayList<>();
            }

            List<Photo> allPhotos = photoService.getLatestPosts();

            return allPhotos.stream()
                    .filter(photo1 -> !isPhotoOfCurrentUser(photo1))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            addErrorMessage("Error", "Could not load photos: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Photo> getUserPhotos() {
        try {
            if (userController.getUser() == null) {
                return new ArrayList<>();
            }
            return photoService.getPhotosByUser(userController.getUser());

        } catch (Exception e) {
            addErrorMessage("Error", "Could not load photos: " + e.getMessage());
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
            FacesContext context = FacesContext.getCurrentInstance();
            User currentUser = userController.getUser();

            if (currentUser == null) {
                context.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR, "Error", "Please login to rate photos"));
                return;
            }

            Rating existingRating = ratingService.userRatingExists(currentUser, photo);
            if (existingRating != null) {
                PrimeFaces.current().executeScript("PF('reratingDialog').show()");
                return;
            }

            if (ratingValue == null || ratingValue < 1 || ratingValue > 5) {
                context.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR, "Error", "Please select a valid rating"));
                return;
            }

            ratingController.addRating(currentUser, photo, ratingValue.doubleValue());

            // Refresh photo data
            photo = photoService.refreshPhoto(photo);
            if (selectedPhoto != null && selectedPhoto.getId().equals(photo.getId())) {
                selectedPhoto = photoService.refreshPhoto(selectedPhoto);
            }

            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO, "Success", "Rating submitted successfully"));

            PrimeFaces.current().ajax().update(":photoDetailForm", ":photosGrid", ":growl");
            ratingValue = null;

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Error", "Failed to process rating: " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl");
        }
    }

    public void updateExistingRating(Photo photo){
        ratingController.updateExistingRating(photo, ratingValue.doubleValue());
    }

    @Transactional
    public void deletePhoto(Photo photo) {
        try {
            if (photo.getUser().getUserName().equals(userController.getUser().getUserName())
                    || userController.hasRole("admin")) {

                // Get the photo owner
                User photoOwner = photo.getUser();

                // Get all ratings for this photo
                List<Rating> photoRatings = ratingService.getRatingByPhoto(photo);

                // Delete the photo first
                photoService.deletePhoto(photo);

                // Adjust user ratings and leaderboard for each rater
                for (Rating rating : photoRatings) {
                    ratingController.reduceUserRating(rating.getUser(), rating.getRating());
                }

                // Update the leaderboard for the photo owner
                leaderboardService.updateLeaderBoard(photoOwner);

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Photo Deleted", "Photo was deleted successfully"));

                // Reload the page
                ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
                ec.redirect(ec.getRequestContextPath() + ec.getRequestServletPath());
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Delete Failed", "You can only delete your own photos"));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_FATAL, "Error", "An error occurred while deleting the photo"));
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
                        addErrorMessage("File too large", "Maximum file size is 1MB");
                        return;
                    }

                    String contentType = uploadedImage.getContentType();
                    if (!ALLOWED_TYPES.contains(contentType)) {
                        addErrorMessage("Invalid file type", "Only JPEG, PNG, and GIF files are allowed");
                        return;
                    }

                    // Save the new image
                    String imagePath = saveFile(uploadedImage);
                    photo.setImagePath(imagePath);
                }

                // Update the photo details
                photoService.updatePhoto(photo);

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Photo Updated", "Photo was updated successfully"));
                PrimeFaces.current().ajax().update("photosGrid");

                // Reset the uploaded file
                uploadedImage = null;
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Update Failed", "You can only update your own photos"));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_FATAL,
                            "Error", "An error occurred while updating the photo: " + e.getMessage()));
        }
    }

    public boolean deleteAllbyUser(User user){
        List<Photo> userPhotos = photoService.getPhotosByUser(user);
        for(Photo photo: userPhotos){
            deletePhoto(photo);
        }
        return true;
    }
    private void addErrorMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }

    public Rating hasUserRated(Photo photo) {
        if (photo == null || userController.getUser() == null) {
            return null;
        }
        return ratingService.userRatingExists(userController.getUser(), photo);
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
}