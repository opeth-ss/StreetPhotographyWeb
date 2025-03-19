package com.example.dao.impl;

import com.example.dao.LeaderboardDao;
import com.example.model.Leaderboard;
import com.example.model.PhotoTag;
import com.example.model.User;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class LeaderboardDaoImpl implements LeaderboardDao {
    @PersistenceContext(unitName = "StreetPhotography") // This injects the EntityManager
    private EntityManager em;

    @Override
    public boolean save(Leaderboard leaderboard) {
        boolean status = false;

        try {
            em.persist(leaderboard); // EntityManager automatically handles the transaction
            status = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    public boolean update(Leaderboard leaderboard) {
        boolean status = false;

        try {
            em.merge(leaderboard); // EntityManager automatically handles the transaction
            status = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    public boolean deleteById(Long id) {
        boolean status = false;

        try {
            Leaderboard leaderboard = em.find(Leaderboard.class, id);
            if (leaderboard != null) {
                em.remove(leaderboard); // EntityManager automatically handles the transaction
                status = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    public Leaderboard findById(Long id) {
        Leaderboard leaderboard = null;
        try {
            leaderboard = em.find(Leaderboard.class, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return leaderboard;
    }

    @Override
    public List<Leaderboard> getTopUsers(int limit) {
        // Clear any cached entities to ensure fresh results
        em.clear();

        TypedQuery<Leaderboard> query = em.createQuery(
                "SELECT l FROM Leaderboard l ORDER BY l.totalRatings DESC, l.averageRating DESC", Leaderboard.class);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public void updateLeaderboard(User user) {
        try {
            // Refresh the user entity to get the latest data
            em.refresh(user);

            // Ensure the user exists in the system
            if (user == null || user.getId() == null) {
                throw new IllegalArgumentException("User must exist in the system.");
            }

            // Get total ratings count for the user - use direct SQL for accuracy
            Long totalRatings = em.createQuery(
                            "SELECT COUNT(r) FROM Rating r JOIN r.photo p WHERE p.user = :user", Long.class)
                    .setParameter("user", user)
                    .getSingleResult();

            // Calculate average rating directly from ratings data for accuracy
            Double averageRating = 0.0;
            if (totalRatings > 0) {
                averageRating = em.createQuery(
                                "SELECT AVG(r.rating) FROM Rating r JOIN r.photo p WHERE p.user = :user", Double.class)
                        .setParameter("user", user)
                        .getSingleResult();

                // Handle null result (no ratings)
                if (averageRating == null) {
                    averageRating = 0.0;
                }
            }

            // Check if the leaderboard entry exists for this user
            List<Leaderboard> results = em.createQuery(
                            "SELECT l FROM Leaderboard l WHERE l.user = :user", Leaderboard.class)
                    .setParameter("user", user)
                    .getResultList();

            Leaderboard leaderboardEntry = results.isEmpty() ? null : results.get(0);

            // If leaderboard entry doesn't exist, create a new one
            if (leaderboardEntry == null) {
                leaderboardEntry = new Leaderboard();
                leaderboardEntry.setUser(user);
                leaderboardEntry.setTotalRatings(totalRatings);
                leaderboardEntry.setAverageRating(averageRating);
                em.persist(leaderboardEntry); // Persist new leaderboard entry
            } else {
                // If it exists, update the existing entry
                leaderboardEntry.setTotalRatings(totalRatings);
                leaderboardEntry.setAverageRating(averageRating);
                em.merge(leaderboardEntry); // Merge the existing leaderboard entry
            }

            // Force flush to ensure changes are written
            em.flush();

            // Update rankings in the same transaction
            updateRankingsInTransaction();

            // Commit the transaction
            em.getTransaction().commit();

            // Clear the entity manager to ensure fresh data on next query
            em.clear();

        } catch (Exception e) {
            handleException(e);
        }
    }



    private void updateRankingsInTransaction() {
        try {
            // Get all leaderboard entries ordered by ranking criteria
            List<Leaderboard> leaderboard = em.createQuery(
                            "SELECT l FROM Leaderboard l ORDER BY l.totalRatings DESC, l.averageRating DESC", Leaderboard.class)
                    .getResultList();

            // Update all ranks in one go
            long rank = 1;
            for (Leaderboard entry : leaderboard) {
                entry.setUserRank(rank++);
                em.merge(entry);
            }

            // Force flush to ensure all changes are written
            em.flush();
        } catch (Exception e) {
            // Let the calling method handle the exception
            throw e;
        }
    }

    private void handleException(Exception e) {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
        e.printStackTrace();
    }
}
