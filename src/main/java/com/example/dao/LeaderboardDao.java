package com.example.dao;

import com.example.model.Leaderboard;
import com.example.model.User;
import org.primefaces.model.FilterMeta;

import java.util.List;
import java.util.Map;

public interface LeaderboardDao extends BaseDao<Leaderboard, Long> {
    boolean save(Leaderboard leaderboard);
    Leaderboard findById(Long id);
    List<Leaderboard> findAll();
    boolean update(Leaderboard leaderboard);
    boolean deleteById(Long id);
    List<Leaderboard> findPaginatedEntities(
            Map<String, FilterMeta> filters,
            Map<String, Object> exactMatchFilters,
            int first,
            int pageSize
    );
    int getTotalEntityCount(Map<String, FilterMeta> filters, Map<String, Object> exactMatchFilters);
    List<javax.persistence.criteria.Predicate> buildFilters(
            javax.persistence.criteria.CriteriaBuilder cb,
            javax.persistence.criteria.Root<Leaderboard> root,
            Map<String, FilterMeta> filters
    );

    List<Leaderboard> getTopUsers(int limit);

    Long getTotalRatings(User user);

    Double getAverageRating(User user);

    Leaderboard findByUser(User user);

    void saveOrUpdate(Leaderboard leaderboard);

    List<Leaderboard> getOrderedLeaderboard();
}
