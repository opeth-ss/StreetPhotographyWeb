package com.example.controller;

import com.example.model.User;
import com.example.services.AuthenticationService;
import com.example.utils.BlockedUserManager;
import com.example.utils.JwtUtil;
import com.example.utils.MessageHandler;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Named("userController")
@SessionScoped
public class UserController implements Serializable {
    private static final long serialVersionUID = 1L;
    private User user = new User();
    private String userName;
    private String email;
    private String password;
    private boolean loggedIn = false;
    private boolean editMode = false;

    @Inject
    private AuthenticationService authenticationService;

    @Inject
    private BlockedUserManager blockedUserManager;

    @Inject
    private MessageHandler messageHandler;

    public UserController() {
    }

    @PostConstruct
    public void init() {
        // No cookie checking needed anymore
    }

    public String register() {
        if (authenticationService == null) {
            messageHandler.addErrorMessage("System error", "Authentication service unavailable", ":growl");
            return "/pages/register.xhtml?faces-redirect=true";
        }
        user.setUserName(userName);
        user.setEmail(email);
        user.setPassword(password);

        if (authenticationService.registerUser(user)) {
            userName = null;
            email = null;
            password = null;
            user = new User();
            return "/pages/login.xhtml?faces-redirect=true";
        } else {
            messageHandler.addErrorMessage("Registration failed", "Username already exists", ":growl");
            return "/pages/register.xhtml?faces-redirect=true";
        }
    }

    public String login() {
        String normalizedUserName = userName != null ? userName.trim().toLowerCase() : userName;
        if (BlockedUserManager.isBlocked(normalizedUserName)) {
            long remainingMinutes = BlockedUserManager.getRemainingBlockTime(normalizedUserName) / 60;
            messageHandler.addErrorMessage("Account Temporarily Blocked",
                    "Your account is blocked for " + remainingMinutes + " more minutes.", "blockMessage");
            return "/pages/login.xhtml?faces-redirect=true";
        }

        String token = authenticationService.authenticate(userName, password);
        if (token != null) {
            loggedIn = true;
            user = authenticationService.getUserByUsername(userName);

            // Store token in cookie
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            // Create HTTP-only secure cookie
            Map<String, Object> properties = new HashMap<>();
            properties.put("maxAge", 86400 * 10); // 10 days
            properties.put("httpOnly", true);
            properties.put("secure", true); // for HTTPS
            properties.put("path", "/");
            externalContext.addResponseCookie("jwt", token, properties);

            if (Objects.equals(user.getRole(), "admin")) {
                return "/pages/admin/admin.xhtml?faces-redirect=true";
            } else {
                return "/pages/home.xhtml?faces-redirect=true";
            }
        } else {
            messageHandler.addErrorMessage("Login failed", "Incorrect Username or Password", ":growl");
            loggedIn = false;
            return "/pages/login.xhtml?faces-redirect=true";
        }
    }

    public String updateUserInfo() {
        // Check if the username already exists in the database
        User existingUser = authenticationService.getUserByUsername(user.getUserName());

        if (existingUser != null && !existingUser.getId().equals(user.getId())) {
            messageHandler.addErrorMessage("Update failed", "Username already exists", ":growl");
            return null;
        }

        if (authenticationService.updateUser(user)) {
            messageHandler.addInfoMessage("Success", "User information updated successfully", ":growl");
            editMode = false;
            try {
                FacesContext.getCurrentInstance().getExternalContext()
                        .redirect(((javax.servlet.http.HttpServletRequest) FacesContext.getCurrentInstance()
                                .getExternalContext().getRequest()).getRequestURI());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null; // Stay on the same page
        } else {
            messageHandler.addErrorMessage("Update failed", "An error occurred while updating user info", ":growl");
            return null;
        }
    }

    public void checkLogin() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();

        // Get token from cookie
        Map<String, Object> cookies = externalContext.getRequestCookieMap();
        String token = null;

        if (cookies.containsKey("jwt")) {
            // Cast the cookie object properly
            javax.servlet.http.Cookie cookie = (javax.servlet.http.Cookie) cookies.get("jwt");
            token = cookie.getValue();
        }

        if (token == null || !JwtUtil.validateToken(token)) {
            try {
                externalContext.redirect(externalContext.getRequestContextPath() + "/pages/login.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // Check if user is blocked
        String username = JwtUtil.extractUsername(token);
        if (blockedUserManager.isBlocked(username)) {
            try {
                // Clear cookie
                externalContext.addResponseCookie("jwt", "", Collections.singletonMap("maxAge", 0));

                long remainingMinutes = blockedUserManager.getRemainingBlockTime(username) / 60;
                messageHandler.addErrorMessage("Account Blocked",
                        "Your account is temporarily blocked. Please try again in " + remainingMinutes + " minutes.", "blockMessage");

                externalContext.redirect(externalContext.getRequestContextPath() + "/pages/login.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Set user data if not already set
        if (user == null || !user.getUserName().equals(username)) {
            user = authenticationService.getUserByUsername(username);
            loggedIn = true;
        }
    }

    public boolean isCurrentUserBlocked() {
        if (!loggedIn) return false;
        return blockedUserManager.isBlocked(user.getUserName());
    }

    public String logout() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();

        Map<String, Object> properties = new HashMap<>();
        properties.put("maxAge", 0);
        properties.put("httpOnly", true);
        properties.put("secure", true);
        properties.put("path", "/");
        externalContext.addResponseCookie("jwt", "", properties);

        loggedIn = false;
        user = null;
        externalContext.invalidateSession();

        return "/pages/login.xhtml?faces-redirect=true";
    }

    public void checkRole() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        String viewId = context.getViewRoot().getViewId();

        if (viewId.startsWith("/pages/admin/") && !hasRole("admin")) {
            try {
                context.getExternalContext().redirect("/streetphotography/pages/home.xhtml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if ("admin".equals(user.getRole()) && !externalContext.getRequestServletPath().contains("admin")) {
            try {
                externalContext.redirect(externalContext.getRequestContextPath() + "/pages/admin/admin.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean hasRole(String role) {
        if (!loggedIn || user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole().equalsIgnoreCase(role);
    }

    // Getters and Setters
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public boolean isUserBlocked(String username) {
        return blockedUserManager.isBlocked(username);
    }

    public long getRemainingBlockTime(String username) {
        return blockedUserManager.getRemainingBlockTime(username);
    }
}