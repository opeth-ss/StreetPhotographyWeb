package com.example.utils;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@JWTRequired
@Priority(Priorities.AUTHENTICATION)
public class JwtRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Cookie jwtCookie = requestContext.getCookies().get(JwtUtil.TOKEN_COOKIE_NAME);

        if (jwtCookie == null) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Missing authentication token").build());
            return;
        }

        String token = jwtCookie.getValue();
        if (token == null || token.trim().isEmpty()) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid token format").build());
            return;
        }

        try {
            if (!JwtUtil.validateToken(token)) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Invalid authentication token").build());
                return;
            }

            String username = JwtUtil.extractUsername(token);
            String role = JwtUtil.extractRole(token);
            requestContext.setProperty("username", username);
            requestContext.setProperty("role", role);

        } catch (Exception e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Token processing error").build());
        }
    }
}