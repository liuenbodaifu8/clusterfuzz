package com.google.clusterfuzz.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Authentication response DTO containing JWT tokens and user information.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Authentication response containing JWT tokens and user info")
public class AuthenticationResponse {

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "JWT refresh token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Token expiration time in seconds", example = "3600")
    private long expiresIn;

    @Schema(description = "User email", example = "user@example.com")
    private String email;

    @Schema(description = "User roles", example = "[\"ADMIN\", \"USER\"]")
    private List<String> roles;

    @Schema(description = "Token issued at timestamp")
    private LocalDateTime issuedAt;

    @Schema(description = "Token expires at timestamp")
    private LocalDateTime expiresAt;
}