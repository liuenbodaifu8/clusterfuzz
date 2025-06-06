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
 * Permission entity for fine-grained access control.
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permission_name", columnList = "name"),
    @Index(name = "idx_permission_resource", columnList = "resource"),
    @Index(name = "idx_permission_action", columnList = "action"),
    @Index(name = "idx_permission_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(exclude = {"roles"})
@ToString(exclude = {"roles"})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Permission name is required")
    @Size(max = 100, message = "Permission name must not exceed 100 characters")
    private String name;

    @Column(name = "display_name", length = 100)
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Column(name = "resource", nullable = false, length = 50)
    @NotBlank(message = "Resource is required")
    @Size(max = 50, message = "Resource must not exceed 50 characters")
    private String resource;

    @Column(name = "action", nullable = false, length = 50)
    @NotBlank(message = "Action is required")
    @Size(max = 50, message = "Action must not exceed 50 characters")
    private String action;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "system_permission", nullable = false)
    private Boolean systemPermission = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Many-to-many relationship with Roles
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<Role> roles = new HashSet<>();

    /**
     * Creates a permission name from resource and action.
     */
    public static String createPermissionName(String resource, String action) {
        return resource.toLowerCase() + ":" + action.toLowerCase();
    }

    /**
     * Checks if this permission matches a resource and action.
     */
    public boolean matches(String resource, String action) {
        return this.resource.equalsIgnoreCase(resource) && 
               this.action.equalsIgnoreCase(action);
    }

    /**
     * Checks if this permission allows access to a resource with any action.
     */
    public boolean allowsResourceAccess(String resource) {
        return this.resource.equalsIgnoreCase(resource) || 
               this.action.equals("*") || 
               this.resource.equals("*");
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
        if (systemPermission == null) {
            systemPermission = false;
        }
        
        // Auto-generate name if not set
        if (name == null && resource != null && action != null) {
            name = createPermissionName(resource, action);
        }
    }

    /**
     * Pre-update callback to update timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Standard ClusterFuzz permissions.
     */
    public static class StandardPermissions {
        // Admin permissions
        public static final String ADMIN_CONFIG_READ = "admin:config:read";
        public static final String ADMIN_CONFIG_WRITE = "admin:config:write";
        public static final String ADMIN_USERS_MANAGE = "admin:users:manage";
        public static final String ADMIN_SYSTEM_MANAGE = "admin:system:manage";
        
        // Testcase permissions
        public static final String TESTCASES_READ = "testcases:read";
        public static final String TESTCASES_WRITE = "testcases:write";
        public static final String TESTCASES_DELETE = "testcases:delete";
        public static final String TESTCASES_MANAGE = "testcases:manage";
        
        // Issue permissions
        public static final String ISSUES_READ = "issues:read";
        public static final String ISSUES_WRITE = "issues:write";
        public static final String ISSUES_COMMENT = "issues:comment";
        public static final String ISSUES_MANAGE = "issues:manage";
        
        // Bot permissions
        public static final String BOTS_READ = "bots:read";
        public static final String BOTS_MANAGE = "bots:manage";
        public static final String BOTS_EXECUTE = "bots:execute";
        
        // Fuzzing permissions
        public static final String FUZZING_READ = "fuzzing:read";
        public static final String FUZZING_EXECUTE = "fuzzing:execute";
        public static final String FUZZING_MANAGE = "fuzzing:manage";
        
        // Coverage permissions
        public static final String COVERAGE_READ = "coverage:read";
        public static final String COVERAGE_MANAGE = "coverage:manage";
        
        // Statistics permissions
        public static final String STATS_READ = "stats:read";
        public static final String STATS_MANAGE = "stats:manage";
    }
}