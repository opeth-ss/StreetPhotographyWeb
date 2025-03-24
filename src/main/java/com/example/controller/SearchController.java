package com.example.controller;

import com.example.model.Photo;
import com.example.services.PhotoService;

import javax.annotation.PostConstruct;
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
    private String searchCriteria;
    private String searchText;
    private List<Photo> searchResults = new ArrayList<>();
    private boolean searchPerformed = false;

    @Inject
    private PhotoService photoService;

    public void handleSearch() {
        if (searchText == null || searchText.trim().isEmpty()) {
            searchResults = new ArrayList<>();
            searchPerformed = true;
            return;
        }

        switch (searchCriteria) {
            case "location":
                searchResults = photoService.searchByLocation(searchText);
                break;
            case "tag":
                 searchResults = photoService.searchByTag(searchText);
                break;
            case "description":
                searchResults = photoService.searchByDescription(searchText);
                break;
            case "username":
                // searchResults = photoService.searchByUsername(searchText); // Uncomment if implemented
                break;
            default:
                searchResults = new ArrayList<>();
                break;
        }

        searchPerformed = true; // Set flag to true after performing the search
    }

    // Getters and Setters
    public String getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(String searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

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
    public void clearSearch() {
        searchText = "";
        searchCriteria = "";
        searchResults.clear();
        searchPerformed = false;
    }
}