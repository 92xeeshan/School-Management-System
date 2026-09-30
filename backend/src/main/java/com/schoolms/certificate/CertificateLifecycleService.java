package com.schoolms.certificate;

import com.schoolms.auth.RefreshTokenRepository;
import com.schoolms.common.enums.StudentStatus;
import com.schoolms.common.enums.UserStatus;
import com.schoolms.notification.NotificationTriggerService;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import com.schoolms.tenant.TenantContext;
import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateLifecycleService {

    static final int TC_DEACTIVATION_DAYS = 15;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final CertificateIssuedRepository issuedRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationTriggerService notifications;

    public void onIssued(CertificateIssued issued, Student student) {
        if (issued.getCertificateType() != CertificateType.TC) {
            return;
        }
        issued.setTcIssuedAt(Instant.now());
        issued.setDeactivationScheduledAt(LocalDate.now().plusDays(TC_DEACTIVATION_DAYS));
        notifications.onTransferCertificateIssued(
                issued.getSchoolId(),
                student.getId(),
                issued.getDeactivationScheduledAt().format(DATE));
    }

    @Transactional
    public int deactivateDueAccounts() {
        TenantContext.setBypassRls(true);
        int count = 0;
        try {
            List<CertificateIssued> due = issuedRepository.findDueForDeactivation(LocalDate.now());
            for (CertificateIssued issued : due) {
                TenantContext.setSchoolId(issued.getSchoolId());
                if (deactivate(issued)) {
                    count++;
                }
            }
            return count;
        } finally {
            TenantContext.clear();
        }
    }

    private boolean deactivate(CertificateIssued issued) {
        Student student = studentRepository.findByIdAndSchoolId(issued.getStudentId(), issued.getSchoolId()).orElse(null);
        if (student == null) {
            return false;
        }
        student.setStatus(StudentStatus.INACTIVE);
        studentRepository.save(student);
        if (student.getUserId() != null) {
            userRepository.findById(student.getUserId()).ifPresent(user -> {
                user.setStatus(UserStatus.INACTIVE);
                userRepository.save(user);
                refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());
            });
        }
        issued.setDeactivatedAt(Instant.now());
        issuedRepository.save(issued);
        log.info("Deactivated student {} after TC {}", student.getId(), issued.getCertificateNo());
        return true;
    }

    public boolean deactivatedForTc(UUID schoolId, UUID userId) {
        if (schoolId == null || userId == null) {
            return false;
        }
        Student student = studentRepository.findBySchoolIdAndUserId(schoolId, userId).orElse(null);
        if (student == null) {
            return false;
        }
        return issuedRepository.findBySchoolIdAndStudentIdOrderByCreatedAtDesc(schoolId, student.getId()).stream()
                .anyMatch(row -> row.getCertificateType() == CertificateType.TC && row.getDeactivatedAt() != null);
    }
}
