package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "photo")
public class Photo extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "pin_point", nullable = false)
    private String pinPoint;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    @Column(name = "average_photo_rating", nullable = false)
    private double averagePhotoRating = 0.0; // Default value set to 0

    @OneToMany(mappedBy = "photo", cascade = CascadeType.ALL)
    private List<Rating> ratings;

    @OneToMany(mappedBy = "photo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhotoTag> photoTags;

    @PrePersist
    public void prePersist() {
        if (uploadDate == null) {
            uploadDate = LocalDateTime.now();
        }
    }

    public Photo() {
        // Optional: Ensure that the default value is set in the constructor too
        this.averagePhotoRating = 0.0;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPinPoint() {
        return pinPoint;
    }

    public void setPinPoint(String pinPoint) {
        this.pinPoint = pinPoint;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public double getAveragePhotoRating() {
        return averagePhotoRating;
    }

    public void setAveragePhotoRating(double averagePhotoRating) {
        this.averagePhotoRating = averagePhotoRating;
    }

    public List<PhotoTag> getPhotoTags() {
        return photoTags;
    }

    public void setPhotoTags(List<PhotoTag> photoTags) {
        this.photoTags = photoTags;
    }

    public List<Rating> getRatings() {
        return ratings;
    }

    public void setRatings(List<Rating> ratings) {
        this.ratings = ratings;
    }
}
