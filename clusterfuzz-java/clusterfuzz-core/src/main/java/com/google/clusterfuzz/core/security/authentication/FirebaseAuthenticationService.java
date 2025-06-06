package com.google.clusterfuzz.core.security.authentication;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.clusterfuzz.core.entity.User;
import com.google.clusterfuzz.core.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for Firebase authentication integration.
 * Handles Firebase token validation and user synchronization.
 */
@Service
@Slf4j
public class FirebaseAuthenticationService {

    private final UserService userService;
    private final String serviceAccountKeyPath;
    private final String projectId;
    private final boolean enabled;
    
    private FirebaseAuth firebaseAuth;

    public FirebaseAuthenticationService(
            UserService userService,
            @Value("${clusterfuzz.security.firebase.service-account-key-path:}") String serviceAccountKeyPath,
            @Value("${clusterfuzz.security.firebase.project-id:}") String projectId,
            @Value("${clusterfuzz.security.firebase.enabled:false}") boolean enabled) {
        
        this.userService = userService;
        this.serviceAccountKeyPath = serviceAccountKeyPath;
        this.projectId = projectId;
        this.enabled = enabled;
    }

    /**
     * Initializes Firebase Admin SDK.
     */
    @PostConstruct
    public void initializeFirebase() {
        if (!enabled) {
            log.info("Firebase authentication is disabled");
            return;
        }

        try {
            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder();
            
            if (serviceAccountKeyPath != null && !serviceAccountKeyPath.isEmpty()) {
                FileInputStream serviceAccount = new FileInputStream(serviceAccountKeyPath);
                optionsBuilder.setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(serviceAccount));
            } else {
                // Use default credentials (ADC)
                optionsBuilder.setCredentials(com.google.auth.oauth2.GoogleCredentials.getApplicationDefault());
            }
            
            if (projectId != null && !projectId.isEmpty()) {
                optionsBuilder.setProjectId(projectId);
            }

            FirebaseOptions options = optionsBuilder.build();
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            
            this.firebaseAuth = FirebaseAuth.getInstance();
            log.info("Firebase authentication initialized successfully");
            
        } catch (IOException e) {
            log.error("Failed to initialize Firebase authentication", e);
            this.firebaseAuth = null;
        }
    }

    /**
     * Validates Firebase ID token and returns user information.
     */
    public FirebaseUserInfo validateToken(String idToken) throws FirebaseAuthenticationException {
        if (!enabled || firebaseAuth == null) {
            throw new FirebaseAuthenticationException("Firebase authentication not available");
        }

        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String picture = decodedToken.getPicture();
            boolean emailVerified = decodedToken.isEmailVerified();

            return FirebaseUserInfo.builder()
                .uid(uid)
                .email(email)
                .name(name)
                .picture(picture)
                .emailVerified(emailVerified)
                .claims(decodedToken.getClaims())
                .build();

        } catch (FirebaseAuthException e) {
            log.warn("Firebase token validation failed: {}", e.getMessage());
            throw new FirebaseAuthenticationException("Invalid Firebase token", e);
        }
    }

    /**
     * Synchronizes Firebase user with local database.
     */
    public User syncFirebaseUser(FirebaseUserInfo firebaseUser) {
        Optional<User> existingUser = userService.findByEmail(firebaseUser.getEmail());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            updateUserFromFirebase(user, firebaseUser);
            return userService.save(user);
        } else {
            User newUser = createUserFromFirebase(firebaseUser);
            return userService.save(newUser);
        }
    }

    /**
     * Sets custom claims for Firebase user.
     */
    public void setCustomClaims(String uid, Map<String, Object> claims) throws FirebaseAuthenticationException {
        if (!enabled || firebaseAuth == null) {
            throw new FirebaseAuthenticationException("Firebase authentication not available");
        }

        try {
            firebaseAuth.setCustomUserClaims(uid, claims);
            log.debug("Set custom claims for Firebase user: {}", uid);
        } catch (FirebaseAuthException e) {
            log.error("Failed to set custom claims for Firebase user: {}", uid, e);
            throw new FirebaseAuthenticationException("Failed to set custom claims", e);
        }
    }

    /**
     * Updates user roles in Firebase custom claims.
     */
    public void updateUserRoles(User user) {
        if (!enabled || firebaseAuth == null || user.getFirebaseUid() == null) {
            return;
        }

        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", user.getRoles().stream()
                .map(role -> role.getName())
                .toArray(String[]::new));
            
            claims.put("permissions", user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName())
                .distinct()
                .toArray(String[]::new));

            setCustomClaims(user.getFirebaseUid(), claims);
            
        } catch (FirebaseAuthenticationException e) {
            log.error("Failed to update Firebase custom claims for user: {}", user.getEmail(), e);
        }
    }

    /**
     * Gets Firebase user by UID.
     */
    public UserRecord getFirebaseUser(String uid) throws FirebaseAuthenticationException {
        if (!enabled || firebaseAuth == null) {
            throw new FirebaseAuthenticationException("Firebase authentication not available");
        }

        try {
            return firebaseAuth.getUser(uid);
        } catch (FirebaseAuthException e) {
            log.warn("Failed to get Firebase user: {}", uid, e);
            throw new FirebaseAuthenticationException("Failed to get Firebase user", e);
        }
    }

    /**
     * Disables Firebase user account.
     */
    public void disableUser(String uid) throws FirebaseAuthenticationException {
        if (!enabled || firebaseAuth == null) {
            throw new FirebaseAuthenticationException("Firebase authentication not available");
        }

        try {
            UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                .setDisabled(true);
            firebaseAuth.updateUser(request);
            log.info("Disabled Firebase user: {}", uid);
        } catch (FirebaseAuthException e) {
            log.error("Failed to disable Firebase user: {}", uid, e);
            throw new FirebaseAuthenticationException("Failed to disable Firebase user", e);
        }
    }

    /**
     * Updates existing user with Firebase information.
     */
    private void updateUserFromFirebase(User user, FirebaseUserInfo firebaseUser) {
        user.setName(firebaseUser.getName());
        user.setPictureUrl(firebaseUser.getPicture());
        user.setEmailVerified(firebaseUser.isEmailVerified());
        user.setFirebaseUid(firebaseUser.getUid());
        user.setLastLoginAt(LocalDateTime.now());
    }

    /**
     * Creates new user from Firebase information.
     */
    private User createUserFromFirebase(FirebaseUserInfo firebaseUser) {
        User user = new User();
        user.setEmail(firebaseUser.getEmail());
        user.setName(firebaseUser.getName());
        user.setPictureUrl(firebaseUser.getPicture());
        user.setEmailVerified(firebaseUser.isEmailVerified());
        user.setFirebaseUid(firebaseUser.getUid());
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        user.setActive(true);
        
        // Assign default roles
        user.setRoles(userService.getDefaultRoles());
        
        return user;
    }

    /**
     * Firebase user information.
     */
    @lombok.Builder
    @lombok.Data
    public static class FirebaseUserInfo {
        private String uid;
        private String email;
        private String name;
        private String picture;
        private boolean emailVerified;
        private Map<String, Object> claims;
    }

    /**
     * Firebase authentication exception.
     */
    public static class FirebaseAuthenticationException extends Exception {
        public FirebaseAuthenticationException(String message) {
            super(message);
        }

        public FirebaseAuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}