package com.example.dao;

import com.example.model.Leaderboard;
import com.example.model.User;

import java.util.List;

public interface LeaderboardDao extends BaseDao<Leaderboard, Long> {

    List<Leaderboard> getTopUsers(int limit);

    Long getTotalRatings(User user);

    Double getAverageRating(User user);

    Leaderboard findByUser(User user);

    void saveOrUpdate(Leaderboard leaderboard);

    List<Leaderboard> getOrderedLeaderboard();
}
