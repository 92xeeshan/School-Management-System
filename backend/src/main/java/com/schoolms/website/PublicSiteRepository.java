package com.schoolms.website;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PublicSiteRepository extends JpaRepository<PublicSite, UUID> {

    Optional<PublicSite> findBySchoolId(UUID schoolId);
}
