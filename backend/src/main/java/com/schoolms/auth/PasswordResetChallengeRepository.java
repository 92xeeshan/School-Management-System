package com.schoolms.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetChallengeRepository extends JpaRepository<PasswordResetChallenge, UUID> {

    Optional<PasswordResetChallenge> findByTokenHash(String tokenHash);

    Optional<PasswordResetChallenge> findByIdAndConsumedAtIsNull(UUID id);
}
