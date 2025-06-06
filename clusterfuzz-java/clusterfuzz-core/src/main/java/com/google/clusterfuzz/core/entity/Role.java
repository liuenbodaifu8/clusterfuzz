package com.google.clusterfuzz.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Role entity for role-based access control (RBAC).
 */
@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_role_name", columnList = "name"),
    @Index(name = "idx_role_active", columnList = "active"),
    @Index(name = "idx_role_hierarchy", columnList = "hierarchy_level")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(exclude = {"users", "permissions", "parentRoles", "childRoles"})
@ToString(exclude = {"users", "permissions", "parentRoles", "childRoles"})
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name must not exceed 50 characters")
    private String name;

    @Column(name = "display_name", length = 100)
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Column(name = "hierarchy_level", nullable = false)
    private Integer hierarchyLevel = 0;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "system_role", nullable = false)
    private Boolean systemRole = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Many-to-many relationship with Users
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    // Many-to-many relationship with Permissions
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    // Self-referencing many-to-many for role hierarchy
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_hierarchy",
        joinColumns = @JoinColumn(name = "parent_role_id"),
        inverseJoinColumns = @JoinColumn(name = "child_role_id")
    )
    private Set<Role> childRoles = new HashSet<>();

    @ManyToMany(mappedBy = "childRoles", fetch = FetchType.LAZY)
    private Set<Role> parentRoles = new HashSet<>();

    /**
     * Adds a permission to this role.
     */
    public void addPermission(Permission permission) {
        permissions.add(permission);
        permission.getRoles().add(this);
    }

    /**
     * Removes a permission from this role.
     */
    public void removePermission(Permission permission) {
        permissions.remove(permission);
        permission.getRoles().remove(this);
    }

    /**
     * Adds a child role to this role.
     */
    public void addChildRole(Role childRole) {
        childRoles.add(childRole);
        childRole.getParentRoles().add(this);
    }

    /**
     * Removes a child role from this role.
     */
    public void removeChildRole(Role childRole) {
        childRoles.remove(childRole);
        childRole.getParentRoles().remove(this);
    }

    /**
     * Gets all permissions including inherited permissions from child roles.
     */
    public Set<Permission> getAllPermissions() {
        Set<Permission> allPermissions = new HashSet<>(permissions);
        
        // Add permissions from child roles recursively
        for (Role childRole : childRoles) {
            allPermissions.addAll(childRole.getAllPermissions());
        }
        
        return allPermissions;
    }

    /**
     * Checks if this role has a specific permission (including inherited).
     */
    public boolean hasPermission(String permissionName) {
        return getAllPermissions().stream()
            .anyMatch(permission -> permission.getName().equals(permissionName));
    }

    /**
     * Checks if this role is higher in hierarchy than another role.
     */
    public boolean isHigherThan(Role otherRole) {
        return this.hierarchyLevel > otherRole.getHierarchyLevel();
    }

    /**
     * Checks if this role inherits from another role.
     */
    public boolean inheritsFrom(Role parentRole) {
        return parentRoles.contains(parentRole) || 
               parentRoles.stream().anyMatch(role -> role.inheritsFrom(parentRole));
    }

    /**
     * Pre-persist callback to set default values.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (systemRole == null) {
            systemRole = false;
        }
        if (hierarchyLevel == null) {
            hierarchyLevel = 0;
        }
    }

    /**
     * Pre-update callback to update timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}