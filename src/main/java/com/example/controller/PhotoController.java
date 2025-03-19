package com.example.controller;

import com.example.model.Photo;
import com.example.model.User;
import com.example.services.PhotoService;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.io.Serializable;

@Named("photoController")
@SessionScoped
public class PhotoController implements Serializable {
    private static final long serialVersionUID = 1L;
    private Photo photo = new Photo();
    private UploadedFile uploadedImage; // Add this field
    private static final String UPLOAD_DIR = "/home/opeth-ss/image";

    @Inject
    private PhotoService photoService;

    // Method to save photo
    public String savePhoto() {
        photo.setUser(getLoggedInUser());
        try {
            // Check if we have a file uploaded
            if (uploadedImage != null && uploadedImage.getContent() != null && uploadedImage.getContent().length > 0) {
                // Save the file and get the image path
                String imagePath = saveFile(uploadedImage);
                photo.setImagePath(imagePath);

                // Save the photo using the PhotoService
                photoService.savePhoto(photo);

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Photo uploaded successfully!", null));

                // Reset the photo object for next upload
                photo = new Photo();
                uploadedImage = null;

                return "success"; // or the appropriate navigation outcome
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "No image uploaded", null));
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error processing upload: " + e.getMessage(), null));
            return null;
        }
    }

    // Method to handle file upload event
    public void handleFileUpload(FileUploadEvent event) {
        uploadedImage = event.getFile();

        if (uploadedImage != null) {
            if (!uploadedImage.getContentType().startsWith("image/")) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Only image files are allowed", null));
                uploadedImage = null;
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "File uploaded successfully: " +
                                uploadedImage.getFileName(), null));
            }
        }
    }

    // Method to save the uploaded file to disk
    private String saveFile(UploadedFile uploadedFile) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + uploadedFile.getFileName();
        String filePath = UPLOAD_DIR + fileName;

        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(filePath);
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(uploadedFile.getContent());  // Write the uploaded file to disk
        }

        return "/uploads/" + fileName;  // Return the relative path to the image
    }

    // Getters and setters for Photo
    public Photo getPhoto() {
        return photo;
    }

    public void setPhoto(Photo photo) {
        this.photo = photo;
    }

    // Getters and setters for uploadedImage
    public UploadedFile getUploadedImage() {
        return uploadedImage;
    }

    public void setUploadedImage(UploadedFile uploadedImage) {
        this.uploadedImage = uploadedImage;
    }

    public User getLoggedInUser() {
        return (User) FacesContext.getCurrentInstance()
                .getExternalContext()
                .getSessionMap()
                .get("loggedInUser");
    }
}