package com.example.services;

import com.example.dao.UserDao;
import com.example.model.User;
import org.mindrot.jbcrypt.BCrypt;

import javax.annotation.ManagedBean;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.Serializable;

@ApplicationScoped
public class AuthenticationService implements Serializable {
    private static final long serialVersionUID = 1L;
    @Inject
    private UserDao userDao;

    @Transactional
    public boolean registerUser(User user){
        if(userDao.findByUserName(user.getUserName()).isPresent()){
            return false;
        }
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        return userDao.save(user);
    }

    public boolean loginUser(String username, String password) {
        User user = userDao.findByUserName(username).orElse(null);
        if (user != null && BCrypt.checkpw(password, user.getPassword())) {
            return true;
        }
        return false;
    }
}
