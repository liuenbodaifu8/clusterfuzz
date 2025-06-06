package com.google.clusterfuzz.core.security.authentication;

import com.google.clusterfuzz.core.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for JWT token management including generation, validation, and refresh.
 */
@Service
@Slf4j
public class JwtTokenService {

    private final SecretKey secretKey;
    private final int expirationTime;
    private final int refreshExpirationTime;
    private final JwtParser jwtParser;

    public JwtTokenService(
            @Value("${clusterfuzz.security.jwt.secret-key:default-secret-key-change-in-production}") String secretKeyString,
            @Value("${clusterfuzz.security.jwt.expiration-time:3600}") int expirationTime,
            @Value("${clusterfuzz.security.jwt.refresh-expiration-time:86400}") int refreshExpirationTime) {
        
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
        this.expirationTime = expirationTime;
        this.refreshExpirationTime = refreshExpirationTime;
        this.jwtParser = Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build();
    }

    /**
     * Generates JWT access token for authenticated user.
     */
    public String generateAccessToken(Authentication authentication) {
        return generateAccessToken(authentication.getName(), 
            authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
    }

    /**
     * Generates JWT access token for user.
     */
    public String generateAccessToken(User user) {
        return generateAccessToken(user.getEmail(), 
            user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName())
                .collect(Collectors.toList()));
    }

    /**
     * Generates JWT access token with custom claims.
     */
    public String generateAccessToken(String subject, java.util.List<String> authorities) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationTime, ChronoUnit.SECONDS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", authorities);
        claims.put("token_type", "access");
        claims.put("iat", now.getEpochSecond());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiry))
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact();
    }

    /**
     * Generates JWT refresh token.
     */
    public String generateRefreshToken(String subject) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshExpirationTime, ChronoUnit.SECONDS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "refresh");
        claims.put("iat", now.getEpochSecond());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiry))
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact();
    }

    /**
     * Validates JWT token and returns claims.
     */
    public Claims validateToken(String token) {
        try {
            return jwtParser.parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            throw new JwtAuthenticationException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw new JwtAuthenticationException("Unsupported token", e);
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw new JwtAuthenticationException("Malformed token", e);
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw new JwtAuthenticationException("Invalid signature", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            throw new JwtAuthenticationException("Invalid token", e);
        }
    }

    /**
     * Extracts username from JWT token.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * Extracts authorities from JWT token.
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> getAuthoritiesFromToken(String token) {
        Claims claims = validateToken(token);
        return (java.util.List<String>) claims.get("authorities");
    }

    /**
     * Checks if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtAuthenticationException e) {
            return true;
        }
    }

    /**
     * Checks if token is a refresh token.
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = validateToken(token);
            return "refresh".equals(claims.get("token_type"));
        } catch (JwtAuthenticationException e) {
            return false;
        }
    }

    /**
     * Refreshes access token using refresh token.
     */
    public TokenPair refreshAccessToken(String refreshToken, java.util.List<String> authorities) {
        if (!isRefreshToken(refreshToken)) {
            throw new JwtAuthenticationException("Invalid refresh token");
        }

        Claims claims = validateToken(refreshToken);
        String subject = claims.getSubject();

        String newAccessToken = generateAccessToken(subject, authorities);
        String newRefreshToken = generateRefreshToken(subject);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    /**
     * Generates token pair (access + refresh tokens).
     */
    public TokenPair generateTokenPair(String subject, java.util.List<String> authorities) {
        String accessToken = generateAccessToken(subject, authorities);
        String refreshToken = generateRefreshToken(subject);
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Token pair containing access and refresh tokens.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
    }

    /**
     * JWT authentication exception.
     */
    public static class JwtAuthenticationException extends RuntimeException {
        public JwtAuthenticationException(String message) {
            super(message);
        }

        public JwtAuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}