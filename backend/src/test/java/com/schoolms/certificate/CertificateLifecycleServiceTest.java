package com.schoolms.certificate;

import com.schoolms.TestSecurity;
import com.schoolms.auth.RefreshTokenRepository;
import com.schoolms.common.enums.StudentStatus;
import com.schoolms.common.enums.UserStatus;
import com.schoolms.notification.NotificationTriggerService;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateLifecycleServiceTest {

    private static final UUID STUDENT_A = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID USER_A = UUID.fromString("20000000-0000-0000-0000-000000000005");

    @Mock private CertificateIssuedRepository issuedRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private NotificationTriggerService notifications;

    @InjectMocks
    private CertificateLifecycleService service;

    @Test
    void onIssuedSchedulesTransferDeactivation() {
        CertificateIssued issued = tcIssued();
        Student student = student();

        service.onIssued(issued, student);

        assertEquals(LocalDate.now().plusDays(15), issued.getDeactivationScheduledAt());
        verify(notifications).onTransferCertificateIssued(eq(TestSecurity.SCHOOL_ID), eq(STUDENT_A), any());
    }

    @Test
    void deactivateDueAccountsInactivatesStudentAndUser() {
        CertificateIssued issued = tcIssued();
        issued.setDeactivationScheduledAt(LocalDate.now().minusDays(1));
        Student student = student();
        User user = new User();
        user.setId(USER_A);
        user.setStatus(UserStatus.ACTIVE);
        when(issuedRepository.findDueForDeactivation(LocalDate.now())).thenReturn(List.of(issued));
        when(studentRepository.findByIdAndSchoolId(STUDENT_A, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(student));
        when(userRepository.findById(USER_A)).thenReturn(Optional.of(user));

        int count = service.deactivateDueAccounts();

        assertEquals(1, count);
        assertEquals(StudentStatus.INACTIVE, student.getStatus());
        assertEquals(UserStatus.INACTIVE, user.getStatus());
        assertTrue(issued.getDeactivatedAt() != null);
        verify(refreshTokenRepository).revokeAllByUserId(eq(USER_A), any());
    }

    @Test
    void deactivatedForTcLooksAtIssuedTransfer() {
        CertificateIssued issued = tcIssued();
        issued.setDeactivatedAt(java.time.Instant.now());
        when(studentRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, USER_A)).thenReturn(Optional.of(student()));
        when(issuedRepository.findBySchoolIdAndStudentIdOrderByCreatedAtDesc(TestSecurity.SCHOOL_ID, STUDENT_A))
                .thenReturn(List.of(issued));

        assertTrue(service.deactivatedForTc(TestSecurity.SCHOOL_ID, USER_A));
        assertFalse(service.deactivatedForTc(null, USER_A));
    }

    private CertificateIssued tcIssued() {
        CertificateIssued issued = new CertificateIssued();
        issued.setId(UUID.fromString("80000000-0000-0000-0000-000000000009"));
        issued.setSchoolId(TestSecurity.SCHOOL_ID);
        issued.setStudentId(STUDENT_A);
        issued.setCertificateType(CertificateType.TC);
        issued.setCertificateNo("DEMO-TC-2025-0001");
        issued.setStatus(CertificateStatus.ISSUED);
        return issued;
    }

    private Student student() {
        Student student = new Student();
        student.setId(STUDENT_A);
        student.setSchoolId(TestSecurity.SCHOOL_ID);
        student.setUserId(USER_A);
        student.setAdmissionNo("ADM0001");
        student.setFirstName("Aarav");
        student.setStatus(StudentStatus.ACTIVE);
        return student;
    }

}
