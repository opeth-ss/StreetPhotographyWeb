package com.example.services;

import com.example.dao.BaseDao;
import com.example.dao.UserDao;
import com.example.model.User;
import com.example.utils.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.primefaces.model.FilterMeta;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AuthenticationService {
    @Inject
    private UserDao userDao;

    @Transactional
    public String authenticate(String username, String password) {
        User user = userDao.findByUserName(username).orElse(null);
        if (user != null && BCrypt.checkpw(password, user.getPassword())) {
            return JwtUtil.generateToken(username, user.getRole());
        }
        return null;
    }

    @Transactional
    public boolean registerUser(User user) {
        if (userDao.findByUserName(user.getUserName()).isPresent()) {
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

    public List<User> findAll() {
        return userDao.findAll();
    }

    public List<User> findAllPaginated(int page, int size, String sortField, String sortOrder, Map<String, Object> filters) {
        // Convert page to 0-based index for DAO (frontend sends 1-based)
        int first = (page - 1) * size;
        return userDao.findPaginatedEntities(null, filters, first, size, sortField, sortOrder, (String) filters.get("global"));
    }

    public long getTotalUserCount(Map<String, Object> filters) {
        return userDao.getTotalEntityCount(null, filters, (String) filters.get("global"));
    }

    @Transactional
    public void deleteUser(User user) {
        userDao.deleteById(user.getId());
    }

    public User findUserById(Long id) {
        return userDao.findById(id);
    }

    public User findByUsername(String userName) {
        return userDao.findByUserName(userName).orElse(null);
    }

    public User findById(Long id) {
        return userDao.findById(id);
    }

    public BaseDao<User, Long> getUserDao() {
        return userDao;
    }

    public User updateNew(User existingUser) {
        if (userDao.update(existingUser)) {
            return findUserById(existingUser.getId());
        }
        return null;
    }
}

