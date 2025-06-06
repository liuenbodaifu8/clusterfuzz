package com.google.clusterfuzz.core.service;

import com.google.clusterfuzz.core.entity.Permission;
import com.google.clusterfuzz.core.entity.Role;
import com.google.clusterfuzz.core.entity.User;
import com.google.clusterfuzz.core.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for managing roles and role hierarchy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

    /**
     * Creates a new role.
     */
    @CacheEvict(value = {"roles", "userRoles"}, allEntries = true)
    public Role createRole(Role role) {
        validateRole(role);
        
        Role savedRole = roleRepository.save(role);
        log.info("Created role: {}", savedRole.getName());
        
        return savedRole;
    }

    /**
     * Updates an existing role.
     */
    @CacheEvict(value = {"roles", "userRoles"}, allEntries = true)
    public Role updateRole(Role role) {
        validateRole(role);
        
        Role existingRole = roleRepository.findById(role.getId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + role.getId()));
        
        // Prevent modification of system roles
        if (existingRole.getSystemRole()) {
            throw new IllegalArgumentException("Cannot modify system role: " + existingRole.getName());
        }
        
        Role savedRole = roleRepository.save(role);
        log.info("Updated role: {}", savedRole.getName());
        
        return savedRole;
    }

    /**
     * Deletes a role.
     */
    @CacheEvict(value = {"roles", "userRoles"}, allEntries = true)
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
        
        // Prevent deletion of system roles
        if (role.getSystemRole()) {
            throw new IllegalArgumentException("Cannot delete system role: " + role.getName());
        }
        
        // Check if role is assigned to users
        if (!role.getUsers().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete role assigned to users: " + role.getName());
        }
        
        roleRepository.delete(role);
        log.info("Deleted role: {}", role.getName());
    }

    /**
     * Finds role by ID.
     */
    @Cacheable(value = "roles", key = "#roleId")
    @Transactional(readOnly = true)
    public Optional<Role> findById(Long roleId) {
        return roleRepository.findById(roleId);
    }

    /**
     * Finds role by name.
     */
    @Cacheable(value = "roles", key = "#name")
    @Transactional(readOnly = true)
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    /**
     * Gets all active roles.
     */
    @Cacheable(value = "roles", key = "'all-active'")
    @Transactional(readOnly = true)
    public List<Role> findAllActive() {
        return roleRepository.findByActiveTrue();
    }

    /**
     * Gets all roles.
     */
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    /**
     * Adds permission to role.
     */
    @CacheEvict(value = {"roles", "userRoles"}, allEntries = true)
    public void addPermissionToRole(String roleName, String permissionName) {
        Role role = findByName(roleName)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        
        Permission permission = permissionService.findByName(permissionName)
            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionName));
        
        role.addPermission(permission);
        roleRepository.save(role);
        
        log.info("Added permission {} to role {}", permissionName, roleName);
    }

    /**
     * Removes permission from role.
     */
    @CacheEvict(value = {"roles", "userRoles"}, allEntries = true)
    public void removePermissionFromRole(String roleName, String permissionName) {
        Role role = findByName(roleName)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        
        Permission permission = permissionService.findByName(permissionName)
            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionName));
        
        role.removePermission(permission);
        roleRepository.save(role);
        
        log.info("Removed permission {} from role {}", permissionName, roleName);
    }

    /**
     * Sets up role hierarchy.
     */
    @CacheEvict(value = {"roles", "userRoles"}, allEntries = true)
    public void addRoleHierarchy(String parentRoleName, String childRoleName) {
        Role parentRole = findByName(parentRoleName)
            .orElseThrow(() -> new IllegalArgumentException("Parent role not found: " + parentRoleName));
        
        Role childRole = findByName(childRoleName)
            .orElseThrow(() -> new IllegalArgumentException("Child role not found: " + childRoleName));
        
        // Prevent circular hierarchy
        if (childRole.inheritsFrom(parentRole) || parentRole.inheritsFrom(childRole)) {
            throw new IllegalArgumentException("Circular role hierarchy detected");
        }
        
        parentRole.addChildRole(childRole);
        roleRepository.save(parentRole);
        
        log.info("Added role hierarchy: {} -> {}", parentRoleName, childRoleName);
    }

    /**
     * Removes role hierarchy.
     */
    @CacheEvict(value = {"roles", "userRoles"}, allEntries = true)
    public void removeRoleHierarchy(String parentRoleName, String childRoleName) {
        Role parentRole = findByName(parentRoleName)
            .orElseThrow(() -> new IllegalArgumentException("Parent role not found: " + parentRoleName));
        
        Role childRole = findByName(childRoleName)
            .orElseThrow(() -> new IllegalArgumentException("Child role not found: " + childRoleName));
        
        parentRole.removeChildRole(childRole);
        roleRepository.save(parentRole);
        
        log.info("Removed role hierarchy: {} -> {}", parentRoleName, childRoleName);
    }

    /**
     * Gets default roles for new users.
     */
    @Cacheable(value = "roles", key = "'default'")
    @Transactional(readOnly = true)
    public Set<Role> getDefaultRoles() {
        Optional<Role> userRole = findByName("USER");
        if (userRole.isPresent()) {
            return Set.of(userRole.get());
        }
        
        log.warn("Default USER role not found");
        return Set.of();
    }

    /**
     * Initializes default roles and permissions.
     */
    @CacheEvict(value = {"roles", "userRoles"}, allEntries = true)
    public void initializeDefaultRoles() {
        log.info("Initializing default roles and permissions");
        
        // Create default permissions first
        permissionService.initializeDefaultPermissions();
        
        // Create ADMIN role
        createRoleIfNotExists("ADMIN", "Administrator", "Full system access", 100, true);
        
        // Create USER role
        createRoleIfNotExists("USER", "User", "Standard user access", 50, true);
        
        // Create BOT role
        createRoleIfNotExists("BOT", "Bot", "Automated system access", 25, true);
        
        // Create GUEST role
        createRoleIfNotExists("GUEST", "Guest", "Read-only access", 10, true);
        
        // Set up permissions for roles
        setupDefaultRolePermissions();
        
        log.info("Default roles and permissions initialized");
    }

    /**
     * Creates role if it doesn't exist.
     */
    private void createRoleIfNotExists(String name, String displayName, String description, 
                                     int hierarchyLevel, boolean systemRole) {
        if (findByName(name).isEmpty()) {
            Role role = new Role();
            role.setName(name);
            role.setDisplayName(displayName);
            role.setDescription(description);
            role.setHierarchyLevel(hierarchyLevel);
            role.setSystemRole(systemRole);
            role.setActive(true);
            
            roleRepository.save(role);
            log.info("Created default role: {}", name);
        }
    }

    /**
     * Sets up default permissions for roles.
     */
    private void setupDefaultRolePermissions() {
        // ADMIN role gets all permissions
        Role adminRole = findByName("ADMIN").orElse(null);
        if (adminRole != null) {
            List<Permission> allPermissions = permissionService.findAll();
            for (Permission permission : allPermissions) {
                adminRole.addPermission(permission);
            }
            roleRepository.save(adminRole);
        }
        
        // USER role gets basic permissions
        Role userRole = findByName("USER").orElse(null);
        if (userRole != null) {
            addPermissionToRoleIfExists(userRole, Permission.StandardPermissions.TESTCASES_READ);
            addPermissionToRoleIfExists(userRole, Permission.StandardPermissions.TESTCASES_WRITE);
            addPermissionToRoleIfExists(userRole, Permission.StandardPermissions.ISSUES_READ);
            addPermissionToRoleIfExists(userRole, Permission.StandardPermissions.ISSUES_WRITE);
            addPermissionToRoleIfExists(userRole, Permission.StandardPermissions.ISSUES_COMMENT);
            addPermissionToRoleIfExists(userRole, Permission.StandardPermissions.FUZZING_READ);
            addPermissionToRoleIfExists(userRole, Permission.StandardPermissions.COVERAGE_READ);
            addPermissionToRoleIfExists(userRole, Permission.StandardPermissions.STATS_READ);
            roleRepository.save(userRole);
        }
        
        // BOT role gets execution permissions
        Role botRole = findByName("BOT").orElse(null);
        if (botRole != null) {
            addPermissionToRoleIfExists(botRole, Permission.StandardPermissions.BOTS_EXECUTE);
            addPermissionToRoleIfExists(botRole, Permission.StandardPermissions.FUZZING_EXECUTE);
            addPermissionToRoleIfExists(botRole, Permission.StandardPermissions.TESTCASES_READ);
            addPermissionToRoleIfExists(botRole, Permission.StandardPermissions.ISSUES_READ);
            roleRepository.save(botRole);
        }
        
        // GUEST role gets read-only permissions
        Role guestRole = findByName("GUEST").orElse(null);
        if (guestRole != null) {
            addPermissionToRoleIfExists(guestRole, Permission.StandardPermissions.TESTCASES_READ);
            addPermissionToRoleIfExists(guestRole, Permission.StandardPermissions.ISSUES_READ);
            addPermissionToRoleIfExists(guestRole, Permission.StandardPermissions.COVERAGE_READ);
            addPermissionToRoleIfExists(guestRole, Permission.StandardPermissions.STATS_READ);
            roleRepository.save(guestRole);
        }
    }

    /**
     * Adds permission to role if permission exists.
     */
    private void addPermissionToRoleIfExists(Role role, String permissionName) {
        permissionService.findByName(permissionName).ifPresent(role::addPermission);
    }

    /**
     * Validates role data.
     */
    private void validateRole(Role role) {
        if (role.getName() == null || role.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Role name is required");
        }
        
        if (role.getHierarchyLevel() == null || role.getHierarchyLevel() < 0) {
            throw new IllegalArgumentException("Valid hierarchy level is required");
        }
        
        // Check for duplicate name (excluding current role)
        Optional<Role> existingRole = findByName(role.getName());
        if (existingRole.isPresent() && !existingRole.get().getId().equals(role.getId())) {
            throw new IllegalArgumentException("Role name already exists: " + role.getName());
        }
    }
}