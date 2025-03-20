package com.example.controller;

import com.example.model.Photo;
import com.example.services.PhotoService;
import org.primefaces.model.file.UploadedFile;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("photoController")
@SessionScoped
public class PhotoController implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(PhotoController.class.getName());
    private Photo photo = new Photo();
    private UploadedFile uploadedImage;
    private String csvTag;
    private static final String UPLOAD_DIR = "/home/opeth-ss/image/";

    @Inject
    private PhotoService photoService;

    @Inject
    private UserController userController;

    @Inject
    private PhotoTagController photoTagController;

    // Method to save photo
    public String savePhoto() {
        try {
            photo.setUser(userController.getUser());

            // Check if we have a file uploaded
            if (uploadedImage == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "No image selected", "Please select an image to upload"));
                return null;
            }

            if (uploadedImage.getContent() == null || uploadedImage.getContent().length == 0) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Empty image file", "The selected file appears to be empty"));
                return null;
            }

            // Validate file type
            String contentType = uploadedImage.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid file type", "Only image files are allowed"));
                return null;
            }

            // Save the file and get the image path
            String imagePath = saveFile(uploadedImage);
            photo.setImagePath(imagePath);

            // Log success of file save
            LOGGER.log(Level.INFO, "File saved successfully at: {0}", imagePath);

            // Save the photo using the PhotoService
            photoService.savePhoto(photo);

            if(photoTagController.saveTag(photo, csvTag, userController.getUser())){
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Photo uploaded successfully!"));
            }
            else{
                photoService.deletePhoto(photo);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Failed", "Photo uploaded failed (Tag couldn't be saved!"));
            }
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Photo uploaded successfully!"));

            // Reset the photo object for next upload
            photo = new Photo();
            uploadedImage = null;

            return "success"; // or the appropriate navigation outcome

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while saving photo", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Upload Error", "Error processing upload: " + e.getMessage()));
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "System Error", "An unexpected error occurred: " + e.getMessage()));
            return null;
        }
    }

    // Method to save the uploaded file to disk
    private String saveFile(UploadedFile uploadedFile) throws IOException {
        // Generate a unique filename to prevent collisions
        String fileName = System.currentTimeMillis() + "_" + uploadedFile.getFileName();

        // Make sure the directory exists
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("Could not create directory: " + UPLOAD_DIR);
            }
        }

        // Construct the full path
        String filePath = UPLOAD_DIR + File.separator + fileName;

        // Write the file
        File file = new File(filePath);
        try (OutputStream os = new FileOutputStream(file);
             InputStream is = uploadedFile.getInputStream()) {

            // Buffer for reading
            byte[] buffer = new byte[8192];
            int bytesRead;

            // Read from input and write to output
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            os.flush();
        }

        // Check if file was written successfully
        if (!file.exists() || file.length() == 0) {
            throw new IOException("Failed to write file or file is empty: " + filePath);
        }

        // Return the relative path to use in your application
        return "/uploads/" + fileName;
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

    public String getCsvTag() {
        return csvTag;
    }

    public void setCsvTag(String csvTag) {
        this.csvTag = csvTag;
    }
}