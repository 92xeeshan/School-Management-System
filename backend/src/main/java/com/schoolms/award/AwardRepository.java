package com.schoolms.award;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AwardRepository extends JpaRepository<Award, UUID> {

    long countBySchoolId(UUID schoolId);

    List<Award> findBySchoolIdOrderByAwardedDateDesc(UUID schoolId);

    List<Award> findBySchoolIdAndStudentIdOrderByAwardedDateDesc(UUID schoolId, UUID studentId);

    List<Award> findTop10BySchoolIdOrderByPointsDescAwardedDateDesc(UUID schoolId);
}
