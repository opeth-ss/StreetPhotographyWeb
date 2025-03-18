
package com.example.model;

import javax.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "rating")

public class Rating extends BaseEntity{
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne
    @JoinColumn(name = "photo_id", nullable = false)
    private Photo photo;
    @Column(name = "rating", nullable = false)
    private double rating;
    @Column(name= "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist(){
        if (createdAt == null){
            createdAt = LocalDateTime.now();
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Photo getPhoto() {
        return photo;
    }

    public void setPhoto(Photo photo) {
        this.photo = photo;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
}
