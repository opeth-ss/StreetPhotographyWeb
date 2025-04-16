package com.example.services;

import com.example.dao.BaseDao;
import com.example.dao.UserDao;
import com.example.model.User;
import org.mindrot.jbcrypt.BCrypt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.util.List;

@ApplicationScoped
public class AuthenticationService {
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
        return user != null && BCrypt.checkpw(password, user.getPassword());
    }

    public User getUserByUsername(String userName) {
        return userDao.findByUserName(userName).orElse(null);
    }

    public boolean updateUser(User user) {
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        return userDao.update(user);
    }

    public List<User> findAll(){
        return userDao.findAll();
    }

    @Transactional
    public void deleteUser(User user){
        userDao.deleteById(user.getId());
    }

    public User findUserById(Long id){
        return userDao.findById(id);
    }

    public BaseDao<User, Long> getUserDao() {
        return userDao;
    }
}
