package com.schoolms.leave;

import com.schoolms.TestSecurity;
import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.TeacherProfile;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.common.enums.StaffType;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.file.MinioService;
import com.schoolms.leave.dto.ApplyLeaveRequest;
import com.schoolms.leave.dto.CancelLeaveRequest;
import com.schoolms.leave.dto.LeaveRequestDto;
import com.schoolms.leave.dto.LeaveTypeRequest;
import com.schoolms.leave.dto.RejectLeaveRequest;
import com.schoolms.notification.NotificationTriggerService;
import com.schoolms.staff.NonTeachingStaffRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    private static final UUID TEACHER_ID = UUID.fromString("30000000-0000-0000-0000-000000000041");
    private static final UUID TYPE_ID = UUID.fromString("a1000000-0000-0000-0000-000000000001");
    private static final UUID POLICY_ID = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    private static final UUID REQUEST_ID = UUID.fromString("c1000000-0000-0000-0000-000000000001");
    private static final UUID BALANCE_ID = UUID.fromString("d1000000-0000-0000-0000-000000000001");
    private static final UUID YEAR_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER = UUID.fromString("20000000-0000-0000-0000-000000000099");

    @Mock private LeaveTypeRepository typeRepository;
    @Mock private LeavePolicyRepository policyRepository;
    @Mock private LeavePolicyItemRepository policyItemRepository;
    @Mock private LeaveYearSettingRepository yearSettingRepository;
    @Mock private LeaveAssignmentRepository assignmentRepository;
    @Mock private LeaveBalanceRepository balanceRepository;
    @Mock private LeaveBalanceLedgerRepository ledgerRepository;
    @Mock private LeaveRequestRepository requestRepository;
    @Mock private LeaveAuditRepository auditRepository;
    @Mock private LeaveDayCalculator dayCalculator;
    @Mock private TeacherProfileRepository teacherRepository;
    @Mock private NonTeachingStaffRepository nonTeachingStaffRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private MinioService minioService;
    @Mock private NotificationTriggerService notifications;

    private LeaveService service;

    @BeforeEach
    void setUp() {
        service = new LeaveService(
                typeRepository, policyRepository, policyItemRepository, yearSettingRepository,
                assignmentRepository, balanceRepository, ledgerRepository, requestRepository,
                auditRepository, dayCalculator, teacherRepository, nonTeachingStaffRepository,
                academicYearRepository, minioService, notifications);
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("TEACHER"),
                Set.of("LEAVE_READ", "LEAVE_APPLY"));
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void studentCannotListTypes() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("STUDENT"), Set.of());
        assertThrows(AccessDeniedException.class, () -> service.listTypes());
    }

    @Test
    void applyReservesPendingBalance() {
        stubTeacher();
        stubYear();
        LeaveType type = type(true);
        when(typeRepository.findByIdAndSchoolId(TYPE_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(type));
        LeaveAssignment assignment = assignment();
        when(assignmentRepository.findBySchoolIdAndStaffTypeAndStaffId(
                TestSecurity.SCHOOL_ID, StaffType.TEACHING, TEACHER_ID)).thenReturn(Optional.of(assignment));
        LeavePolicyItem item = new LeavePolicyItem();
        item.setLeaveTypeId(TYPE_ID);
        when(policyItemRepository.findByPolicyIdOrderByCreatedAtAsc(POLICY_ID)).thenReturn(List.of(item));
        when(dayCalculator.countWorkingDays(eq(TestSecurity.SCHOOL_ID), any(), any())).thenReturn(new BigDecimal("2.0"));
        when(requestRepository.findOverlapping(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(requestRepository.existsBySchoolIdAndStaffTypeAndStaffIdAndLeaveTypeIdAndFromDateAndToDateAndStatus(
                any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
        LeaveBalance balance = balance(new BigDecimal("12.0"), BigDecimal.ZERO, BigDecimal.ZERO);
        when(balanceRepository.lockByStaffAndType(any(), any(), any(), any(), any())).thenReturn(Optional.of(balance));
        when(requestRepository.save(any())).thenAnswer(invocation -> {
            LeaveRequest saved = invocation.getArgument(0);
            saved.setId(REQUEST_ID);
            return saved;
        });
        when(balanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequestDto dto = service.apply(new ApplyLeaveRequest(
                TYPE_ID, LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 7), "Family function"), null);

        assertEquals(LeaveRequestStatus.PENDING, dto.status());
        assertEquals(new BigDecimal("2.0"), dto.days());
        assertEquals(new BigDecimal("2.0"), balance.getPending());
        verify(notifications).onLeaveRequested(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void applyBlocksOverlap() {
        stubTeacher();
        LeaveType type = type(true);
        when(typeRepository.findByIdAndSchoolId(TYPE_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(type));
        when(assignmentRepository.findBySchoolIdAndStaffTypeAndStaffId(
                TestSecurity.SCHOOL_ID, StaffType.TEACHING, TEACHER_ID)).thenReturn(Optional.of(assignment()));
        LeavePolicyItem item = new LeavePolicyItem();
        item.setLeaveTypeId(TYPE_ID);
        when(policyItemRepository.findByPolicyIdOrderByCreatedAtAsc(POLICY_ID)).thenReturn(List.of(item));
        when(dayCalculator.countWorkingDays(any(), any(), any())).thenReturn(new BigDecimal("1.0"));
        when(requestRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new LeaveRequest()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.apply(
                new ApplyLeaveRequest(TYPE_ID, LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 6), "Overlap"), null));
        assertEquals("leave.overlap", ex.getCode());
    }

    @Test
    void applyBlocksInactiveType() {
        stubTeacher();
        LeaveType type = type(false);
        when(typeRepository.findByIdAndSchoolId(TYPE_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(type));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.apply(
                new ApplyLeaveRequest(TYPE_ID, LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 6), "Medical"), null));
        assertEquals("leave.type_inactive", ex.getCode());
    }

    @Test
    void approveMovesPendingToUsed() {
        loginApprover();
        stubYear();
        LeaveRequest request = pendingRequest();
        when(requestRepository.findByIdAndSchoolId(REQUEST_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(request));
        when(teacherRepository.findByIdAndSchoolId(TEACHER_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(teacher()));
        LeaveBalance balance = balance(new BigDecimal("12.0"), BigDecimal.ZERO, new BigDecimal("2.0"));
        when(balanceRepository.lockByStaffAndType(any(), any(), any(), any(), any())).thenReturn(Optional.of(balance));
        when(typeRepository.findByIdAndSchoolId(TYPE_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(type(true)));
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequestDto dto = service.approve(REQUEST_ID);

        assertEquals(LeaveRequestStatus.APPROVED, dto.status());
        assertEquals(new BigDecimal("2.0"), balance.getUsed());
        assertEquals(0, balance.getPending().compareTo(BigDecimal.ZERO));
        verify(notifications).onLeaveDecision(any(), any(), eq(true), any(), any(), any(), any());
    }

    @Test
    void rejectRequiresPendingAndReleasesDays() {
        loginApprover();
        stubYear();
        LeaveRequest request = pendingRequest();
        when(requestRepository.findByIdAndSchoolId(REQUEST_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(request));
        when(teacherRepository.findByIdAndSchoolId(TEACHER_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(teacher()));
        LeaveBalance balance = balance(new BigDecimal("12.0"), BigDecimal.ZERO, new BigDecimal("2.0"));
        when(balanceRepository.lockByStaffAndType(any(), any(), any(), any(), any())).thenReturn(Optional.of(balance));
        when(typeRepository.findByIdAndSchoolId(TYPE_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(type(true)));
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequestDto dto = service.reject(REQUEST_ID, new RejectLeaveRequest("Coverage missing"));

        assertEquals(LeaveRequestStatus.REJECTED, dto.status());
        assertEquals(0, balance.getPending().compareTo(BigDecimal.ZERO));
        verify(notifications).onLeaveDecision(any(), any(), eq(false), any(), any(), any(), eq("Coverage missing"));
    }

    @Test
    void alreadyActionedCannotBeApprovedAgain() {
        loginApprover();
        LeaveRequest request = pendingRequest();
        request.setStatus(LeaveRequestStatus.APPROVED);
        when(requestRepository.findByIdAndSchoolId(REQUEST_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(request));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approve(REQUEST_ID));
        assertEquals("leave.already_actioned", ex.getCode());
        verify(notifications, never()).onLeaveDecision(any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void cancelPendingRestoresBalance() {
        stubTeacher();
        stubYear();
        LeaveRequest request = pendingRequest();
        request.setUserId(TestSecurity.USER_ID);
        when(requestRepository.findByIdAndSchoolId(REQUEST_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(request));
        when(teacherRepository.findByIdAndSchoolId(TEACHER_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(teacher()));
        LeaveBalance balance = balance(new BigDecimal("12.0"), BigDecimal.ZERO, new BigDecimal("2.0"));
        when(balanceRepository.lockByStaffAndType(any(), any(), any(), any(), any())).thenReturn(Optional.of(balance));
        when(typeRepository.findByIdAndSchoolId(TYPE_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(type(true)));
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequestDto dto = service.cancel(REQUEST_ID, new CancelLeaveRequest("Plans changed"));

        assertEquals(LeaveRequestStatus.CANCELLED, dto.status());
        assertEquals(0, balance.getPending().compareTo(BigDecimal.ZERO));
    }

    @Test
    void duplicateTypeCodeIsRejected() {
        loginApprover();
        when(typeRepository.existsBySchoolIdAndCodeIgnoreCase(TestSecurity.SCHOOL_ID, "CL")).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createType(
                new LeaveTypeRequest("CL", "Casual", new BigDecimal("12"), true, true, true)));
        assertEquals("leave.type_code_exists", ex.getCode());
    }

    private void loginApprover() {
        TestSecurity.login(OTHER_USER, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("LEAVE_READ", "LEAVE_APPLY", "LEAVE_MANAGE", "LEAVE_APPROVE"));
    }

    private void stubTeacher() {
        when(teacherRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(Optional.of(teacher()));
    }

    private void stubYear() {
        AcademicYear year = new AcademicYear();
        year.setId(YEAR_ID);
        year.setStartDate(LocalDate.of(2025, 4, 1));
        year.setEndDate(LocalDate.of(2026, 3, 31));
        year.setCurrent(true);
        when(yearSettingRepository.findBySchoolId(TestSecurity.SCHOOL_ID)).thenReturn(Optional.empty());
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(year));
    }

    private TeacherProfile teacher() {
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(TEACHER_ID);
        teacher.setSchoolId(TestSecurity.SCHOOL_ID);
        teacher.setUserId(TestSecurity.USER_ID);
        teacher.setEmployeeNo("EMP001");
        teacher.setFirstName("Asha");
        teacher.setLastName("Sharma");
        teacher.setJoinDate(LocalDate.of(2022, 6, 1));
        return teacher;
    }

    private LeaveType type(boolean active) {
        LeaveType type = new LeaveType();
        type.setId(TYPE_ID);
        type.setSchoolId(TestSecurity.SCHOOL_ID);
        type.setCode("CL");
        type.setName("Casual Leave");
        type.setYearlyQuota(new BigDecimal("12.0"));
        type.setPaid(true);
        type.setReasonMandatory(true);
        type.setActive(active);
        return type;
    }

    private LeaveAssignment assignment() {
        LeaveAssignment assignment = new LeaveAssignment();
        assignment.setSchoolId(TestSecurity.SCHOOL_ID);
        assignment.setStaffType(StaffType.TEACHING);
        assignment.setStaffId(TEACHER_ID);
        assignment.setPolicyId(POLICY_ID);
        assignment.setYearStart(LocalDate.of(2025, 4, 1));
        return assignment;
    }

    private LeaveBalance balance(BigDecimal entitled, BigDecimal used, BigDecimal pending) {
        LeaveBalance balance = new LeaveBalance();
        balance.setId(BALANCE_ID);
        balance.setSchoolId(TestSecurity.SCHOOL_ID);
        balance.setStaffType(StaffType.TEACHING);
        balance.setStaffId(TEACHER_ID);
        balance.setLeaveTypeId(TYPE_ID);
        balance.setYearStart(LocalDate.of(2025, 4, 1));
        balance.setEntitled(entitled);
        balance.setUsed(used);
        balance.setPending(pending);
        return balance;
    }

    private LeaveRequest pendingRequest() {
        LeaveRequest request = new LeaveRequest();
        request.setId(REQUEST_ID);
        request.setSchoolId(TestSecurity.SCHOOL_ID);
        request.setStaffType(StaffType.TEACHING);
        request.setStaffId(TEACHER_ID);
        request.setUserId(TestSecurity.USER_ID);
        request.setLeaveTypeId(TYPE_ID);
        request.setFromDate(LocalDate.of(2026, 4, 6));
        request.setToDate(LocalDate.of(2026, 4, 7));
        request.setDays(new BigDecimal("2.0"));
        request.setReason("Family function");
        request.setStatus(LeaveRequestStatus.PENDING);
        return request;
    }
}
