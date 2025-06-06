package com.google.clusterfuzz.core.service;

import com.google.clusterfuzz.core.entity.Role;
import com.google.clusterfuzz.core.entity.User;
import com.google.clusterfuzz.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for managing users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;

    /**
     * Creates a new user.
     */
    @CacheEvict(value = {"users", "userRoles"}, allEntries = true)
    public User createUser(User user) {
        validateUser(user);
        
        // Assign default roles if none specified
        if (user.getRoles().isEmpty()) {
            user.setRoles(getDefaultRoles());
        }
        
        User savedUser = userRepository.save(user);
        log.info("Created user: {}", savedUser.getEmail());
        
        return savedUser;
    }

    /**
     * Updates an existing user.
     */
    @CacheEvict(value = {"users", "userRoles"}, allEntries = true)
    public User updateUser(User user) {
        validateUser(user);
        
        User savedUser = userRepository.save(user);
        log.info("Updated user: {}", savedUser.getEmail());
        
        return savedUser;
    }

    /**
     * Saves user (create or update).
     */
    @CacheEvict(value = {"users", "userRoles"}, allEntries = true)
    public User save(User user) {
        if (user.getId() == null) {
            return createUser(user);
        } else {
            return updateUser(user);
        }
    }

    /**
     * Deletes a user.
     */
    @CacheEvict(value = {"users", "userRoles"}, allEntries = true)
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        userRepository.delete(user);
        log.info("Deleted user: {}", user.getEmail());
    }

    /**
     * Deactivates a user instead of deleting.
     */
    @CacheEvict(value = {"users", "userRoles"}, allEntries = true)
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.setActive(false);
        userRepository.save(user);
        log.info("Deactivated user: {}", user.getEmail());
    }

    /**
     * Activates a user.
     */
    @CacheEvict(value = {"users", "userRoles"}, allEntries = true)
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.setActive(true);
        userRepository.save(user);
        log.info("Activated user: {}", user.getEmail());
    }

    /**
     * Finds user by ID.
     */
    @Cacheable(value = "users", key = "#userId")
    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Finds user by email.
     */
    @Cacheable(value = "users", key = "#email")
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Finds user by OAuth2 provider and provider ID.
     */
    @Cacheable(value = "users", key = "#provider + ':' + #providerId")
    @Transactional(readOnly = true)
    public Optional<User> findByOAuth2Provider(String provider, String providerId) {
        return userRepository.findByOauth2ProviderAndOauth2ProviderId(provider, providerId);
    }

    /**
     * Finds user by Firebase UID.
     */
    @Cacheable(value = "users", key = "'firebase:' + #firebaseUid")
    @Transactional(readOnly = true)
    public Optional<User> findByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid);
    }

    /**
     * Gets all active users.
     */
    @Cacheable(value = "users", key = "'all-active'")
    @Transactional(readOnly = true)
    public List<User> findAllActive() {
        return userRepository.findByActiveTrue();
    }

    /**
     * Gets all users.
     */
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Finds users with a specific role.
     */
    @Cacheable(value = "users", key = "'role:' + #roleName")
    @Transactional(readOnly = true)
    public List<User> findByRole(String roleName) {
        return userRepository.findByRoleName(roleName);
    }

    /**
     * Finds inactive users since a specific date.
     */
    @Transactional(readOnly = true)
    public List<User> findInactiveUsersSince(LocalDateTime cutoffDate) {
        return userRepository.findInactiveUsersSince(cutoffDate);
    }

    /**
     * Adds role to user.
     */
    @CacheEvict(value = {"users", "userRoles"}, allEntries = true)
    public void addRoleToUser(String userEmail, String roleName) {
        User user = findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        
        Role role = roleService.findByName(roleName)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        
        user.addRole(role);
        userRepository.save(user);
        
        log.info("Added role {} to user {}", roleName, userEmail);
    }

    /**
     * Removes role from user.
     */
    @CacheEvict(value = {"users", "userRoles"}, allEntries = true)
    public void removeRoleFromUser(String userEmail, String roleName) {
        User user = findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        
        Role role = roleService.findByName(roleName)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        
        user.removeRole(role);
        userRepository.save(user);
        
        log.info("Removed role {} from user {}", roleName, userEmail);
    }

    /**
     * Updates user roles from IAM.
     */
    @CacheEvict(value = {"users", "userRoles"}, allEntries = true)
    public void updateUserRolesFromIam(User user, Set<String> iamRoles) {
        // This would map IAM roles to local roles
        // Implementation depends on your IAM role mapping strategy
        
        for (String iamRole : iamRoles) {
            String localRoleName = mapIamRoleToLocal(iamRole);
            if (localRoleName != null) {
                Optional<Role> role = roleService.findByName(localRoleName);
                if (role.isPresent() && !user.getRoles().contains(role.get())) {
                    user.addRole(role.get());
                }
            }
        }
        
        userRepository.save(user);
        log.info("Updated user roles from IAM for: {}", user.getEmail());
    }

    /**
     * Gets default roles for new users.
     */
    @Cacheable(value = "userRoles", key = "'default'")
    @Transactional(readOnly = true)
    public Set<Role> getDefaultRoles() {
        return roleService.getDefaultRoles();
    }

    /**
     * Checks if user has permission.
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(String userEmail, String permission) {
        Optional<User> userOpt = findByEmail(userEmail);
        return userOpt.map(user -> user.hasPermission(permission)).orElse(false);
    }

    /**
     * Checks if user has role.
     */
    @Transactional(readOnly = true)
    public boolean hasRole(String userEmail, String roleName) {
        Optional<User> userOpt = findByEmail(userEmail);
        return userOpt.map(user -> user.hasRole(roleName)).orElse(false);
    }

    /**
     * Updates user last login time.
     */
    @CacheEvict(value = "users", key = "#userEmail")
    public void updateLastLogin(String userEmail) {
        Optional<User> userOpt = findByEmail(userEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    /**
     * Updates user last logout time.
     */
    @CacheEvict(value = "users", key = "#userEmail")
    public void updateLastLogout(String userEmail) {
        Optional<User> userOpt = findByEmail(userEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLogoutAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    /**
     * Maps IAM role to local role.
     */
    private String mapIamRoleToLocal(String iamRole) {
        // This would contain your IAM to local role mapping logic
        // For example:
        if (iamRole.contains("admin")) {
            return "ADMIN";
        } else if (iamRole.contains("user")) {
            return "USER";
        } else if (iamRole.contains("bot")) {
            return "BOT";
        }
        return null;
    }

    /**
     * Validates user data.
     */
    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email is required");
        }
        
        if (!user.getEmail().contains("@")) {
            throw new IllegalArgumentException("Valid email address is required");
        }
        
        // Check for duplicate email (excluding current user)
        Optional<User> existingUser = findByEmail(user.getEmail());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
            throw new IllegalArgumentException("User email already exists: " + user.getEmail());
        }
    }
}