package com.example.controller;

import com.example.model.User;
import com.example.services.AuthenticationService;
import com.example.utils.BlockedUserManager;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Serializable;
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

    public UserController() {
    }

    @PostConstruct
    public void init() {
        // No cookie checking needed anymore
    }

    public String register() {
        if (authenticationService == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "System error", "Authentication service unavailable"));
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
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Registration failed", "Username already exists"));
            return "/pages/register.xhtml?faces-redirect=true";
        }
    }

    public String login() {
        // Check if user is blocked first
        if (BlockedUserManager.isBlocked(userName)) {
            long remainingMinutes = BlockedUserManager.getRemainingBlockTime(userName) / 60;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Account Temporarily Blocked",
                            "Your account is blocked for " + remainingMinutes + " more minutes."));
            return "/pages/login.xhtml?faces-redirect=true";
        }

        if (authenticationService.loginUser(userName, password)) {
            loggedIn = true;
            user = authenticationService.getUserByUsername(userName);

            // Store user info in session
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.getSessionMap().put("username", userName);
            externalContext.getSessionMap().put("role", user.getRole());

            if (Objects.equals(user.getRole(), "admin")) {
                return "/pages/admin/admin.xhtml?faces-redirect=true";
            } else {
                return "/pages/home.xhtml?faces-redirect=true";
            }
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login failed", "Incorrect Username or Password"));
            loggedIn = false;
            return "/pages/login.xhtml?faces-redirect=true";
        }
    }

    public String updateUserInfo() {
        // Check if the username already exists in the database
        User existingUser = authenticationService.getUserByUsername(user.getUserName());

        if (existingUser != null && !existingUser.getId().equals(user.getId())) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Update failed", "Username already exists"));
            return null;
        }

        if (authenticationService.updateUser(user)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "User information updated successfully"));
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
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Update failed", "An error occurred while updating user info"));
            return null;
        }
    }

    public void checkLogin() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();

        // Check session attribute instead of cookies
        Object username = externalContext.getSessionMap().get("username");

        if (username == null || !loggedIn) {
            try {
                externalContext.redirect(externalContext.getRequestContextPath() + "/pages/login.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Check if user is blocked (even if already logged in)
        else if (blockedUserManager.isBlocked(username.toString())) {
            try {
                externalContext.getSessionMap().remove("username");
                externalContext.getSessionMap().remove("role");
                externalContext.invalidateSession();
                loggedIn = false;

                long remainingMinutes = blockedUserManager.getRemainingBlockTime(username.toString()) / 60;
                facesContext.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Account Blocked",
                                "Your account is temporarily blocked. Please try again in " + remainingMinutes + " minutes."));

                externalContext.redirect(externalContext.getRequestContextPath() + "/pages/login.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if ("admin".equals(user.getRole()) && !externalContext.getRequestServletPath().contains("admin")) {
            try {
                externalContext.redirect(externalContext.getRequestContextPath() + "/pages/admin/admin.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isCurrentUserBlocked() {
        if (!loggedIn) return false;
        return blockedUserManager.isBlocked(user.getUserName());
    }

    public String logout() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();

        // Clear session
        externalContext.getSessionMap().remove("username");
        externalContext.getSessionMap().remove("role");
        externalContext.invalidateSession();
        loggedIn = false;

        return "/pages/login.xhtml?faces-redirect=true";
    }

    // Removed checkUserFromCookie() as it's no longer needed

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