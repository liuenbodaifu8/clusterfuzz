package com.google.clusterfuzz.web.service;

import com.google.clusterfuzz.web.dto.AuthenticationRequest;
import com.google.clusterfuzz.web.dto.AuthenticationResponse;
import com.google.clusterfuzz.web.dto.RefreshTokenRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication service handling JWT-based authentication operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    
    // Simple in-memory token blacklist (in production, use Redis or database)
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    /**
     * Authenticate user and generate JWT tokens.
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Authenticate user credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        
        // Generate tokens
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("rememberMe", request.isRememberMe());
        
        String accessToken = jwtService.generateToken(extraClaims, userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Extract roles
        var roles = userDetails.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .toList();

        LocalDateTime now = LocalDateTime.now();
        long expiresInSeconds = jwtService.getExpirationTime() / 1000;

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .email(userDetails.getUsername())
                .roles(roles)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresInSeconds))
                .build();
    }

    /**
     * Refresh access token using refresh token.
     */
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        // Validate refresh token
        if (blacklistedTokens.contains(refreshToken)) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        String userEmail = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Generate new access token
        String newAccessToken = jwtService.generateToken(userDetails);
        
        // Extract roles
        var roles = userDetails.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .toList();

        LocalDateTime now = LocalDateTime.now();
        long expiresInSeconds = jwtService.getExpirationTime() / 1000;

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Keep the same refresh token
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .email(userDetails.getUsername())
                .roles(roles)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresInSeconds))
                .build();
    }

    /**
     * Logout user by blacklisting tokens.
     */
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            blacklistedTokens.add(token);
            log.debug("Token blacklisted for logout");
        }
    }

    /**
     * Validate JWT token.
     */
    public boolean validateToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        String token = authHeader.substring(7);
        
        if (blacklistedTokens.contains(token)) {
            return false;
        }

        try {
            String userEmail = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            return jwtService.isTokenValid(token, userDetails);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get current user information from token.
     */
    public Map<String, Object> getCurrentUserInfo(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid authorization header");
        }

        String token = authHeader.substring(7);
        
        if (blacklistedTokens.contains(token)) {
            throw new RuntimeException("Token has been revoked");
        }

        String userEmail = jwtService.extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        if (!jwtService.isTokenValid(token, userDetails)) {
            throw new RuntimeException("Invalid token");
        }

        var roles = userDetails.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .toList();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("email", userDetails.getUsername());
        userInfo.put("roles", roles);
        userInfo.put("authorities", jwtService.extractAuthorities(token));
        
        return userInfo;
    }

    /**
     * Check if token is blacklisted.
     */
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    /**
     * Clear expired tokens from blacklist (cleanup method).
     */
    public void cleanupBlacklistedTokens() {
        // In production, implement proper cleanup based on token expiration
        // For now, this is a placeholder for the cleanup logic
        log.debug("Cleanup blacklisted tokens - {} tokens in blacklist", blacklistedTokens.size());
    }
}