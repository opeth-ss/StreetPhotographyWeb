package com.example.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "configuration")
public class Configuration extends BaseEntity {
    @Column(name = "min_rating", nullable = true)
    private Double minRating;

    @Column(name = "min_photos", nullable = true)
    private Integer minPhotos;

    public Double getMinRating() {
        return minRating;
    }

    public void setMinRating(Double minRating) {
        this.minRating = minRating;
    }

    public Integer getMinPhotos() {
        return minPhotos;
    }

    public void setMinPhotos(Integer minPhotos) {
        this.minPhotos = minPhotos;
    }
}
