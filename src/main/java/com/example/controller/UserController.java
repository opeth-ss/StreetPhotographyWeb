package com.example.controller;

import com.example.model.User;
import com.example.services.AuthenticationService;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;

@Named("userController")
@RequestScoped
public class UserController {
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

        System.out.println("Registering user - Username: " + user.getUserName() + ", Email: " + user.getEmail());

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
            System.out.println("Login successful System.outfor: " + userName);
            loggedIn = true;
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
        return "login.xhtml?faces-redirect=true";
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