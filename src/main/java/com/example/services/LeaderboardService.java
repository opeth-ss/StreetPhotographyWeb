package com.example.services;

import com.example.dao.LeaderboardDao;
import com.example.model.Leaderboard;
import com.example.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class LeaderboardService {
    @Inject
    private LeaderboardDao leaderboardDao;

    @Transactional
    public void updateLeaderBoard(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must exist in the system.");
        }

        // Calculate total ratings for the user
        Long totalRatings = leaderboardDao.getTotalRatings(user);
        Double averageRating = totalRatings > 0 ? leaderboardDao.getAverageRating(user) : 0.0;

        // Retrieve or create leaderboard entry
        Leaderboard leaderboardEntry = leaderboardDao.findByUser(user);
        if (leaderboardEntry == null) {
            leaderboardEntry = new Leaderboard();
            leaderboardEntry.setUser(user);
            leaderboardEntry.setUserRank(0L);
        }

        leaderboardEntry.setTotalRatings(totalRatings);
        leaderboardEntry.setAverageRating(averageRating);
        leaderboardDao.saveOrUpdate(leaderboardEntry);

        // Update rankings
        updateRankings();
    }

    private void updateRankings() {
        List<Leaderboard> leaderboard = leaderboardDao.getOrderedLeaderboard();
        long rank = 1;
        for (Leaderboard entry : leaderboard) {
            entry.setUserRank(rank++);
            leaderboardDao.saveOrUpdate(entry);
        }
    }

    public List<Leaderboard> returnLeaderboard(){
        return leaderboardDao.getTopUsers(100);
    }
}