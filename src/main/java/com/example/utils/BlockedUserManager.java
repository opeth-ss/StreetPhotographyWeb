package com.example.utils;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockedUserManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Map<String, Long> blockedUsers = new ConcurrentHashMap<>();

    public static void blockUser(String username, int minutes) {
        username = username != null ? username.trim().toLowerCase() : username;
        long blockUntil = System.currentTimeMillis() + (minutes * 60 * 1000);
        blockedUsers.put(username, blockUntil);
    }

    public static boolean isBlocked(String username) {
        username = username != null ? username.trim().toLowerCase() : username;
        Long blockedUntil = blockedUsers.get(username);
        if (blockedUntil == null) return false;

        if (System.currentTimeMillis() > blockedUntil) {
            blockedUsers.remove(username);
            return false;
        }
        return true;
    }

    public static long getRemainingBlockTime(String username) {
        username = username != null ? username.trim().toLowerCase() : username;
        Long blockedUntil = blockedUsers.get(username);
        if (blockedUntil == null) return 0;
        long remaining = blockedUntil - System.currentTimeMillis();
        return Math.max(remaining / 1000, 0); // seconds
    }
}