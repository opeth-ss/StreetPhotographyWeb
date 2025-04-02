package com.example.controller;

import com.example.model.User;
import com.example.services.AuthenticationService;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    public UserController() {
    }


    public String register() {
        // Set user properties from form fields
        user.setUserName(userName);
        user.setEmail(email);
        user.setPassword(password);

        if (authenticationService.registerUser(user)) {
            // Reset form fields after successful registration
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
        if (authenticationService.loginUser(userName, password)) {
            loggedIn = true;
            user = authenticationService.getUserByUsername(userName); // Fetch user details

            FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();

            // Save user data in cookies
            Cookie userCookie = new Cookie("loggedInUser", user.getUserName());
            userCookie.setMaxAge(60 * 60 * 24 * 7); // 7 days
            userCookie.setPath("/"); // Make it available across the entire app
            response.addCookie(userCookie);

            if(Objects.equals(user.getRole(), "admin")){
                return "/pages/admin/admin.xhtml?faces-redirect=true";
            }else{
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

        // Update the user information
        if (authenticationService.updateUser(user)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "User information updated successfully"));

            FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            Cookie userCookie = new Cookie("loggedInUser", user.getUserName());
            userCookie.setMaxAge(60 * 60 * 24 * 7); // 7 days
            userCookie.setPath("/"); // Make it available across the entire app
            response.addCookie(userCookie);

            try {
                facesContext.getExternalContext().redirect(((HttpServletRequest) facesContext.getExternalContext().getRequest()).getRequestURI());
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Reset edit mode and clear password fields
            editMode = false;

            return null; // Stay on the same page
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Update failed", "An error occurred while updating user info"));
            return null;
        }
    }


    public String logout() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();

        // Remove cookie
        Cookie userCookie = new Cookie("loggedInUser", null);
        userCookie.setMaxAge(0);
        userCookie.setPath("/");
        response.addCookie(userCookie);

        // Invalidate session
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        loggedIn = false;

        return "/pages/login.xhtml?faces-redirect=true";
    }

    @PostConstruct
    public void checkUserFromCookie() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("loggedInUser".equals(cookie.getName())) {
                    String storedUsername = cookie.getValue();
                    if (storedUsername != null && !storedUsername.isEmpty()) {
                        user = authenticationService.getUserByUsername(storedUsername);
                        loggedIn = true;
                        return;
                    }
                }
            }
        }

        // Only redirect if response isn't committed and not already on login page
        if (!((ExternalContext) externalContext).isResponseCommitted() &&
                !externalContext.getRequestServletPath().contains("login.xhtml")) {
            try {
                externalContext.redirect(externalContext.getRequestContextPath() + "/pages/login.xhtml");
            } catch (IOException ignored) {
            }
        }
    }

    // Add a new method specifically for role checking
    public void checkRole() {
        FacesContext context = FacesContext.getCurrentInstance();
        String viewId = context.getViewRoot().getViewId();

        // Example: Only allow ADMIN to access admin pages
        if (viewId.startsWith("/pages/admin/") && !hasRole("admin")) {
            try {
                context.getExternalContext().redirect("/streetphotography/pages/home.xhtml");
            } catch (IOException e) {
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

}
