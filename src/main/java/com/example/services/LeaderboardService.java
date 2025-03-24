package com.example.services;

import com.example.dao.LeaderboardDao;
import com.example.model.Leaderboard;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class LeaderboardService {
    @Inject
    private LeaderboardDao leaderboardDao;

    public List<Leaderboard>  returnLeaderboard(){
        return leaderboardDao.getTopUsers(100);
    }
}
