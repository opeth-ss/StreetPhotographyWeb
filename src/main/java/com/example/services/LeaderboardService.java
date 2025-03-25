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
    public void updateLeaderBoard(User user){
        leaderboardDao.updateLeaderboard(user);
    }

    public List<Leaderboard>  returnLeaderboard(){
        return leaderboardDao.getTopUsers(100);
    }
}
