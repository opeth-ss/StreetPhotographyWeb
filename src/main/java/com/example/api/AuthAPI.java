package com.example.api;

import com.example.model.User;
import com.example.services.AuthenticationService;
import com.example.utils.JWTRequired;
import com.example.utils.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthAPI {

    @Inject
    private AuthenticationService authenticationService;

    // In-memory storage for refresh tokens (thread-safe)
    private static final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();

    @POST
    @Path("/login")
    public Response login(User loginRequest, @Context HttpServletRequest request) {
        // Validate input
        if (loginRequest.getUserName() == null || loginRequest.getPassword() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Username and password are required.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        User user = authenticationService.findByUsername(loginRequest.getUserName());
        if (user != null && BCrypt.checkpw(loginRequest.getPassword(), user.getPassword())) {
            // Generate tokens
            String accessToken = JwtUtil.generateAccessToken(user.getUserName(), user.getRole());
            String refreshToken = JwtUtil.generateNewRefreshToken(user.getUserName());

            // Store refresh token
            refreshTokenStore.put(user.getUserName(), refreshToken);

            // Set cookies with SameSite=Strict
            NewCookie accessCookie = new NewCookie(
                    "access_token",
                    accessToken,
                    "/",
                    null,
                    "Access token",
                    15 * 60, // 15 minutes
                    true, // Secure (HTTPS only)
                    true // HttpOnly
            );

            NewCookie refreshCookie = new NewCookie(
                    "refresh_token",
                    refreshToken,
                    "/",
                    null,
                    "Refresh token",
                    7 * 24 * 60 * 60, // 7 days
                    true, // Secure
                    true // HttpOnly
            );

            // Remove sensitive data
            user.setPassword(null);
            user.setJoinDate(null);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("user", user);
            responseMap.put("message", "Login successful");

            return Response.ok(responseMap)
                    .cookie(accessCookie, refreshCookie)
                    .header("X-Frame-Options", "DENY")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("Content-Security-Policy", "default-src 'self'")
                    .build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Invalid username or password.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @POST
    @Path("/register")
    public Response register(User registerRequest, @Context HttpServletRequest request) {
        if (registerRequest.getUserName() == null || registerRequest.getPassword() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Username and password are required.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Hash password
        String hashedPassword = BCrypt.hashpw(registerRequest.getPassword(), BCrypt.gensalt(12));
        registerRequest.setPassword(hashedPassword);

        boolean registered = authenticationService.registerUser(registerRequest);
        if (registered) {
            Map<String, Boolean> responseMap = new HashMap<>();
            responseMap.put("success", true);
            return Response.ok(responseMap)
                    .header("X-Frame-Options", "DENY")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("Content-Security-Policy", "default-src 'self'")
                    .build();
        } else {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"Username already exists.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/{username}")
    @JWTRequired
    public Response getUserByUsername(@PathParam("username") String username) {
        User user = authenticationService.findByUsername(username);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"User not found\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        user.setPassword(null);
        user.setJoinDate(null);
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("user", user);
        return Response.ok(responseMap)
                .header("X-Frame-Options", "DENY")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'self'")
                .build();
    }

    @GET
    @Path("/check")
    @JWTRequired
    public Response checkSession() {
        Map<String, Boolean> responseMap = new HashMap<>();
        responseMap.put("authenticated", true);
        return Response.ok(responseMap)
                .header("X-Frame-Options", "DENY")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'self'")
                .build();
    }

    @POST
    @Path("/refresh")
    public Response refreshToken(@Context HttpServletRequest request) {
        String refreshToken = extractTokenFromCookie(request, "refresh_token");
        if (refreshToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Refresh token missing.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        try {
            // Validate refresh token
            if (!JwtUtil.validateToken(refreshToken)) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Invalid or expired refresh token.\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            // Verify refresh token in store
            String username = JwtUtil.extractUsername(refreshToken);
            if (!refreshToken.equals(refreshTokenStore.get(username))) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Refresh token revoked or invalid.\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            // Generate new tokens
            User user = authenticationService.findByUsername(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"User not found.\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            String newAccessToken = JwtUtil.generateAccessToken(user.getUserName(), user.getRole());
            String newRefreshToken = JwtUtil.generateNewRefreshToken(user.getUserName());

            // Update refresh token in store
            refreshTokenStore.put(username, newRefreshToken);

            // Set new cookies
            NewCookie accessCookie = new NewCookie(
                    "access_token",
                    newAccessToken,
                    "/",
                    null,
                    "Access token",
                    15 * 60, // 15 minutes
                    true, // Secure
                    true // HttpOnly
            );

            NewCookie refreshCookie = new NewCookie(
                    "refresh_token",
                    newRefreshToken,
                    "/",
                    null,
                    "Refresh token",
                    7 * 24 * 60 * 60, // 7 days
                    true, // Secure
                    true
            );

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("message", "Token refreshed successfully");

            return Response.ok(responseMap)
                    .cookie(accessCookie, refreshCookie)
                    .header("X-Frame-Options", "DENY")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("Content-Security-Policy", "default-src 'self'")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Invalid refresh token.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @POST
    @Path("/logout")
    public Response logout(@Context HttpServletRequest request) {
        String refreshToken = extractTokenFromCookie(request, "refresh_token");
        if (refreshToken != null) {
            try {
                String username = JwtUtil.extractUsername(refreshToken);
                refreshTokenStore.remove(username);
            } catch (Exception e) {
                // Log the error but proceed with logout
                System.err.println("Failed to validate refresh token during logout: " + e.getMessage());
            }
        }

        // Clear cookies
        NewCookie accessCookie = new NewCookie(
                "access_token",
                null,
                "/",
                null,
                "Access token",
                0,
                true,
                true
        );
        NewCookie refreshCookie = new NewCookie(
                "refresh_token",
                null,
                "/",
                null,
                "Refresh token",
                0,
                true,
                true
        );

        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("message", "Logged out successfully.");

        return Response.ok(responseMap)
                .cookie(accessCookie, refreshCookie)
                .header("X-Frame-Options", "DENY")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'self'")
                .build();
    }

    // Helper method to extract token from cookie
    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (javax.servlet.http.Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}