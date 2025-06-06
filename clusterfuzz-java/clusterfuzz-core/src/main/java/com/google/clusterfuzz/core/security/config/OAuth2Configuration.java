package com.google.clusterfuzz.core.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * OAuth2 configuration for Google Cloud Identity integration.
 * Supports multiple OAuth2 providers for different environments.
 */
@Configuration
@ConfigurationProperties(prefix = "clusterfuzz.security.oauth2")
@Data
public class OAuth2Configuration {

    private Google google = new Google();
    private Github github = new Github();
    private boolean enabled = true;

    /**
     * Client registration repository with configured OAuth2 providers.
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
            googleClientRegistration(),
            githubClientRegistration()
        );
    }

    /**
     * Google OAuth2 client registration.
     */
    private ClientRegistration googleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
            .clientId(google.getClientId())
            .clientSecret(google.getClientSecret())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/api/v1/auth/oauth2/callback/{registrationId}")
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://www.googleapis.com/oauth2/v4/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
            .userNameAttributeName("sub")
            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .clientName("Google")
            .build();
    }

    /**
     * GitHub OAuth2 client registration (for development/testing).
     */
    private ClientRegistration githubClientRegistration() {
        return ClientRegistration.withRegistrationId("github")
            .clientId(github.getClientId())
            .clientSecret(github.getClientSecret())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/api/v1/auth/oauth2/callback/{registrationId}")
            .scope("read:user", "user:email")
            .authorizationUri("https://github.com/login/oauth/authorize")
            .tokenUri("https://github.com/login/oauth/access_token")
            .userInfoUri("https://api.github.com/user")
            .userNameAttributeName("id")
            .clientName("GitHub")
            .build();
    }

    @Data
    public static class Google {
        private String clientId = "${GOOGLE_OAUTH2_CLIENT_ID:}";
        private String clientSecret = "${GOOGLE_OAUTH2_CLIENT_SECRET:}";
        private String hostedDomain = "${GOOGLE_OAUTH2_HOSTED_DOMAIN:}";
        private boolean restrictToDomain = false;
    }

    @Data
    public static class Github {
        private String clientId = "${GITHUB_OAUTH2_CLIENT_ID:}";
        private String clientSecret = "${GITHUB_OAUTH2_CLIENT_SECRET:}";
        private String organization = "${GITHUB_OAUTH2_ORGANIZATION:}";
        private boolean restrictToOrganization = false;
    }
}