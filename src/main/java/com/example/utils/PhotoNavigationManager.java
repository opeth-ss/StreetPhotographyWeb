package com.example.utils;

import com.example.model.Photo;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PhotoNavigationManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Photo> currentPhotoPage = new ArrayList<>();
    private Photo selectedPhoto;

    public void updateCurrentPhotoPage(List<Photo> photos) {
        this.currentPhotoPage = new ArrayList<>(photos);
        // Reset selectedPhoto if it's not in the new page
        if (selectedPhoto != null && !currentPhotoPage.contains(selectedPhoto)) {
            selectedPhoto = currentPhotoPage.isEmpty() ? null : currentPhotoPage.get(0);
        }
    }

    public void setSelectedPhoto(Photo photo) {
        this.selectedPhoto = photo;
    }

    public Photo getSelectedPhoto() {
        return selectedPhoto;
    }

    public List<Photo> getCurrentPhotoPage() {
        return currentPhotoPage;
    }

    public void navigateToNextPhoto() {
        if (selectedPhoto == null && !currentPhotoPage.isEmpty()) {
            selectedPhoto = currentPhotoPage.get(0);
            return;
        }
        if (selectedPhoto != null) {
            int index = currentPhotoPage.indexOf(selectedPhoto);
            if (index >= 0 && index < currentPhotoPage.size() - 1) {
                selectedPhoto = currentPhotoPage.get(index + 1);
            }
        }
    }

    public void navigateToPreviousPhoto() {
        if (selectedPhoto == null && !currentPhotoPage.isEmpty()) {
            selectedPhoto = currentPhotoPage.get(0);
            return;
        }
        if (selectedPhoto != null) {
            int index = currentPhotoPage.indexOf(selectedPhoto);
            if (index > 0) {
                selectedPhoto = currentPhotoPage.get(index - 1);
            }
        }
    }

    public boolean hasNextPhoto() {
        if (selectedPhoto == null) {
            return !currentPhotoPage.isEmpty();
        }
        int index = currentPhotoPage.indexOf(selectedPhoto);
        return index >= 0 && index < currentPhotoPage.size() - 1;
    }

    public boolean hasPreviousPhoto() {
        if (selectedPhoto == null) {
            return !currentPhotoPage.isEmpty();
        }
        int index = currentPhotoPage.indexOf(selectedPhoto);
        return index > 0;
    }
}