package com.schoolms.user;

import com.schoolms.auth.AuthUserView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByIdAndSchoolId(UUID id, UUID schoolId);

    List<User> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    boolean existsByUsernameAndSchoolId(String username, UUID schoolId);

    boolean existsByEmailAndSchoolId(String email, UUID schoolId);

    @Query(value = """
            SELECT id, school_id AS schoolId, username, password_hash AS passwordHash,
                   status, locale, first_name AS firstName, last_name AS lastName, email
            FROM auth_find_user(:login)
            """, nativeQuery = true)
    Optional<AuthUserView> findAuthUser(@Param("login") String login);

    @Query(value = """
            SELECT r.code FROM role r
            JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = :userId
            """, nativeQuery = true)
    List<String> findRoleCodesByUserId(@Param("userId") UUID userId);

    @Query(value = """
            SELECT DISTINCT p.code FROM permission p
            JOIN role_permission rp ON rp.permission_id = p.id
            JOIN user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = :userId
            """, nativeQuery = true)
    List<String> findPermissionCodesByUserId(@Param("userId") UUID userId);
}
