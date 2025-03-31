package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="user")
public class User extends BaseEntity {
    @Column(name = "user_name",nullable = false, unique = true )
    private String userName;
    @Column(name = "email",nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(name = "join_date", nullable = false)
    private LocalDateTime joinDate;
    @Column(name = "average_rating", nullable = false)
    private double averageRating = 0.0;
    @Column(nullable = false)
    private String role = "user";
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Rating> ratings;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Photo> photos;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Leaderboard> leaderboards;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<PhotoTag> photoTags;
    @PrePersist
    public void prePersist(){
        if (joinDate == null){
            joinDate = LocalDateTime.now();
        }
        if (role == null) {
            role = "user";
        }
        if (averageRating == 0.0) {
            averageRating = 0.0;
        }
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDateTime joinDate) {
        this.joinDate = joinDate;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
