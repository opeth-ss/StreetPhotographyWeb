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

    @Lob
    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    @Column(name="status", nullable = false)
    private String status;

    @Column(name= "approved_date", nullable = true)
    private LocalDateTime approvedDate;

    @ManyToOne
    @JoinColumn(name= "approved_by", nullable = true)
    private User approvedBy;

    @Column(name = "average_photo_rating", nullable = false)
    private double averagePhotoRating = 0.0;

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
        this.averagePhotoRating = 0.0;
        this.status = "PENDING";
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDateTime approvedDate) {
        this.approvedDate = approvedDate;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }
}
