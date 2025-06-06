package com.google.clusterfuzz.core.repository;

import com.google.clusterfuzz.core.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Permission entity.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Finds permission by name.
     */
    Optional<Permission> findByName(String name);

    /**
     * Finds permissions by resource.
     */
    List<Permission> findByResource(String resource);

    /**
     * Finds permissions by action.
     */
    List<Permission> findByAction(String action);

    /**
     * Finds permissions by resource and action.
     */
    List<Permission> findByResourceAndAction(String resource, String action);

    /**
     * Finds all active permissions.
     */
    List<Permission> findByActiveTrue();

    /**
     * Finds system permissions.
     */
    List<Permission> findBySystemPermissionTrue();

    /**
     * Finds non-system permissions.
     */
    List<Permission> findBySystemPermissionFalse();

    /**
     * Finds permissions assigned to a specific role.
     */
    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.name = :roleName")
    List<Permission> findByRoleName(@Param("roleName") String roleName);

    /**
     * Finds permissions for a specific user (through roles).
     */
    @Query("SELECT DISTINCT p FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.email = :userEmail")
    List<Permission> findByUserEmail(@Param("userEmail") String userEmail);

    /**
     * Finds permissions by resource pattern (using LIKE).
     */
    @Query("SELECT p FROM Permission p WHERE p.resource LIKE :resourcePattern")
    List<Permission> findByResourcePattern(@Param("resourcePattern") String resourcePattern);

    /**
     * Checks if permission exists by name.
     */
    boolean existsByName(String name);

    /**
     * Checks if permission exists by resource and action.
     */
    boolean existsByResourceAndAction(String resource, String action);

    /**
     * Counts active permissions.
     */
    long countByActiveTrue();

    /**
     * Finds permissions ordered by resource and action.
     */
    List<Permission> findAllByOrderByResourceAscActionAsc();
}