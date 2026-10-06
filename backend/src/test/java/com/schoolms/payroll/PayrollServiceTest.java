package com.schoolms.payroll;

import com.schoolms.TestSecurity;
import com.schoolms.academics.TeacherProfile;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.common.enums.PayrollRunStatus;
import com.schoolms.common.enums.StaffType;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.payroll.dto.PayrollRunRequest;
import com.schoolms.payroll.dto.PayslipDto;
import com.schoolms.payroll.dto.SalaryLineItemDto;
import com.schoolms.payroll.dto.SalaryStructureRequest;
import com.schoolms.staff.NonTeachingStaffRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock private StaffSalaryStructureRepository salaryRepository;
    @Mock private PayrollRunRepository runRepository;
    @Mock private PayslipRepository payslipRepository;
    @Mock private TeacherProfileRepository teacherRepository;
    @Mock private NonTeachingStaffRepository nonTeachingStaffRepository;
    @Mock private PayslipPdfService payslipPdfService;

    private PayrollService payrollService;

    private final UUID schoolId = TestSecurity.SCHOOL_ID;
    private final UUID teacherId = UUID.fromString("30000000-0000-0000-0000-000000000041");
    private final UUID runId = UUID.fromString("a4000000-0000-0000-0000-000000000001");
    private final UUID slipId = UUID.fromString("a5000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        payrollService = new PayrollService(salaryRepository, runRepository, payslipRepository,
                teacherRepository, nonTeachingStaffRepository, payslipPdfService);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void publishMovesProcessedRunToPublished() {
        PayrollRun run = run(PayrollRunStatus.PROCESSED);
        when(runRepository.findByIdAndSchoolId(runId, schoolId)).thenReturn(Optional.of(run));
        when(runRepository.save(run)).thenReturn(run);
        when(payslipRepository.findByPayrollRunIdOrderByStaffNameAsc(runId)).thenReturn(List.of());

        var dto = payrollService.publish(runId);

        assertEquals(PayrollRunStatus.PUBLISHED, dto.status());
        assertEquals(PayrollRunStatus.PUBLISHED, run.getStatus());
    }

    @Test
    void publishRejectsDraftRun() {
        when(runRepository.findByIdAndSchoolId(runId, schoolId)).thenReturn(Optional.of(run(PayrollRunStatus.DRAFT)));
        assertThrows(BusinessException.class, () -> payrollService.publish(runId));
        verify(runRepository, never()).save(any());
    }

    @Test
    void processSnapshotsSalaryOntoPayslip() {
        PayrollRun run = run(PayrollRunStatus.DRAFT);
        when(runRepository.findByIdAndSchoolId(runId, schoolId)).thenReturn(Optional.of(run));
        when(payslipRepository.existsByPayrollRunId(runId)).thenReturn(false);

        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(teacherId);
        teacher.setSchoolId(schoolId);
        teacher.setUserId(TestSecurity.USER_ID);
        teacher.setEmployeeNo("EMP001");
        teacher.setFirstName("Asha");
        teacher.setLastName("Sharma");
        teacher.setDesignation("Primary Teacher");
        teacher.setDepartment("Primary");
        teacher.setStatus("ACTIVE");
        when(teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId)).thenReturn(List.of(teacher));
        when(nonTeachingStaffRepository.findBySchoolIdOrderByFirstNameAsc(schoolId)).thenReturn(List.of());

        StaffSalaryStructure structure = new StaffSalaryStructure();
        structure.setBasic(new BigDecimal("35000.00"));
        structure.setHra(new BigDecimal("14000.00"));
        structure.setAllowancesJson("[{\"name\":\"Transport\",\"amount\":2000}]");
        structure.setDeductionsJson("[{\"name\":\"PF\",\"amount\":4200}]");
        when(salaryRepository.findFirstBySchoolIdAndStaffTypeAndStaffIdAndStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                eq(schoolId), eq(StaffType.TEACHING), eq(teacherId), eq("ACTIVE"), any(LocalDate.class)))
                .thenReturn(Optional.of(structure));
        when(payslipRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(runRepository.save(run)).thenReturn(run);
        when(payslipRepository.findByPayrollRunIdOrderByStaffNameAsc(runId)).thenReturn(List.of());

        payrollService.process(runId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Payslip>> captor = ArgumentCaptor.forClass(List.class);
        verify(payslipRepository).saveAll(captor.capture());
        Payslip slip = captor.getValue().getFirst();
        assertEquals(new BigDecimal("35000.00"), slip.getBasic());
        assertEquals(new BigDecimal("14000.00"), slip.getHra());
        assertEquals(new BigDecimal("51000.00"), slip.getGross());
        assertEquals(new BigDecimal("4200.00"), slip.getTotalDeductions());
        assertEquals(new BigDecimal("46800.00"), slip.getNet());
        assertEquals(PayrollRunStatus.PROCESSED, run.getStatus());
    }

    @Test
    void myPayslipsHidesUnpublishedRuns() {
        TestSecurity.login(TestSecurity.USER_ID, schoolId, List.of("TEACHER"), Set.of("PAYSLIP_READ"));
        Payslip published = payslip(TestSecurity.USER_ID);
        Payslip hidden = payslip(TestSecurity.USER_ID);
        hidden.setId(UUID.fromString("a5000000-0000-0000-0000-000000000099"));
        hidden.setPayrollRunId(UUID.fromString("a4000000-0000-0000-0000-000000000002"));
        when(payslipRepository.findBySchoolIdAndUserIdOrderByCreatedAtDesc(schoolId, TestSecurity.USER_ID))
                .thenReturn(List.of(published, hidden));
        when(runRepository.findByIdAndSchoolId(runId, schoolId))
                .thenReturn(Optional.of(run(PayrollRunStatus.PUBLISHED)));
        when(runRepository.findByIdAndSchoolId(hidden.getPayrollRunId(), schoolId))
                .thenReturn(Optional.of(run(PayrollRunStatus.PROCESSED)));

        List<PayslipDto> result = payrollService.myPayslips();

        assertEquals(1, result.size());
        assertEquals(published.getId(), result.getFirst().id());
        assertEquals(PayrollRunStatus.PUBLISHED, result.getFirst().runStatus());
    }

    @Test
    void downloadRejectsOtherUsersPayslip() {
        TestSecurity.login(TestSecurity.USER_ID, schoolId, List.of("TEACHER"), Set.of("PAYSLIP_READ"));
        Payslip other = payslip(UUID.fromString("20000000-0000-0000-0000-000000000099"));
        when(payslipRepository.findByIdAndSchoolId(slipId, schoolId)).thenReturn(Optional.of(other));
        assertThrows(AuthException.class, () -> payrollService.downloadMyPayslip(slipId));
        verify(payslipPdfService, never()).render(any(), any(), any());
    }

    @Test
    void downloadRejectsUnpublishedPayslip() {
        TestSecurity.login(TestSecurity.USER_ID, schoolId, List.of("TEACHER"), Set.of("PAYSLIP_READ"));
        when(payslipRepository.findByIdAndSchoolId(slipId, schoolId)).thenReturn(Optional.of(payslip(TestSecurity.USER_ID)));
        when(runRepository.findByIdAndSchoolId(runId, schoolId)).thenReturn(Optional.of(run(PayrollRunStatus.PROCESSED)));
        BusinessException ex = assertThrows(BusinessException.class, () -> payrollService.downloadMyPayslip(slipId));
        assertEquals("payroll.payslip_not_published", ex.getCode());
    }

    @Test
    void createRunRejectsDuplicatePeriod() {
        when(runRepository.existsBySchoolIdAndYearAndMonth(schoolId, (short) 2026, (short) 8)).thenReturn(true);
        assertThrows(BusinessException.class, () -> payrollService.createRun(new PayrollRunRequest(2026, 8, null)));
    }

    @Test
    void upsertSalaryStructurePersistsSnapshotJson() {
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(teacherId);
        teacher.setFirstName("Asha");
        teacher.setLastName("Sharma");
        teacher.setEmployeeNo("EMP001");
        when(teacherRepository.findByIdAndSchoolId(teacherId, schoolId)).thenReturn(Optional.of(teacher));
        when(salaryRepository.save(any(StaffSalaryStructure.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = payrollService.upsertSalaryStructure(new SalaryStructureRequest(
                StaffType.TEACHING,
                teacherId,
                new BigDecimal("35000"),
                new BigDecimal("14000"),
                List.of(new SalaryLineItemDto("Transport", new BigDecimal("2000"))),
                List.of(new SalaryLineItemDto("PF", new BigDecimal("4200"))),
                LocalDate.of(2026, 4, 1),
                null));

        assertEquals("Asha Sharma", dto.staffName());
        assertEquals(new BigDecimal("51000.00"), dto.gross());
        assertEquals(1, dto.allowances().size());
        assertEquals("Transport", dto.allowances().getFirst().name());
    }

    private PayrollRun run(PayrollRunStatus status) {
        PayrollRun run = new PayrollRun();
        run.setId(runId);
        run.setSchoolId(schoolId);
        run.setYear((short) 2026);
        run.setMonth((short) 8);
        run.setStatus(status);
        return run;
    }

    private Payslip payslip(UUID userId) {
        Payslip slip = new Payslip();
        slip.setId(slipId);
        slip.setSchoolId(schoolId);
        slip.setPayrollRunId(runId);
        slip.setStaffType(StaffType.TEACHING);
        slip.setStaffId(teacherId);
        slip.setUserId(userId);
        slip.setEmployeeNo("EMP001");
        slip.setStaffName("Asha Sharma");
        slip.setBasic(new BigDecimal("35000.00"));
        slip.setHra(new BigDecimal("14000.00"));
        slip.setAllowancesJson("[]");
        slip.setDeductionsJson("[]");
        slip.setGross(new BigDecimal("49000.00"));
        slip.setTotalDeductions(BigDecimal.ZERO);
        slip.setNet(new BigDecimal("49000.00"));
        return slip;
    }
}
