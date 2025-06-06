package com.google.clusterfuzz.core.repository;

import com.google.clusterfuzz.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds user by OAuth2 provider and provider ID.
     */
    Optional<User> findByOauth2ProviderAndOauth2ProviderId(String provider, String providerId);

    /**
     * Finds user by Firebase UID.
     */
    Optional<User> findByFirebaseUid(String firebaseUid);

    /**
     * Finds all active users.
     */
    List<User> findByActiveTrue();

    /**
     * Finds all inactive users.
     */
    List<User> findByActiveFalse();

    /**
     * Finds users by email verification status.
     */
    List<User> findByEmailVerified(Boolean emailVerified);

    /**
     * Finds users by OAuth2 provider.
     */
    List<User> findByOauth2Provider(String provider);

    /**
     * Finds users who haven't logged in since a specific date.
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :cutoffDate OR u.lastLoginAt IS NULL")
    List<User> findInactiveUsersSince(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Finds users with a specific role.
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    /**
     * Finds users with any of the specified roles.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name IN :roleNames")
    List<User> findByRoleNames(@Param("roleNames") List<String> roleNames);

    /**
     * Finds users with a specific permission (through roles).
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r JOIN r.permissions p WHERE p.name = :permissionName")
    List<User> findByPermissionName(@Param("permissionName") String permissionName);

    /**
     * Finds users created after a specific date.
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Finds users who logged in after a specific date.
     */
    List<User> findByLastLoginAtAfter(LocalDateTime date);

    /**
     * Checks if user exists by email.
     */
    boolean existsByEmail(String email);

    /**
     * Checks if user exists by Firebase UID.
     */
    boolean existsByFirebaseUid(String firebaseUid);

    /**
     * Counts active users.
     */
    long countByActiveTrue();

    /**
     * Counts users by role.
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    /**
     * Finds users ordered by last login date.
     */
    List<User> findAllByOrderByLastLoginAtDesc();

    /**
     * Finds recently created users.
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= :since ORDER BY u.createdAt DESC")
    List<User> findRecentlyCreatedUsers(@Param("since") LocalDateTime since);
}