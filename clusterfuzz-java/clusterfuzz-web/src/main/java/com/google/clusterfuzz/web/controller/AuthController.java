package com.google.clusterfuzz.web.controller;

import com.google.clusterfuzz.web.dto.AuthenticationRequest;
import com.google.clusterfuzz.web.dto.AuthenticationResponse;
import com.google.clusterfuzz.web.dto.RefreshTokenRequest;
import com.google.clusterfuzz.web.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller for JWT-based authentication.
 * Handles login, token refresh, and authentication operations.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and authorization operations")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticate user with email and password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful",
                content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Invalid request format")
    })
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        log.info("Authentication attempt for user: {}", request.getEmail());
        
        try {
            AuthenticationResponse response = authenticationService.authenticate(request);
            log.info("Authentication successful for user: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Authentication failed for user: {} - {}", request.getEmail(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token", description = "Refresh access token using refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refresh successful",
                content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token"),
        @ApiResponse(responseCode = "400", description = "Invalid request format")
    })
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        log.debug("Token refresh attempt");
        
        try {
            AuthenticationResponse response = authenticationService.refreshToken(request);
            log.debug("Token refresh successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidate user session and tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader
    ) {
        log.debug("Logout attempt");
        
        try {
            authenticationService.logout(authHeader);
            log.debug("Logout successful");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.warn("Logout failed: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT token", description = "Validate current JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    public ResponseEntity<Void> validateToken(
            @RequestHeader("Authorization") String authHeader
    ) {
        log.debug("Token validation attempt");
        
        try {
            boolean isValid = authenticationService.validateToken(authHeader);
            if (isValid) {
                log.debug("Token validation successful");
                return ResponseEntity.ok().build();
            } else {
                log.debug("Token validation failed");
                return ResponseEntity.status(401).build();
            }
        } catch (Exception e) {
            log.warn("Token validation error: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/user-info")
    @Operation(summary = "Get current user info", description = "Get information about currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User info retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<?> getCurrentUserInfo(
            @RequestHeader("Authorization") String authHeader
    ) {
        log.debug("User info request");
        
        try {
            var userInfo = authenticationService.getCurrentUserInfo(authHeader);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.warn("Failed to get user info: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }
}