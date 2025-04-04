package com.example.controller;

import com.example.model.Photo;
import com.example.services.PhotoService;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("searchController")
@ViewScoped
public class SearchController implements Serializable {
    private static final long serialVersionUID = 1L;
    private String searchText;
    private List<Photo> searchResults = new ArrayList<>();
    private boolean searchPerformed = false;

    @Inject
    private PhotoService photoService;
    @Inject
    private PhotoController photoController;

    public void handleSearch() {
        if (searchText == null || searchText.trim().isEmpty()) {
            searchResults.clear();
            searchPerformed = false;
        } else {
            List<Photo> results = photoService.searchPhotos(searchText.trim());
            searchResults = new ArrayList<>(results);
            searchPerformed = true;
        }
    }

    public void clearSearch() {
        searchText = "";
        searchResults.clear();
        searchPerformed = false;
    }

    // Getters and Setters
    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public List<Photo> getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(List<Photo> searchResults) {
        this.searchResults = searchResults;
    }

    public boolean isSearchPerformed() {
        return searchPerformed;
    }

    public void setSearchPerformed(boolean searchPerformed) {
        this.searchPerformed = searchPerformed;
    }
}