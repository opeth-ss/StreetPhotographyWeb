package com.example.dao;

import com.example.model.Leaderboard;
import com.example.model.User;

import java.util.List;

public interface LeaderboardDao extends BaseDao<Leaderboard, Long> {
    void updateLeaderboard(User user);
    List<Leaderboard> getTopUsers(int limit);
}
