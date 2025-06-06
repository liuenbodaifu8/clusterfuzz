package com.google.clusterfuzz.core.security.authorization;

import com.google.clusterfuzz.core.entity.User;
import com.google.clusterfuzz.core.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Optional;

/**
 * Custom permission evaluator for fine-grained access control.
 * Evaluates permissions based on user roles and resource ownership.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final UserService userService;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();
        String permissionName = permission.toString();

        log.debug("Evaluating permission {} for user {} on object {}", 
                 permissionName, username, targetDomainObject);

        try {
            Optional<User> userOpt = userService.findByEmail(username);
            if (userOpt.isEmpty()) {
                log.warn("User not found: {}", username);
                return false;
            }

            User user = userOpt.get();
            
            // Check if user is active
            if (!user.getActive()) {
                log.warn("User is inactive: {}", username);
                return false;
            }

            // Check basic permission
            if (user.hasPermission(permissionName)) {
                log.debug("User {} has permission {}", username, permissionName);
                return true;
            }

            // Check resource-specific permissions
            if (targetDomainObject != null) {
                return evaluateResourcePermission(user, targetDomainObject, permissionName);
            }

            log.debug("User {} does not have permission {}", username, permissionName);
            return false;

        } catch (Exception e) {
            log.error("Error evaluating permission {} for user {}", permissionName, username, e);
            return false;
        }
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, 
                               String targetType, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();
        String permissionName = permission.toString();

        log.debug("Evaluating permission {} for user {} on {} with ID {}", 
                 permissionName, username, targetType, targetId);

        try {
            Optional<User> userOpt = userService.findByEmail(username);
            if (userOpt.isEmpty()) {
                log.warn("User not found: {}", username);
                return false;
            }

            User user = userOpt.get();
            
            // Check if user is active
            if (!user.getActive()) {
                log.warn("User is inactive: {}", username);
                return false;
            }

            // Check basic permission
            if (user.hasPermission(permissionName)) {
                log.debug("User {} has permission {}", username, permissionName);
                return true;
            }

            // Check type-specific permissions
            return evaluateTypePermission(user, targetType, targetId, permissionName);

        } catch (Exception e) {
            log.error("Error evaluating permission {} for user {} on {} {}", 
                     permissionName, username, targetType, targetId, e);
            return false;
        }
    }

    /**
     * Evaluates permission on a specific resource object.
     */
    private boolean evaluateResourcePermission(User user, Object resource, String permission) {
        String resourceType = resource.getClass().getSimpleName().toLowerCase();
        
        // Check if user has permission for this resource type
        String resourcePermission = resourceType + ":" + extractAction(permission);
        if (user.hasPermission(resourcePermission)) {
            return true;
        }

        // Check ownership-based permissions
        return checkOwnership(user, resource, permission);
    }

    /**
     * Evaluates permission on a resource type with ID.
     */
    private boolean evaluateTypePermission(User user, String targetType, Serializable targetId, String permission) {
        String resourceType = targetType.toLowerCase();
        
        // Check if user has permission for this resource type
        String resourcePermission = resourceType + ":" + extractAction(permission);
        if (user.hasPermission(resourcePermission)) {
            return true;
        }

        // For specific resource instances, check ownership
        if (targetId != null) {
            return checkOwnershipById(user, resourceType, targetId, permission);
        }

        return false;
    }

    /**
     * Checks if user owns the resource.
     */
    private boolean checkOwnership(User user, Object resource, String permission) {
        // This would be implemented based on your domain model
        // For example, checking if user created the testcase, issue, etc.
        
        if (resource instanceof com.google.clusterfuzz.core.entity.Testcase) {
            // Check if user can access this testcase
            return checkTestcaseAccess(user, (com.google.clusterfuzz.core.entity.Testcase) resource, permission);
        }
        
        // Add more resource type checks as needed
        return false;
    }

    /**
     * Checks ownership by resource type and ID.
     */
    private boolean checkOwnershipById(User user, String resourceType, Serializable resourceId, String permission) {
        switch (resourceType) {
            case "testcase":
                return checkTestcaseAccessById(user, resourceId, permission);
            case "issue":
                return checkIssueAccessById(user, resourceId, permission);
            // Add more cases as needed
            default:
                return false;
        }
    }

    /**
     * Checks testcase access permissions.
     */
    private boolean checkTestcaseAccess(User user, com.google.clusterfuzz.core.entity.Testcase testcase, String permission) {
        // Example: Users can read/write their own testcases or public ones
        // This would depend on your business logic
        
        String action = extractAction(permission);
        
        switch (action) {
            case "read":
                // Users can read public testcases or their own
                return testcase.isPublic() || isTestcaseOwner(user, testcase);
            case "write":
            case "delete":
                // Users can modify only their own testcases (unless admin)
                return user.isAdmin() || isTestcaseOwner(user, testcase);
            default:
                return false;
        }
    }

    /**
     * Checks testcase access by ID.
     */
    private boolean checkTestcaseAccessById(User user, Serializable testcaseId, String permission) {
        // This would load the testcase and check access
        // For now, return false as a safe default
        return false;
    }

    /**
     * Checks issue access by ID.
     */
    private boolean checkIssueAccessById(User user, Serializable issueId, String permission) {
        // This would load the issue and check access
        // For now, return false as a safe default
        return false;
    }

    /**
     * Checks if user is the owner of a testcase.
     */
    private boolean isTestcaseOwner(User user, com.google.clusterfuzz.core.entity.Testcase testcase) {
        // This would check if the user created or owns the testcase
        // Implementation depends on your domain model
        return false; // Placeholder
    }

    /**
     * Extracts action from permission string.
     */
    private String extractAction(String permission) {
        if (permission.contains(":")) {
            String[] parts = permission.split(":");
            return parts[parts.length - 1];
        }
        return permission.toLowerCase();
    }
}