package com.google.clusterfuzz.core.security.authentication;

import com.google.clusterfuzz.core.entity.User;
import com.google.clusterfuzz.core.security.config.GoogleCloudSecurityConfig;
import com.google.clusterfuzz.core.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service for synchronizing user data between different authentication providers
 * and the local database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSynchronizationService {

    private final UserService userService;
    private final FirebaseAuthenticationService firebaseAuthenticationService;
    private final GoogleCloudSecurityConfig.GoogleCloudIamService googleCloudIamService;

    /**
     * Synchronizes user data from all authentication providers.
     */
    @Async
    @Transactional
    public CompletableFuture<Void> syncAllUsers() {
        log.info("Starting user synchronization from all providers");
        
        try {
            List<User> users = userService.findAll();
            
            for (User user : users) {
                syncUserData(user);
            }
            
            log.info("Completed user synchronization for {} users", users.size());
            
        } catch (Exception e) {
            log.error("Error during user synchronization", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Synchronizes individual user data.
     */
    @Transactional
    public void syncUserData(User user) {
        try {
            // Sync Firebase custom claims
            syncFirebaseCustomClaims(user);
            
            // Sync Google Cloud IAM roles
            syncGoogleCloudRoles(user);
            
            // Update last sync time
            user.setLastSyncAt(LocalDateTime.now());
            userService.save(user);
            
            log.debug("Synchronized user data for: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Error synchronizing user data for: {}", user.getEmail(), e);
        }
    }

    /**
     * Synchronizes Firebase custom claims for user.
     */
    private void syncFirebaseCustomClaims(User user) {
        if (user.getFirebaseUid() != null) {
            try {
                firebaseAuthenticationService.updateUserRoles(user);
            } catch (Exception e) {
                log.warn("Failed to sync Firebase custom claims for user: {}", user.getEmail(), e);
            }
        }
    }

    /**
     * Synchronizes Google Cloud IAM roles for user.
     */
    private void syncGoogleCloudRoles(User user) {
        try {
            Set<String> iamRoles = googleCloudIamService.getUserRoles(user.getEmail());
            
            // Update user roles based on IAM roles
            if (!iamRoles.isEmpty()) {
                userService.updateUserRolesFromIam(user, iamRoles);
            }
            
        } catch (Exception e) {
            log.warn("Failed to sync Google Cloud IAM roles for user: {}", user.getEmail(), e);
        }
    }

    /**
     * Scheduled task to sync users periodically.
     */
    @Scheduled(fixedRateString = "${clusterfuzz.security.sync.interval:3600000}") // Default: 1 hour
    public void scheduledUserSync() {
        log.debug("Starting scheduled user synchronization");
        syncAllUsers();
    }

    /**
     * Synchronizes user permissions with external providers.
     */
    @Async
    public CompletableFuture<Void> syncUserPermissions(String userEmail) {
        try {
            User user = userService.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
            
            syncUserData(user);
            
        } catch (Exception e) {
            log.error("Error synchronizing permissions for user: {}", userEmail, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Validates user access across all providers.
     */
    public boolean validateUserAccess(String userEmail, String permission) {
        try {
            // Check local permissions
            User user = userService.findByEmail(userEmail).orElse(null);
            if (user == null || !user.isActive()) {
                return false;
            }
            
            boolean hasLocalPermission = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(p -> p.getName().equals(permission));
            
            if (!hasLocalPermission) {
                return false;
            }
            
            // Validate with Google Cloud IAM if available
            return googleCloudIamService.hasPermission(userEmail, permission);
            
        } catch (Exception e) {
            log.error("Error validating user access for: {} permission: {}", userEmail, permission, e);
            return false;
        }
    }

    /**
     * Handles user login event and triggers synchronization.
     */
    @Async
    public CompletableFuture<Void> handleUserLogin(String userEmail) {
        try {
            User user = userService.findByEmail(userEmail).orElse(null);
            if (user != null) {
                user.setLastLoginAt(LocalDateTime.now());
                userService.save(user);
                
                // Trigger async synchronization
                syncUserData(user);
            }
            
        } catch (Exception e) {
            log.error("Error handling user login for: {}", userEmail, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Handles user logout event.
     */
    @Async
    public CompletableFuture<Void> handleUserLogout(String userEmail) {
        try {
            User user = userService.findByEmail(userEmail).orElse(null);
            if (user != null) {
                user.setLastLogoutAt(LocalDateTime.now());
                userService.save(user);
            }
            
        } catch (Exception e) {
            log.error("Error handling user logout for: {}", userEmail, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Synchronizes roles from Google Cloud IAM.
     */
    @Scheduled(fixedRateString = "${clusterfuzz.security.iam.sync-interval:3600000}") // Default: 1 hour
    public void syncRolesFromIam() {
        try {
            googleCloudIamService.syncRolesFromIam();
        } catch (Exception e) {
            log.error("Error synchronizing roles from Google Cloud IAM", e);
        }
    }

    /**
     * Cleans up inactive users.
     */
    @Scheduled(cron = "${clusterfuzz.security.cleanup.cron:0 0 2 * * ?}") // Default: 2 AM daily
    public void cleanupInactiveUsers() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(90); // 90 days inactive
            List<User> inactiveUsers = userService.findInactiveUsersSince(cutoff);
            
            for (User user : inactiveUsers) {
                if (user.getFirebaseUid() != null) {
                    try {
                        firebaseAuthenticationService.disableUser(user.getFirebaseUid());
                    } catch (Exception e) {
                        log.warn("Failed to disable Firebase user: {}", user.getEmail(), e);
                    }
                }
                
                user.setActive(false);
                userService.save(user);
            }
            
            log.info("Cleaned up {} inactive users", inactiveUsers.size());
            
        } catch (Exception e) {
            log.error("Error during inactive user cleanup", e);
        }
    }
}