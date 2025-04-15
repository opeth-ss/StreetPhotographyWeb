package com.example.dao.impl;

import com.example.dao.LeaderboardDao;
import com.example.model.Leaderboard;
import com.example.model.User;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class LeaderboardDaoImpl extends BaseDaoImpl<Leaderboard, Long> implements LeaderboardDao {
    @PersistenceContext(unitName = "StreetPhotography")
    private EntityManager em;

    public LeaderboardDaoImpl() {
        super(Leaderboard.class);
    }
    

    @Override
    public List<Leaderboard> getTopUsers(int limit) {
        em.clear();
        TypedQuery<Leaderboard> query = em.createQuery(
                "SELECT l FROM Leaderboard l ORDER BY l.totalRatings DESC, l.averageRating DESC", Leaderboard.class);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public Long getTotalRatings(User user) {
        return em.createQuery(
                        "SELECT COUNT(r) FROM Rating r JOIN r.photo p WHERE p.user = :user", Long.class)
                .setParameter("user", user)
                .getSingleResult();
    }

    @Override
    public Double getAverageRating(User user) {
        Double averageRating = em.createQuery(
                        "SELECT AVG(r.rating) FROM Rating r JOIN r.photo p WHERE p.user = :user", Double.class)
                .setParameter("user", user)
                .getSingleResult();
        return averageRating != null ? averageRating : 0.0;
    }

    @Override
    public Leaderboard findByUser(User user) {
        List<Leaderboard> results = em.createQuery(
                        "SELECT l FROM Leaderboard l WHERE l.user = :user", Leaderboard.class)
                .setParameter("user", user)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public void saveOrUpdate(Leaderboard leaderboard) {
        if (leaderboard.getId() == null) {
            em.persist(leaderboard);
        } else {
            em.merge(leaderboard);
        }
    }

    @Override
    public List<Leaderboard> getOrderedLeaderboard() {
        return em.createQuery(
                        "SELECT l FROM Leaderboard l ORDER BY l.totalRatings DESC, l.averageRating DESC", Leaderboard.class)
                .getResultList();
    }
}
