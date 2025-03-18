package com.example.model;


import javax.persistence.*;

@Entity
@Table(name = "leaderboard")
public class Leaderboard extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "user_rank",nullable = false)
    private Long userRank;
    @Column(name = "total_ratings",nullable = false)
    private Long totalRatings;
    @Column(name = "average_rating",nullable = false)
    private Double averageRating;
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getUserRank() {
        return userRank;
    }

    public void setUserRank(Long userRank) {
        this.userRank = userRank;
    }

    public Long getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Long totalRatings) {
        this.totalRatings = totalRatings;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }
    
    
}
