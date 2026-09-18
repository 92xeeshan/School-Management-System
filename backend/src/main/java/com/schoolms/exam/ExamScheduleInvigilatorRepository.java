package com.schoolms.exam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ExamScheduleInvigilatorRepository extends JpaRepository<ExamScheduleInvigilator, UUID> {

    List<ExamScheduleInvigilator> findByExamScheduleId(UUID examScheduleId);

    List<ExamScheduleInvigilator> findByExamScheduleIdIn(Collection<UUID> examScheduleIds);

    void deleteByExamScheduleId(UUID examScheduleId);
}
