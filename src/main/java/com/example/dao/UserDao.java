package com.example.dao;

import com.example.model.User;

import javax.faces.bean.ApplicationScoped;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;

@Named
@ApplicationScoped
public interface UserDao extends BaseDao<User, Long>{
    Optional<User> findByUserName(String userName);
    Optional<User> findByEmail(String email);
    List<User> findAll();
}
