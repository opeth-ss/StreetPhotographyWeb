package com.example.utils;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockedUserManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Map<String, Long> blockedUsers = new ConcurrentHashMap<>();

    public static void blockUser(String username, int minutes) {
        long blockUntil = System.currentTimeMillis() + (minutes * 60 * 1000);
        blockedUsers.put(username, blockUntil);
    }

    public static boolean isBlocked(String username) {
        Long blockedUntil = blockedUsers.get(username);
        if (blockedUntil == null) return false;

        if (System.currentTimeMillis() > blockedUntil) {
            blockedUsers.remove(username);
            return false;
        }
        return true;
    }

    public static long getRemainingBlockTime(String username) {
        Long blockedUntil = blockedUsers.get(username);
        if (blockedUntil == null) return 0;
        long remaining = blockedUntil - System.currentTimeMillis();
        return Math.max(remaining / 1000, 0); // seconds
    }
}