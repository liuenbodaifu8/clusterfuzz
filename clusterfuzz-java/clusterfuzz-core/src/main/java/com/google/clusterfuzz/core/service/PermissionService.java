package com.google.clusterfuzz.core.service;

import com.google.clusterfuzz.core.entity.Permission;
import com.google.clusterfuzz.core.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing permissions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PermissionService {

    private final PermissionRepository permissionRepository;

    /**
     * Creates a new permission.
     */
    @CacheEvict(value = {"permissions", "userRoles"}, allEntries = true)
    public Permission createPermission(Permission permission) {
        validatePermission(permission);
        
        Permission savedPermission = permissionRepository.save(permission);
        log.info("Created permission: {}", savedPermission.getName());
        
        return savedPermission;
    }

    /**
     * Updates an existing permission.
     */
    @CacheEvict(value = {"permissions", "userRoles"}, allEntries = true)
    public Permission updatePermission(Permission permission) {
        validatePermission(permission);
        
        Permission existingPermission = permissionRepository.findById(permission.getId())
            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permission.getId()));
        
        // Prevent modification of system permissions
        if (existingPermission.getSystemPermission()) {
            throw new IllegalArgumentException("Cannot modify system permission: " + existingPermission.getName());
        }
        
        Permission savedPermission = permissionRepository.save(permission);
        log.info("Updated permission: {}", savedPermission.getName());
        
        return savedPermission;
    }

    /**
     * Deletes a permission.
     */
    @CacheEvict(value = {"permissions", "userRoles"}, allEntries = true)
    public void deletePermission(Long permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionId));
        
        // Prevent deletion of system permissions
        if (permission.getSystemPermission()) {
            throw new IllegalArgumentException("Cannot delete system permission: " + permission.getName());
        }
        
        // Check if permission is assigned to roles
        if (!permission.getRoles().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete permission assigned to roles: " + permission.getName());
        }
        
        permissionRepository.delete(permission);
        log.info("Deleted permission: {}", permission.getName());
    }

    /**
     * Finds permission by ID.
     */
    @Cacheable(value = "permissions", key = "#permissionId")
    @Transactional(readOnly = true)
    public Optional<Permission> findById(Long permissionId) {
        return permissionRepository.findById(permissionId);
    }

    /**
     * Finds permission by name.
     */
    @Cacheable(value = "permissions", key = "#name")
    @Transactional(readOnly = true)
    public Optional<Permission> findByName(String name) {
        return permissionRepository.findByName(name);
    }

    /**
     * Finds permissions by resource.
     */
    @Cacheable(value = "permissions", key = "'resource:' + #resource")
    @Transactional(readOnly = true)
    public List<Permission> findByResource(String resource) {
        return permissionRepository.findByResource(resource);
    }

    /**
     * Finds permissions by resource and action.
     */
    @Cacheable(value = "permissions", key = "'resource-action:' + #resource + ':' + #action")
    @Transactional(readOnly = true)
    public List<Permission> findByResourceAndAction(String resource, String action) {
        return permissionRepository.findByResourceAndAction(resource, action);
    }

    /**
     * Gets all active permissions.
     */
    @Cacheable(value = "permissions", key = "'all-active'")
    @Transactional(readOnly = true)
    public List<Permission> findAllActive() {
        return permissionRepository.findByActiveTrue();
    }

    /**
     * Gets all permissions.
     */
    @Transactional(readOnly = true)
    public List<Permission> findAll() {
        return permissionRepository.findAll();
    }

    /**
     * Creates permission from resource and action.
     */
    @CacheEvict(value = {"permissions", "userRoles"}, allEntries = true)
    public Permission createPermission(String resource, String action, String description) {
        String permissionName = Permission.createPermissionName(resource, action);
        
        // Check if permission already exists
        Optional<Permission> existing = findByName(permissionName);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        Permission permission = new Permission();
        permission.setName(permissionName);
        permission.setDisplayName(formatDisplayName(resource, action));
        permission.setDescription(description);
        permission.setResource(resource);
        permission.setAction(action);
        permission.setActive(true);
        permission.setSystemPermission(false);
        
        return createPermission(permission);
    }

    /**
     * Initializes default permissions.
     */
    @CacheEvict(value = {"permissions", "userRoles"}, allEntries = true)
    public void initializeDefaultPermissions() {
        log.info("Initializing default permissions");
        
        // Admin permissions
        createSystemPermissionIfNotExists("admin", "config:read", "Read configuration settings");
        createSystemPermissionIfNotExists("admin", "config:write", "Write configuration settings");
        createSystemPermissionIfNotExists("admin", "users:manage", "Manage users and roles");
        createSystemPermissionIfNotExists("admin", "system:manage", "Manage system settings");
        
        // Testcase permissions
        createSystemPermissionIfNotExists("testcases", "read", "Read testcases");
        createSystemPermissionIfNotExists("testcases", "write", "Create and update testcases");
        createSystemPermissionIfNotExists("testcases", "delete", "Delete testcases");
        createSystemPermissionIfNotExists("testcases", "manage", "Manage testcase settings");
        
        // Issue permissions
        createSystemPermissionIfNotExists("issues", "read", "Read issues");
        createSystemPermissionIfNotExists("issues", "write", "Create and update issues");
        createSystemPermissionIfNotExists("issues", "comment", "Comment on issues");
        createSystemPermissionIfNotExists("issues", "manage", "Manage issue settings");
        
        // Bot permissions
        createSystemPermissionIfNotExists("bots", "read", "Read bot information");
        createSystemPermissionIfNotExists("bots", "manage", "Manage bot configuration");
        createSystemPermissionIfNotExists("bots", "execute", "Execute bot tasks");
        
        // Fuzzing permissions
        createSystemPermissionIfNotExists("fuzzing", "read", "Read fuzzing information");
        createSystemPermissionIfNotExists("fuzzing", "execute", "Execute fuzzing tasks");
        createSystemPermissionIfNotExists("fuzzing", "manage", "Manage fuzzing configuration");
        
        // Coverage permissions
        createSystemPermissionIfNotExists("coverage", "read", "Read coverage information");
        createSystemPermissionIfNotExists("coverage", "manage", "Manage coverage settings");
        
        // Statistics permissions
        createSystemPermissionIfNotExists("stats", "read", "Read statistics");
        createSystemPermissionIfNotExists("stats", "manage", "Manage statistics settings");
        
        log.info("Default permissions initialized");
    }

    /**
     * Creates system permission if it doesn't exist.
     */
    private void createSystemPermissionIfNotExists(String resource, String action, String description) {
        String permissionName = Permission.createPermissionName(resource, action);
        
        if (findByName(permissionName).isEmpty()) {
            Permission permission = new Permission();
            permission.setName(permissionName);
            permission.setDisplayName(formatDisplayName(resource, action));
            permission.setDescription(description);
            permission.setResource(resource);
            permission.setAction(action);
            permission.setActive(true);
            permission.setSystemPermission(true);
            
            permissionRepository.save(permission);
            log.debug("Created system permission: {}", permissionName);
        }
    }

    /**
     * Formats display name from resource and action.
     */
    private String formatDisplayName(String resource, String action) {
        return capitalize(action) + " " + capitalize(resource);
    }

    /**
     * Capitalizes first letter of string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Validates permission data.
     */
    private void validatePermission(Permission permission) {
        if (permission.getName() == null || permission.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Permission name is required");
        }
        
        if (permission.getResource() == null || permission.getResource().trim().isEmpty()) {
            throw new IllegalArgumentException("Permission resource is required");
        }
        
        if (permission.getAction() == null || permission.getAction().trim().isEmpty()) {
            throw new IllegalArgumentException("Permission action is required");
        }
        
        // Check for duplicate name (excluding current permission)
        Optional<Permission> existingPermission = findByName(permission.getName());
        if (existingPermission.isPresent() && !existingPermission.get().getId().equals(permission.getId())) {
            throw new IllegalArgumentException("Permission name already exists: " + permission.getName());
        }
    }
}