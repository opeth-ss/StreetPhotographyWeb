package com.example.dao.impl;

import com.example.dao.UserDao;
import com.example.model.User;

import javax.ejb.Stateless;
import javax.persistence.*;
import java.util.List;
import java.util.Optional;

@Stateless
public class UserDaoImpl extends BaseDaoImpl<User, Long> implements UserDao {

    @PersistenceContext(unitName = "StreetPhotography") // This injects the EntityManager
    private EntityManager em;

    public UserDaoImpl(){
        super(User.class);
    }

    @Override
    public Optional<User> findByUserName(String userName) {
        try {
            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.userName = :userName", User.class);
            query.setParameter("userName", userName);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try {
            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.email = :email", User.class);
            query.setParameter("email", email);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<User> findAll() {
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u", User.class);
        return query.getResultList();
    }
}
