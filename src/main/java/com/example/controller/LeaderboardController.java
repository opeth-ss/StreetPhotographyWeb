package com.example.controller;

import com.example.model.Leaderboard;
import com.example.model.User;
import com.example.services.LeaderboardService;
import com.example.services.PhotoService;

import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named("leaderBoardController")
@ViewScoped
public class LeaderboardController implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private LeaderboardService leaderboardService;

    @Inject
    private PhotoService photoService;

    private List<Leaderboard> leaderboard;

    public List<Leaderboard> getLeaderboard() {
        if (leaderboard == null) {
            leaderboard = leaderboardService.returnLeaderboard();
        }
        return leaderboard;
    }

    public Long getUserPhotoCount(User user) {
        return photoService.getTotalPhotos(user);
    }
}
