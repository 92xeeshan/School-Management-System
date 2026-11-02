package com.schoolms.website;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EnquiryRepository extends JpaRepository<Enquiry, UUID> {

    Page<Enquiry> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);

    Page<Enquiry> findBySchoolIdAndStatusOrderByCreatedAtDesc(UUID schoolId, EnquiryStatus status, Pageable pageable);

    Optional<Enquiry> findByIdAndSchoolId(UUID id, UUID schoolId);

    long countBySchoolIdAndStatus(UUID schoolId, EnquiryStatus status);
}
