package com.example.controller;

import com.example.model.User;
import com.example.services.AuthenticationService;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpSession;
import java.io.Serializable;

@Named("userController")
@SessionScoped
public class UserController implements Serializable {
    private static final long serialVersionUID = 1L;
    private User user = new User();
    private String userName;
    private String email;
    private String password;
    private boolean loggedIn = false;

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
        System.out.println("UserName: " + userName);
        if (authenticationService.loginUser(userName, password)) {
            loggedIn = true;
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("loggedInUser", user);
            return "/pages/home.xhtml?faces-redirect=true"; // Ensure it's an absolute path
        } else {
            System.out.println("Login failed for: " + userName);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login failed", "Incorrect Username or Password"));
            loggedIn = false;
            return "/pages/login.xhtml?faces-redirect=true"; // Ensure it's an absolute path
        }
    }

    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        loggedIn = false;
        return "/pages/login.xhtml?faces-redirect=true"; // Use relative path
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
}
