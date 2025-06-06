package com.google.clusterfuzz.web.security;

import com.google.clusterfuzz.web.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JWT service functionality.
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        
        // Set test configuration
        ReflectionTestUtils.setField(jwtService, "secretKey", 
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L); // 24 hours
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L); // 7 days
        
        testUser = User.builder()
            .username("test@example.com")
            .password("password")
            .authorities(List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
            ))
            .build();
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void testGenerateToken() {
        String token = jwtService.generateToken(testUser);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    @DisplayName("Should extract username from token")
    void testExtractUsername() {
        String token = jwtService.generateToken(testUser);
        String extractedUsername = jwtService.extractUsername(token);
        
        assertEquals(testUser.getUsername(), extractedUsername);
    }

    @Test
    @DisplayName("Should validate token correctly")
    void testTokenValidation() {
        String token = jwtService.generateToken(testUser);
        
        assertTrue(jwtService.isTokenValid(token, testUser));
    }

    @Test
    @DisplayName("Should extract authorities from token")
    void testExtractAuthorities() {
        String token = jwtService.generateToken(testUser);
        String[] authorities = jwtService.extractAuthorities(token);
        
        assertNotNull(authorities);
        assertEquals(2, authorities.length);
        assertTrue(List.of(authorities).contains("ROLE_USER"));
        assertTrue(List.of(authorities).contains("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Should generate refresh token")
    void testGenerateRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(testUser);
        
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        assertTrue(refreshToken.split("\\.").length == 3);
    }

    @Test
    @DisplayName("Should validate token format")
    void testTokenFormatValidation() {
        String validToken = jwtService.generateToken(testUser);
        String invalidToken = "invalid.token.format";
        
        assertTrue(jwtService.isTokenFormatValid(validToken));
        assertFalse(jwtService.isTokenFormatValid(invalidToken));
    }

    @Test
    @DisplayName("Should return correct expiration times")
    void testExpirationTimes() {
        assertEquals(86400000L, jwtService.getExpirationTime());
        assertEquals(604800000L, jwtService.getRefreshExpirationTime());
    }

    @Test
    @DisplayName("Should handle invalid tokens gracefully")
    void testInvalidTokenHandling() {
        assertThrows(Exception.class, () -> {
            jwtService.extractUsername("invalid.token");
        });
        
        assertThrows(Exception.class, () -> {
            jwtService.isTokenValid("invalid.token", testUser);
        });
    }
}