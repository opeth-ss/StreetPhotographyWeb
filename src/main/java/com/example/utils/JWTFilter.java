package com.example.utils;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@JWTRequired
@Priority(Priorities.AUTHENTICATION)
public class JWTFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Get token from Authorization header or cookie
        String token = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (token == null || !token.startsWith("Bearer ")) {
            // Try to get from cookie if not in header
            token = requestContext.getCookies().get("access_token") != null
                    ? requestContext.getCookies().get("access_token").getValue()
                    : null;
        } else {
            token = token.substring(7); // Remove "Bearer " prefix
        }

        if (token == null || !JwtUtil.validateToken(token)) {
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("{\"error\":\"Invalid or missing token\"}")
                            .build()
            );
            return;
        }

        // Optionally add user info to request context for use in endpoints
        String username = JwtUtil.extractUsername(token);
        requestContext.setProperty("username", username);
    }

    // In JWTFilter
    private String extractToken(ContainerRequestContext requestContext) {
        // Try Authorization header first
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Then try cookie
        javax.ws.rs.core.Cookie cookie = requestContext.getCookies().get("access_token");
        if (cookie != null) {
            return cookie.getValue();
        }

        return null;
    }
}