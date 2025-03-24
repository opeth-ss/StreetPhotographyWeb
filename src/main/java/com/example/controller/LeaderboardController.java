package com.example.controller;

import com.example.model.Leaderboard;
import com.example.services.LeaderboardService;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named("leaderBoardController")
@SessionScoped
public class LeaderboardController implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private LeaderboardService leaderboardService;

    private List<Leaderboard> leaderboard;

    public List<Leaderboard> getLeaderboard() {
        if (leaderboard == null) {
            leaderboard = leaderboardService.returnLeaderboard();
        }
        return leaderboard;
    }
}
