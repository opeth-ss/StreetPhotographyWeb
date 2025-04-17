package com.example.utils;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionUtil implements HttpSessionListener {
    private static final Map<String, HttpSession> activeSessions = new ConcurrentHashMap<>();
    private static final Map<String, HttpSession> sessionsByUsername = new ConcurrentHashMap<>();

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        activeSessions.put(session.getId(), session);
        String username = (String) session.getAttribute("username");
        if (username != null) {
            sessionsByUsername.put(username, session);
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        activeSessions.remove(session.getId());
        String username = (String) session.getAttribute("username");
        if (username != null) {
            sessionsByUsername.remove(username);
        }
    }

    public static Map<String, HttpSession> getActiveSessions() {
        return activeSessions;
    }

    public static boolean isSessionActive(String username) {
        return username != null && sessionsByUsername.containsKey(username);
    }

    public static void invalidateSessionByUser(String username) {
        if (username == null) {
            return;
        }
        HttpSession session = sessionsByUsername.get(username);
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException e) {
                // Session already invalidated
            }
        }
    }

    public static Map<String, Object> getSessionMap() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return new HashMap<>();
        }
        return facesContext.getExternalContext().getSessionMap();
    }

    public static String getLoggedInUsername() {
        return (String) getSessionMap().get("username");
    }
}