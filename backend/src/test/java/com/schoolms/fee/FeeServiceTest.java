package com.schoolms.fee;

import com.schoolms.TestSecurity;
import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.common.enums.FeeFrequency;
import com.schoolms.fee.dto.AdhocFeeLevyRequest;
import com.schoolms.fee.dto.CloneFeeStructureRequest;
import com.schoolms.fee.dto.CloneFeeStructureResult;
import com.schoolms.fee.dto.FeeAssignmentDto;
import com.schoolms.fee.dto.FeeAssignmentRequest;
import com.schoolms.fee.dto.FeeCategoryDto;
import com.schoolms.fee.dto.FeeCategoryRequest;
import com.schoolms.fee.dto.FeeStructureDto;
import com.schoolms.fee.dto.FeeStructureRequest;
import com.schoolms.fee.dto.FeeStructureUpdateRequest;
import com.schoolms.school.SchoolRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import com.schoolms.user.UserRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeServiceTest {

    @Mock private FeeCategoryRepository categoryRepository;
    @Mock private FeeStructureRepository structureRepository;
    @Mock private StudentFeeAssignmentRepository assignmentRepository;
    @Mock private FeeInstallmentRepository installmentRepository;
    @Mock private FeePaymentRepository paymentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private FeePdfService feePdfService;
    @Mock private SiblingDiscountRuleRepository siblingRuleRepository;
    @Mock private AdhocFeeLevyRepository levyRepository;
    @Mock private FeeStructureAuditRepository auditRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private UserRepository userRepository;

    private FeeService feeService;

    private final UUID schoolId = TestSecurity.SCHOOL_ID;
    private final UUID yearId = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private final UUID priorYearId = UUID.fromString("30000000-0000-0000-0000-0000000000a1");
    private final UUID class5Id = UUID.fromString("30000000-0000-0000-0000-000000000011");
    private final UUID class6Id = UUID.fromString("30000000-0000-0000-0000-000000000012");
    private final UUID tuitionId = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private final UUID studentId = UUID.fromString("40000000-0000-0000-0000-000000000030");
    private final UUID siblingId = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private final UUID structureId = UUID.fromString("50000000-0000-0000-0000-000000000011");

    @BeforeEach
    void setUp() {
        feeService = new FeeService(
                categoryRepository, structureRepository, assignmentRepository, installmentRepository,
                paymentRepository, studentRepository, academicYearRepository, classRepository,
                sectionRepository, enrollmentRepository, schoolRepository, feePdfService,
                siblingRuleRepository, levyRepository, auditRepository, studentGuardianRepository,
                userRepository);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createCategoryPersistsFrequencyAndFlags() {
        when(categoryRepository.existsBySchoolIdAndCode(schoolId, "EXAM")).thenReturn(false);
        when(categoryRepository.save(any(FeeCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        FeeCategoryDto dto = feeService.createCategory(new FeeCategoryRequest(
                "Examination Fee", "exam", "Term exam", false, false, "ONE_TIME", "ACTIVE"));

        assertEquals("EXAM", dto.code());
        assertEquals("ONE_TIME", dto.frequency());
        assertEquals("ACTIVE", dto.status());
    }

    @Test
    void createStructureUsesDifferentAmountsPerClass() {
        when(academicYearRepository.findByIdAndSchoolId(yearId, schoolId)).thenReturn(Optional.of(year()));
        when(categoryRepository.findByIdAndSchoolId(tuitionId, schoolId)).thenReturn(Optional.of(tuition()));
        when(classRepository.findByIdAndSchoolId(class5Id, schoolId)).thenReturn(Optional.of(schoolClass(class5Id, "Class 5")));
        when(structureRepository.existsBySchoolIdAndClassIdAndAcademicYearIdAndCategoryId(
                schoolId, class5Id, yearId, tuitionId)).thenReturn(false);
        when(structureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepository.save(any(FeeStructureAudit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(classRepository.findByIdAndSchoolId(class5Id, schoolId)).thenReturn(Optional.of(schoolClass(class5Id, "Class 5")));
        when(categoryRepository.findByIdAndSchoolId(tuitionId, schoolId)).thenReturn(Optional.of(tuition()));

        List<FeeStructureDto> created = feeService.createStructure(new FeeStructureRequest(
                class5Id, false, yearId, tuitionId, new BigDecimal("1500.00"),
                "MONTHLY", (short) 10, null, null, null));

        assertEquals(1, created.size());
        assertEquals(new BigDecimal("1500.00"), created.getFirst().amount());
        verify(auditRepository).save(any(FeeStructureAudit.class));
    }

    @Test
    void cloneCopiesPriorYearStructures() {
        FeeStructure source = structure(structureId, class5Id, priorYearId, new BigDecimal("1400.00"));
        when(academicYearRepository.findByIdAndSchoolId(priorYearId, schoolId)).thenReturn(Optional.of(priorYear()));
        when(academicYearRepository.findByIdAndSchoolId(yearId, schoolId)).thenReturn(Optional.of(year()));
        when(structureRepository.findBySchoolIdAndAcademicYearIdOrderByClassIdAsc(schoolId, priorYearId))
                .thenReturn(List.of(source));
        when(structureRepository.existsBySchoolIdAndClassIdAndAcademicYearIdAndCategoryId(
                schoolId, class5Id, yearId, tuitionId)).thenReturn(false);
        when(structureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepository.save(any(FeeStructureAudit.class))).thenAnswer(inv -> inv.getArgument(0));

        CloneFeeStructureResult result = feeService.cloneStructures(new CloneFeeStructureRequest(priorYearId, yearId));

        assertEquals(1, result.cloned());
        assertEquals(0, result.skipped());
        ArgumentCaptor<FeeStructureAudit> captor = ArgumentCaptor.forClass(FeeStructureAudit.class);
        verify(auditRepository).save(captor.capture());
        assertEquals("CLONE", captor.getValue().getAction());
    }

    @Test
    void updateStructureWritesAuditTrail() {
        FeeStructure existing = structure(structureId, class5Id, yearId, new BigDecimal("1500.00"));
        when(structureRepository.findByIdAndSchoolId(structureId, schoolId)).thenReturn(Optional.of(existing));
        when(structureRepository.save(existing)).thenReturn(existing);
        when(auditRepository.save(any(FeeStructureAudit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(classRepository.findByIdAndSchoolId(class5Id, schoolId)).thenReturn(Optional.of(schoolClass(class5Id, "Class 5")));
        when(categoryRepository.findByIdAndSchoolId(tuitionId, schoolId)).thenReturn(Optional.of(tuition()));

        feeService.updateStructure(structureId, new FeeStructureUpdateRequest(
                new BigDecimal("1800.00"), "MONTHLY", (short) 10, null, null, null));

        ArgumentCaptor<FeeStructureAudit> captor = ArgumentCaptor.forClass(FeeStructureAudit.class);
        verify(auditRepository).save(captor.capture());
        assertEquals("UPDATE", captor.getValue().getAction());
        assertEquals(new BigDecimal("1500.00"), captor.getValue().getPreviousAmount());
        assertEquals(new BigDecimal("1800.00"), captor.getValue().getNewAmount());
    }

    @Test
    void assignAppliesSecondChildSiblingDiscount() {
        Student younger = student(studentId, "Anya", LocalDate.of(2016, 9, 1));
        Student elder = student(siblingId, "Aarav", LocalDate.of(2014, 5, 10));
        FeeStructure structure = structure(structureId, class5Id, yearId, new BigDecimal("1500.00"));
        when(studentRepository.findByIdAndSchoolId(studentId, schoolId)).thenReturn(Optional.of(younger));
        when(structureRepository.findByIdAndSchoolId(structureId, schoolId)).thenReturn(Optional.of(structure));
        when(assignmentRepository.existsByStudentIdAndFeeStructureId(studentId, structureId)).thenReturn(false);
        when(studentGuardianRepository.findSiblingStudentIds(schoolId, studentId))
                .thenReturn(List.of(studentId, siblingId));
        when(studentRepository.findBySchoolIdAndIdIn(schoolId, List.of(studentId, siblingId)))
                .thenReturn(List.of(younger, elder));
        SiblingDiscountRule rule = new SiblingDiscountRule();
        rule.setFeeCategoryId(tuitionId);
        rule.setSiblingOrder((short) 2);
        rule.setDiscountType("PERCENT");
        rule.setDiscountValue(new BigDecimal("10"));
        when(siblingRuleRepository.findBySchoolIdAndAcademicYearIdAndStatusOrderBySiblingOrderAsc(
                schoolId, yearId, "ACTIVE")).thenReturn(List.of(rule));
        when(assignmentRepository.save(any(StudentFeeAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(academicYearRepository.findByIdAndSchoolId(yearId, schoolId)).thenReturn(Optional.of(year()));
        when(installmentRepository.save(any(FeeInstallment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.findByIdAndSchoolId(tuitionId, schoolId)).thenReturn(Optional.of(tuition()));

        FeeAssignmentDto dto = feeService.assign(new FeeAssignmentRequest(studentId, structureId, null, null));

        assertEquals("PERCENT", dto.discountType());
        assertEquals(new BigDecimal("10"), dto.discountAmount());
        ArgumentCaptor<FeeInstallment> inst = ArgumentCaptor.forClass(FeeInstallment.class);
        verify(installmentRepository, times(12)).save(inst.capture());
        assertEquals(new BigDecimal("1350.00"), inst.getAllValues().getFirst().getAmountDue());
    }

    @Test
    void adhocLevyAssignsIndividualStudent() {
        when(academicYearRepository.findByIdAndSchoolId(yearId, schoolId)).thenReturn(Optional.of(year()));
        FeeCategory lostId = tuition();
        lostId.setId(UUID.fromString("50000000-0000-0000-0000-000000000005"));
        lostId.setName("Lost ID Card Fee");
        lostId.setCode("LOST_ID");
        when(categoryRepository.findByIdAndSchoolId(lostId.getId(), schoolId)).thenReturn(Optional.of(lostId));
        when(studentRepository.findByIdAndSchoolId(studentId, schoolId)).thenReturn(Optional.of(student(studentId, "Anya", LocalDate.of(2016, 9, 1))));
        when(levyRepository.save(any(AdhocFeeLevy.class))).thenAnswer(inv -> inv.getArgument(0));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId))
                .thenReturn(Optional.of(enrollment(class5Id)));
        when(sectionRepository.findByIdAndSchoolId(any(), any())).thenReturn(Optional.of(section(class5Id)));
        when(structureRepository.findBySchoolIdAndAcademicYearIdAndClassId(schoolId, yearId, class5Id))
                .thenReturn(List.of());
        when(structureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepository.save(any(FeeStructureAudit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentRepository.existsByStudentIdAndFeeStructureId(any(), any())).thenReturn(false);
        when(assignmentRepository.save(any(StudentFeeAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(installmentRepository.save(any(FeeInstallment.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = feeService.createLevy(new AdhocFeeLevyRequest(
                yearId, lostId.getId(), new BigDecimal("250.00"), LocalDate.of(2025, 10, 1),
                "STUDENT", null, null, studentId, "Lost ID"));

        assertEquals(1, dto.assignedCount());
        assertEquals("STUDENT", dto.scope());
        verify(assignmentRepository).save(any(StudentFeeAssignment.class));
        ArgumentCaptor<FeeInstallment> inst = ArgumentCaptor.forClass(FeeInstallment.class);
        verify(installmentRepository).save(inst.capture());
        assertEquals(new BigDecimal("250.00"), inst.getValue().getAmountDue());
        assertEquals(LocalDate.of(2025, 10, 1), inst.getValue().getDueDate());
    }

    @Test
    void siblingDiscountSkipsEldestChild() {
        Student younger = student(studentId, "Anya", LocalDate.of(2016, 9, 1));
        Student elder = student(siblingId, "Aarav", LocalDate.of(2014, 5, 10));
        FeeStructure structure = structure(structureId, class5Id, yearId, new BigDecimal("1500.00"));
        when(studentGuardianRepository.findSiblingStudentIds(schoolId, siblingId))
                .thenReturn(List.of(studentId, siblingId));
        when(studentRepository.findBySchoolIdAndIdIn(schoolId, List.of(studentId, siblingId)))
                .thenReturn(List.of(younger, elder));

        Optional<FeeService.SiblingDiscount> discount = feeService.resolveSiblingDiscount(siblingId, structure, schoolId);

        assertTrue(discount.isEmpty());
    }

    private AcademicYear year() {
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        year.setSchoolId(schoolId);
        year.setName("2025-26");
        year.setStartDate(LocalDate.of(2025, 4, 1));
        year.setEndDate(LocalDate.of(2026, 3, 31));
        year.setCurrent(true);
        return year;
    }

    private AcademicYear priorYear() {
        AcademicYear year = new AcademicYear();
        year.setId(priorYearId);
        year.setSchoolId(schoolId);
        year.setName("2024-25");
        year.setStartDate(LocalDate.of(2024, 4, 1));
        year.setEndDate(LocalDate.of(2025, 3, 31));
        return year;
    }

    private FeeCategory tuition() {
        FeeCategory category = new FeeCategory();
        category.setId(tuitionId);
        category.setSchoolId(schoolId);
        category.setName("Tuition Fee");
        category.setCode("TUITION");
        category.setFrequency(FeeFrequency.MONTHLY);
        category.setStatus("ACTIVE");
        return category;
    }

    private SchoolClass schoolClass(UUID id, String name) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setSchoolId(schoolId);
        schoolClass.setName(name);
        return schoolClass;
    }

    private FeeStructure structure(UUID id, UUID classId, UUID academicYearId, BigDecimal amount) {
        FeeStructure structure = new FeeStructure();
        structure.setId(id);
        structure.setSchoolId(schoolId);
        structure.setClassId(classId);
        structure.setAcademicYearId(academicYearId);
        structure.setCategoryId(tuitionId);
        structure.setAmount(amount);
        structure.setFrequency(FeeFrequency.MONTHLY);
        structure.setDueDay((short) 10);
        return structure;
    }

    private Student student(UUID id, String firstName, LocalDate dob) {
        Student student = new Student();
        student.setId(id);
        student.setSchoolId(schoolId);
        student.setFirstName(firstName);
        student.setLastName("Kumar");
        student.setAdmissionNo("ADM" + id.toString().substring(32));
        student.setDateOfBirth(dob);
        student.setAdmissionDate(LocalDate.of(2025, 4, 1));
        return student;
    }

    private com.schoolms.academics.StudentEnrollment enrollment(UUID classId) {
        com.schoolms.academics.StudentEnrollment enrollment = new com.schoolms.academics.StudentEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setSectionId(UUID.fromString("30000000-0000-0000-0000-000000000021"));
        enrollment.setAcademicYearId(yearId);
        enrollment.setStatus("ACTIVE");
        return enrollment;
    }

    private com.schoolms.academics.Section section(UUID classId) {
        com.schoolms.academics.Section section = new com.schoolms.academics.Section();
        section.setId(UUID.fromString("30000000-0000-0000-0000-000000000021"));
        section.setClassId(classId);
        section.setName("A");
        return section;
    }
}
