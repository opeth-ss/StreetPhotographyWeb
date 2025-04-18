package com.example.services;

import com.example.dao.BaseDao;
import com.example.dao.PhotoDao;
import com.example.dao.PhotoTagDao;
import com.example.dao.TagDao;
import com.example.model.*;
import org.primefaces.model.file.UploadedFile;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class PhotoService {

    @Inject
    private PhotoDao photoDao;

    @Inject
    private PhotoTagDao photoTagDao;

    @Inject
    private TagDao tagDao;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private PhotoTagService photoTagService;

    @Inject
    private RatingService ratingService;

    @Inject
    private LeaderboardService leaderboardService;

    @PersistenceContext(unitName = "StreetPhotography")
    private EntityManager em;

    @Transactional
    public void savePhoto(Photo photo, UploadedFile uploadedFile, List<String> tagNames, User user, String imageDirectory, long maxFileSize, List<String> allowedTypes) throws IOException {
        if (uploadedFile == null) {
            throw new IllegalArgumentException("No image selected");
        }
        if (uploadedFile.getSize() == 0) {
            throw new IllegalArgumentException("Empty image file");
        }
        if (uploadedFile.getSize() > maxFileSize) {
            throw new IllegalArgumentException("Maximum file size is 1MB");
        }
        String contentType = uploadedFile.getContentType();
        if (!allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, and GIF files are allowed");
        }

        photo.setUser(user);
        String imagePath = saveFile(uploadedFile, imageDirectory);
        photo.setImagePath(imagePath);

        try {
            photoDao.save(photo);
            if (tagNames != null && !tagNames.isEmpty()) {
                List<Tag> tags = tagNames.stream()
                        .map(photoTagService::findTagByName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (tags.isEmpty()) {
                    throw new IllegalArgumentException("No valid tags were selected");
                }
                photoTagService.addTagsToPhoto(photo, tags, user);
            }
        } catch (Exception e) {
            photoDao.deleteById(photo.getId());
            new File(imagePath).delete();
            throw e;
        }
    }

    private String saveFile(UploadedFile uploadedFile, String imageDirectory) throws IOException {
        String extension = getFileExtension(uploadedFile.getFileName());
        String fileName = UUID.randomUUID().toString() + extension;
        String filePath = imageDirectory + File.separator + fileName;

        File directory = new File(imageDirectory);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create directory: " + imageDirectory);
        }
        if (!directory.canWrite()) {
            throw new SecurityException("No write permission for directory: " + imageDirectory);
        }

        File file = new File(filePath);
        try (InputStream is = uploadedFile.getInputStream();
             OutputStream os = Files.newOutputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }

        if (!file.exists() || file.length() == 0) {
            file.delete();
            throw new IOException("Failed to write file or file is empty: " + filePath);
        }

        return filePath;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    @Transactional
    public Photo ratePhoto(Photo photo, User user, Integer ratingValue, Map<Long, Integer> ratingMap) {
        if (user == null) {
            throw new IllegalStateException("Please login to rate photos");
        }
        if (ratingValue == null || ratingValue < 1 || ratingValue > 5) {
            throw new IllegalArgumentException("Invalid rating value");
        }

        Rating existingRating = ratingService.userRatingExists(user, photo);
        if (existingRating != null) {
            Double oldRating = existingRating.getRating();
            existingRating.setRating((double) ratingValue);
            ratingService.update(existingRating);
            ratingService.adjustRatingsForReRating(photo, user, oldRating, (double) ratingValue);
        } else {
            Rating rating = new Rating();
            rating.setRating((double) ratingValue);
            rating.setUser(user);
            rating.setPhoto(photo);
            ratingService.save(rating);
            ratingService.recalculateImageRating(photo, (double) ratingValue);
            ratingService.recalculateUserRating(photo.getUser());
        }

        ratingMap.put(photo.getId(), ratingValue);
        return refreshPhoto(photo);
    }

    @Transactional
    public void deletePhoto(Photo photo, User currentUser) {
        if (!photo.getUser().getUserName().equals(currentUser.getUserName()) && !currentUser.getRole().equalsIgnoreCase("admin")) {
            throw new SecurityException("You can only delete your own photos");
        }

        User photoOwner = photo.getUser();
        photoDao.deleteById(photo.getId());
        ratingService.recalculateUserRating(photoOwner);
        leaderboardService.updateLeaderBoard(photoOwner);
    }

    @Transactional
    public void updatePhoto(Photo photo, UploadedFile uploadedFile, User currentUser, String imageDirectory, long maxFileSize, List<String> allowedTypes) throws IOException {
        if (!photo.getUser().getUserName().equals(currentUser.getUserName()) && !currentUser.getRole().equalsIgnoreCase("admin")) {
            throw new SecurityException("You can only update your own photos");
        }

        photo.setStatus("PENDING");
        if (uploadedFile != null && uploadedFile.getSize() > 0) {
            if (uploadedFile.getSize() > maxFileSize) {
                throw new IllegalArgumentException("Maximum file size is 1MB");
            }
            String contentType = uploadedFile.getContentType();
            if (!allowedTypes.contains(contentType)) {
                throw new IllegalArgumentException("Only JPEG, PNG, and GIF files are allowed");
            }
            String imagePath = saveFile(uploadedFile, imageDirectory);
            photo.setImagePath(imagePath);
        }

        photoDao.update(photo);
    }

    @Transactional
    public void approvePhoto(Photo photo, User admin) {
        if (!admin.getRole().equalsIgnoreCase("ADMIN")) {
            throw new SecurityException("Admin role required");
        }
        if (photo == null) {
            throw new IllegalArgumentException("There is no photo to approve");
        }
        photo.setStatus("APPROVED");
        photo.setApprovedBy(admin);
        photo.setApprovedDate(LocalDateTime.now());
        photoDao.update(photo);
    }

    @Transactional
    public void rejectPhoto(Photo photo, User admin) {
        if (!admin.getRole().equalsIgnoreCase("ADMIN")) {
            throw new SecurityException("Admin role required");
        }
        if (photo == null) {
            throw new IllegalArgumentException("There is no photo to approve");
        }
        photo.setStatus("REJECTED");
        photo.setApprovedBy(admin);
        photo.setApprovedDate(LocalDateTime.now());
        photoDao.update(photo);
    }

    @Transactional
    public void pendingPhoto(Photo photo, User admin) {
        if (!admin.getRole().equalsIgnoreCase("ADMIN")) {
            throw new SecurityException("Admin role required");
        }
        if (photo == null) {
            throw new IllegalArgumentException("There is no photo to approve");
        }
        photo.setStatus("PENDING");
        photo.setApprovedBy(admin);
        photo.setApprovedDate(LocalDateTime.now());
        photoDao.update(photo);
    }

    @Transactional
    public boolean deleteAllPhotosByUser(User user, User currentUser) {
        List<Photo> userPhotos = photoDao.findByUser(user);
        for (Photo photo : userPhotos) {
            deletePhoto(photo, currentUser);
        }
        return true;
    }

    @Transactional
    public List<Photo> getPhotosByUser(User user) {
        return photoDao.findByUser(user);
    }

    public List<Tag> getPhotoTags(Photo photo) {
        List<PhotoTag> photoTags = photoTagDao.findByPhotoId(photo.getId());
        List<Tag> tags = new ArrayList<>();
        for (PhotoTag photoTag : photoTags) {
            tags.add(photoTag.getTag());
        }
        return tags;
    }

    public Photo refreshPhoto(Photo photo) {
        if (photo == null || photo.getId() == null) {
            return photo;
        }
        return em.find(Photo.class, photo.getId());
    }

    public Photo findById(Long id) {
        return photoDao.findById(id);
    }

    public Long getTotalPhotos(User user) {
        return photoDao.countByUser(user);
    }

    public List<Photo> getPhotosPaginated(int first, int pageSize) {
        return photoDao.getPhotosPaginated(first, pageSize);
    }

    public int getAllCount() {
        return photoDao.getAllCount();
    }

    private Configuration getConfig() {
        return configurationService.getConfiguration();
    }

    public List<String> getAllPinPoints() {
        return photoDao.getAllPinPoints();
    }

    public List<Photo> getFilteredPhotos(int first, int pageSize, String filterLocation,
                                         List<String> filterTags, Double filterMinRating, String searchText, User currentUser) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("filterLocation", filterLocation);
        filters.put("filterTags", filterTags);
        filters.put("filterMinRating", filterMinRating);
        filters.put("searchText", searchText);
        filters.put("currentUser", currentUser);

        return photoDao.findFilteredPhotos(filters, first, pageSize);
    }

    public int getFilteredCount(String filterLocation, List<String> filterTags, Double filterMinRating, String searchText, User currentUser) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("filterLocation", filterLocation);
        filters.put("filterTags", filterTags);
        filters.put("filterMinRating", filterMinRating);
        filters.put("searchText", searchText);
        filters.put("currentUser", currentUser);

        return photoDao.getFilteredCount(filters);
    }

    public BaseDao<Photo, Long> getPhotoDao() {
        return photoDao;
    }
}