package com.example.controller;

import com.example.model.Photo;
import com.example.model.Rating;
import com.example.model.Tag;
import com.example.model.User;
import com.example.services.PhotoService;
import com.example.services.RatingService;
import org.primefaces.PrimeFaces;
import org.primefaces.model.file.UploadedFile;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Named("photoController")
@SessionScoped
public class PhotoController implements Serializable {
    private static final long serialVersionUID = 1L;
    private Photo photo = new Photo();
    private UploadedFile uploadedImage;
    private String csvTag;
    private Photo selectedPhoto;
    private Integer ratingValue;
    private static final String IMAGE_DIRECTORY = "/home/opeth-ss/image";
    private static final long MAX_FILE_SIZE = 1048576; // 1MB
    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif");


    @Inject
    private PhotoService photoService;

    @Inject
    private UserController userController;

    @Inject
    private PhotoTagController photoTagController;

    @Inject
    private RatingController ratingController;

    @Inject
    private RatingService ratingService;


    public String savePhoto() {
        try {
            photo.setUser(userController.getUser());

            if (uploadedImage == null) {
                addErrorMessage("No image selected", "Please select an image to upload");
                return null;
            }

            if (uploadedImage.getSize() == 0) {
                addErrorMessage("Empty image file", "The selected file appears to be empty");
                return null;
            }

            if (uploadedImage.getSize() > MAX_FILE_SIZE) {
                addErrorMessage("File too large", "Maximum file size is 1MB");
                return null;
            }

            String contentType = uploadedImage.getContentType();
            if (!ALLOWED_TYPES.contains(contentType)) {
                addErrorMessage("Invalid file type", "Only JPEG, PNG, and GIF files are allowed");
                return null;
            }

            String imagePath = saveFile(uploadedImage);
            photo.setImagePath(imagePath);
            photoService.savePhoto(photo);

            if (photoTagController.saveTag(photo, csvTag, userController.getUser())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Photo uploaded successfully!"));
                PrimeFaces.current().ajax().update("photosGrid");
                PrimeFaces.current().executeScript("PF('createPostDialog').hide()");
            } else {
                photoService.deletePhoto(photo);
                new File(imagePath).delete();
                addErrorMessage("Failed", "Photo upload failed (Tag couldn't be saved)!");
                return null;
            }

            photo = new Photo();
            uploadedImage = null;
            csvTag = null;
            return "success";

        } catch (IOException e) {
            addErrorMessage("Upload Error", "Error processing upload: " + e.getMessage());
            return null;
        } catch (SecurityException e) {
            addErrorMessage("Permission Error", "No permission to save file: " + e.getMessage());
            return null;
        }
    }

    public List<Photo> getLatestPhotos() {
        try {
            if (userController.getUser() == null) {
                return new ArrayList<>();
            }

            List<Photo> allPhotos = photoService.getLatestPosts();

            List<Photo> filteredPhotos = allPhotos.stream()
                    .filter(photo -> !isPhotoOfCurrentUser(photo))
                    .collect(Collectors.toList());

            return filteredPhotos;

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
             OutputStream os = new FileOutputStream(file)) {
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
        ratingController.addRating(userController.getUser(), photo, ratingValue.doubleValue());
        this.ratingValue = null;
        PrimeFaces.current().ajax().update("photoDetailForm", "growl");
        PrimeFaces.current().ajax().update("photosGrid");
    }

    public void deletePhoto(Photo photo) {
        try {
            if (photo.getUser().getUserName().equals(userController.getUser().getUserName()) || userController.hasRole("admin")) {
                ratingController.reduceUserRating(photo.getUser(), photo.getAveragePhotoRating());
                photoService.deletePhoto(photo);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Photo Deleted", "Photo was deleted successfully"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Delete Failed", "You can only delete your own photos"));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_FATAL,
                            "Error", "An error occurred while deleting the photo"));
        }
    }

    public void updatePhoto(Photo photo) {
        try {
            if (photo.getUser().getUserName().equals(userController.getUser().getUserName()) || userController.hasRole("admin")) {
                // Update the photo details
                photoService.updatePhoto(photo);

                // Update the tags
                photoTagController.saveTag(photo, csvTag, userController.getUser());

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Photo Updated", "Photo was updated successfully"));
                PrimeFaces.current().ajax().update("photosGrid");
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

    public boolean isPhotoOfCurrentUser(Photo photo) {
        User currentUser = userController.getUser();
        return currentUser != null && currentUser.getId().equals(photo.getUser().getId()); // assuming Photo has a 'User' relationship
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

    public String getCsvTag() {
        return csvTag;
    }

    public void setCsvTag(String csvTag) {
        this.csvTag = csvTag;
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
}