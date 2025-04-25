package com.example.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class JwtUtil {
    // Use a fixed secret key from configuration
    private static final String SECRET_KEY_STRING = System.getenv("JWT_SECRET_KEY") != null
            ? System.getenv("JWT_SECRET_KEY")
            : "your-secure-secret-key-with-at-least-512-bits-length"; // Fallback for development
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_TIME = 864_000_000; // 10 days in milliseconds
    private static final long ACCESS_TOKEN_EXPIRY = 15 * 60 * 1000; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY = 7 * 24 * 60 * 60 * 1000; // 7 days
    public static final String TOKEN_COOKIE_NAME = "access-token";

    // Rest of the methods remain unchanged
    public static String generateToken(String username, String role) {
        if (username == null || role == null) {
            throw new IllegalArgumentException("Username and role cannot be null");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static boolean validateToken(String token) {
        if (token == null) {
            return false;
        }
        try {
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public static String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    private static <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private static Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static String generateAccessToken(String username, String role) {
        if (username == null || role == null) {
            throw new IllegalArgumentException("Username and role cannot be null");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static String generateNewRefreshToken(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }

        return Jwts.builder()
                .setSubject(username)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY))
                .signWith(SECRET_KEY)
                .compact();
    }

}