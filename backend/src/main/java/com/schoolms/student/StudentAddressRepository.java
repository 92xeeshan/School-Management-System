package com.schoolms.student;

import com.schoolms.common.enums.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentAddressRepository extends JpaRepository<StudentAddress, UUID> {

    List<StudentAddress> findByStudentIdAndSchoolId(UUID studentId, UUID schoolId);

    Optional<StudentAddress> findByStudentIdAndAddressType(UUID studentId, AddressType addressType);
}
