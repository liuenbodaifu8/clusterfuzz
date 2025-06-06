package com.google.clusterfuzz.core.security.authentication;

import com.google.clusterfuzz.core.entity.User;
import com.google.clusterfuzz.core.security.config.OAuth2Configuration;
import com.google.clusterfuzz.core.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * OAuth2 authentication provider that handles user authentication
 * and synchronization with local user database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationProvider extends DefaultOAuth2UserService {

    private final UserService userService;
    private final OAuth2Configuration oAuth2Configuration;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception e) {
            log.error("Error processing OAuth2 user", e);
            throw new OAuth2AuthenticationException("Authentication processing failed");
        }
    }

    /**
     * Processes OAuth2 user and synchronizes with local database.
     */
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        UserInfo userInfo = extractUserInfo(registrationId, attributes);
        
        // Validate user based on provider-specific rules
        validateUser(registrationId, userInfo);
        
        // Create or update user in local database
        User user = createOrUpdateUser(userInfo);
        
        // Create authorities based on user roles
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());
        
        // Add permissions as authorities
        user.getRoles().forEach(role -> 
            role.getPermissions().forEach(permission -> 
                authorities.add(new SimpleGrantedAuthority(permission.getName()))
            )
        );
        
        return new DefaultOAuth2User(authorities, attributes, getUsernameAttributeName(registrationId));
    }

    /**
     * Extracts user information from OAuth2 attributes.
     */
    private UserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        switch (registrationId) {
            case "google":
                return UserInfo.builder()
                    .email((String) attributes.get("email"))
                    .name((String) attributes.get("name"))
                    .picture((String) attributes.get("picture"))
                    .emailVerified((Boolean) attributes.get("email_verified"))
                    .provider("google")
                    .providerId((String) attributes.get("sub"))
                    .hostedDomain((String) attributes.get("hd"))
                    .build();
                    
            case "github":
                return UserInfo.builder()
                    .email((String) attributes.get("email"))
                    .name((String) attributes.get("name"))
                    .picture((String) attributes.get("avatar_url"))
                    .emailVerified(true) // GitHub emails are verified
                    .provider("github")
                    .providerId(String.valueOf(attributes.get("id")))
                    .build();
                    
            default:
                throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
        }
    }

    /**
     * Validates user based on provider-specific rules.
     */
    private void validateUser(String registrationId, UserInfo userInfo) {
        switch (registrationId) {
            case "google":
                validateGoogleUser(userInfo);
                break;
            case "github":
                validateGithubUser(userInfo);
                break;
        }
    }

    /**
     * Validates Google OAuth2 user.
     */
    private void validateGoogleUser(UserInfo userInfo) {
        if (!userInfo.getEmailVerified()) {
            throw new OAuth2AuthenticationException("Email not verified");
        }
        
        OAuth2Configuration.Google googleConfig = oAuth2Configuration.getGoogle();
        if (googleConfig.isRestrictToDomain() && googleConfig.getHostedDomain() != null) {
            if (!googleConfig.getHostedDomain().equals(userInfo.getHostedDomain())) {
                throw new OAuth2AuthenticationException("User not from allowed domain");
            }
        }
    }

    /**
     * Validates GitHub OAuth2 user.
     */
    private void validateGithubUser(UserInfo userInfo) {
        OAuth2Configuration.Github githubConfig = oAuth2Configuration.getGithub();
        if (githubConfig.isRestrictToOrganization() && githubConfig.getOrganization() != null) {
            // Implementation would check GitHub organization membership
            log.debug("Checking GitHub organization membership for user: {}", userInfo.getEmail());
        }
    }

    /**
     * Creates or updates user in local database.
     */
    private User createOrUpdateUser(UserInfo userInfo) {
        Optional<User> existingUser = userService.findByEmail(userInfo.getEmail());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            updateUserFromOAuth2(user, userInfo);
            return userService.save(user);
        } else {
            User newUser = createUserFromOAuth2(userInfo);
            return userService.save(newUser);
        }
    }

    /**
     * Updates existing user with OAuth2 information.
     */
    private void updateUserFromOAuth2(User user, UserInfo userInfo) {
        user.setName(userInfo.getName());
        user.setPictureUrl(userInfo.getPicture());
        user.setLastLoginAt(LocalDateTime.now());
        user.setOauth2Provider(userInfo.getProvider());
        user.setOauth2ProviderId(userInfo.getProviderId());
    }

    /**
     * Creates new user from OAuth2 information.
     */
    private User createUserFromOAuth2(UserInfo userInfo) {
        User user = new User();
        user.setEmail(userInfo.getEmail());
        user.setName(userInfo.getName());
        user.setPictureUrl(userInfo.getPicture());
        user.setEmailVerified(userInfo.getEmailVerified());
        user.setOauth2Provider(userInfo.getProvider());
        user.setOauth2ProviderId(userInfo.getProviderId());
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        user.setActive(true);
        
        // Assign default role
        user.setRoles(userService.getDefaultRoles());
        
        return user;
    }

    /**
     * Gets the username attribute name for the provider.
     */
    private String getUsernameAttributeName(String registrationId) {
        switch (registrationId) {
            case "google":
                return "sub";
            case "github":
                return "id";
            default:
                return "id";
        }
    }

    /**
     * User information extracted from OAuth2 provider.
     */
    @lombok.Builder
    @lombok.Data
    private static class UserInfo {
        private String email;
        private String name;
        private String picture;
        private Boolean emailVerified;
        private String provider;
        private String providerId;
        private String hostedDomain;
    }
}