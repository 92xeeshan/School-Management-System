package com.schoolms.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserSecurityAnswerRepository extends JpaRepository<UserSecurityAnswer, UUID> {

    @Query("select a from UserSecurityAnswer a join fetch a.question where a.userId = :userId")
    List<UserSecurityAnswer> findByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from UserSecurityAnswer a where a.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
