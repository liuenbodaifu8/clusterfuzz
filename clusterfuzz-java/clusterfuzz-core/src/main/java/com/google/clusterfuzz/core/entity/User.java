package com.google.clusterfuzz.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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
 * User entity for authentication and authorization.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_active", columnList = "active"),
    @Index(name = "idx_user_oauth2_provider", columnList = "oauth2_provider"),
    @Index(name = "idx_user_firebase_uid", columnList = "firebase_uid"),
    @Index(name = "idx_user_last_login", columnList = "last_login_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(exclude = {"roles"})
@ToString(exclude = {"roles"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Column(name = "name", length = 100)
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Column(name = "picture_url", length = 500)
    @Size(max = 500, message = "Picture URL must not exceed 500 characters")
    private String pictureUrl;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "oauth2_provider", length = 50)
    @Size(max = 50, message = "OAuth2 provider must not exceed 50 characters")
    private String oauth2Provider;

    @Column(name = "oauth2_provider_id", length = 100)
    @Size(max = 100, message = "OAuth2 provider ID must not exceed 100 characters")
    private String oauth2ProviderId;

    @Column(name = "firebase_uid", length = 128)
    @Size(max = 128, message = "Firebase UID must not exceed 128 characters")
    private String firebaseUid;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_logout_at")
    private LocalDateTime lastLogoutAt;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    // Many-to-many relationship with Roles
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Adds a role to this user.
     */
    public void addRole(Role role) {
        roles.add(role);
        role.getUsers().add(this);
    }

    /**
     * Removes a role from this user.
     */
    public void removeRole(Role role) {
        roles.remove(role);
        role.getUsers().remove(this);
    }

    /**
     * Gets all permissions from all roles.
     */
    public Set<Permission> getAllPermissions() {
        Set<Permission> allPermissions = new HashSet<>();
        
        for (Role role : roles) {
            allPermissions.addAll(role.getAllPermissions());
        }
        
        return allPermissions;
    }

    /**
     * Checks if user has a specific permission.
     */
    public boolean hasPermission(String permissionName) {
        return getAllPermissions().stream()
            .anyMatch(permission -> permission.getName().equals(permissionName));
    }

    /**
     * Checks if user has a specific role.
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
            .anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * Checks if user has any of the specified roles.
     */
    public boolean hasAnyRole(String... roleNames) {
        for (String roleName : roleNames) {
            if (hasRole(roleName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the highest hierarchy level role.
     */
    public Role getHighestRole() {
        return roles.stream()
            .max((r1, r2) -> Integer.compare(r1.getHierarchyLevel(), r2.getHierarchyLevel()))
            .orElse(null);
    }

    /**
     * Checks if user is an admin.
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Checks if user is a bot.
     */
    public boolean isBot() {
        return hasRole("BOT");
    }

    /**
     * Gets user's display name.
     */
    public String getDisplayName() {
        return name != null && !name.trim().isEmpty() ? name : email;
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
        if (emailVerified == null) {
            emailVerified = false;
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