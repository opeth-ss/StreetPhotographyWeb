package com.example.config;

import com.example.api.AuthAPI;
import com.example.cors.CORS;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class RestApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(CORS.class);
        classes.add(AuthAPI.class);
        return classes;
    }
}