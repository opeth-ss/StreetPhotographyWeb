package com.example.model;

import javax.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "photo_tag")
public class PhotoTag extends BaseEntity{
    @ManyToOne
    @JoinColumn(name= "photo_id", nullable = false)
    private Photo photo;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist(){
        if (createdAt == null){
            createdAt = LocalDateTime.now();
        }
    }

    public Photo getPhoto() {
        return photo;
    }

    public void setPhoto(Photo photo) {
        this.photo = photo;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
}
