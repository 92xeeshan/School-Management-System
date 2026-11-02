package com.schoolms.website;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicSiteCopyRepository extends JpaRepository<PublicSiteCopy, UUID> {

    List<PublicSiteCopy> findBySchoolIdOrderByLocaleAsc(UUID schoolId);

    Optional<PublicSiteCopy> findBySchoolIdAndLocale(UUID schoolId, String locale);
}
