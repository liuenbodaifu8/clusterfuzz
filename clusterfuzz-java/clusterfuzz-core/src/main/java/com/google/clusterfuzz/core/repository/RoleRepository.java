package com.google.clusterfuzz.core.repository;

import com.google.clusterfuzz.core.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Role entity.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Finds role by name.
     */
    Optional<Role> findByName(String name);

    /**
     * Finds all active roles.
     */
    List<Role> findByActiveTrue();

    /**
     * Finds roles by hierarchy level.
     */
    List<Role> findByHierarchyLevel(Integer hierarchyLevel);

    /**
     * Finds roles with hierarchy level greater than or equal to specified level.
     */
    List<Role> findByHierarchyLevelGreaterThanEqual(Integer hierarchyLevel);

    /**
     * Finds system roles.
     */
    List<Role> findBySystemRoleTrue();

    /**
     * Finds non-system roles.
     */
    List<Role> findBySystemRoleFalse();

    /**
     * Finds roles that have a specific permission.
     */
    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.name = :permissionName")
    List<Role> findByPermissionName(@Param("permissionName") String permissionName);

    /**
     * Finds roles assigned to a specific user.
     */
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.email = :userEmail")
    List<Role> findByUserEmail(@Param("userEmail") String userEmail);

    /**
     * Finds parent roles of a specific role.
     */
    @Query("SELECT r FROM Role r JOIN r.childRoles c WHERE c.id = :roleId")
    List<Role> findParentRoles(@Param("roleId") Long roleId);

    /**
     * Finds child roles of a specific role.
     */
    @Query("SELECT r FROM Role r JOIN r.parentRoles p WHERE p.id = :roleId")
    List<Role> findChildRoles(@Param("roleId") Long roleId);

    /**
     * Checks if role exists by name.
     */
    boolean existsByName(String name);

    /**
     * Counts active roles.
     */
    long countByActiveTrue();

    /**
     * Finds roles ordered by hierarchy level descending.
     */
    List<Role> findAllByOrderByHierarchyLevelDesc();
}