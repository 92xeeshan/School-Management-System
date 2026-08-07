package com.schoolms.fee;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StudentEnrollment;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.common.enums.FeeFrequency;
import com.schoolms.common.enums.InstallmentStatus;
import com.schoolms.common.enums.PaymentMethod;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.fee.dto.FeeAssignmentDto;
import com.schoolms.fee.dto.FeeAssignmentRequest;
import com.schoolms.fee.dto.FeeCategoryDto;
import com.schoolms.fee.dto.FeeCategoryRequest;
import com.schoolms.fee.dto.FeePaymentDto;
import com.schoolms.fee.dto.FeeStructureDto;
import com.schoolms.fee.dto.FeeStructureRequest;
import com.schoolms.fee.dto.InstallmentDto;
import com.schoolms.fee.dto.PaymentRequest;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.security.SecurityUtils;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeeService {

    private final FeeCategoryRepository categoryRepository;
    private final FeeStructureRepository structureRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final FeeInstallmentRepository installmentRepository;
    private final FeePaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final SchoolRepository schoolRepository;
    private final FeePdfService feePdfService;

    // ---- Categories --------------------------------------------------------
    @Transactional(readOnly = true)
    public List<FeeCategoryDto> listCategories() {
        return categoryRepository.findBySchoolIdOrderByNameAsc(SecurityUtils.currentSchoolId())
                .stream().map(FeeCategoryDto::from).toList();
    }

    @Transactional
    public FeeCategoryDto createCategory(FeeCategoryRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        if (categoryRepository.existsBySchoolIdAndCode(schoolId, request.code())) {
            throw new BusinessException("error.conflict");
        }
        FeeCategory category = new FeeCategory();
        category.setSchoolId(schoolId);
        category.setName(request.name());
        category.setCode(request.code());
        category.setDescription(request.description());
        return FeeCategoryDto.from(categoryRepository.save(category));
    }

    // ---- Structures ----------------------------------------------------------
    @Transactional(readOnly = true)
    public List<FeeStructureDto> listStructures(UUID academicYearId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Map<UUID, String> classNames = classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId).stream()
                .collect(java.util.stream.Collectors.toMap(SchoolClass::getId, SchoolClass::getName));
        Map<UUID, String> categoryNames = categoryRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .collect(java.util.stream.Collectors.toMap(FeeCategory::getId, FeeCategory::getName));
        return structureRepository.findBySchoolIdAndAcademicYearIdOrderByClassIdAsc(schoolId, academicYearId)
                .stream().map(s -> FeeStructureDto.from(s,
                        classNames.get(s.getClassId()), categoryNames.get(s.getCategoryId())))
                .toList();
    }

    @Transactional
    public FeeStructureDto createStructure(FeeStructureRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        if (structureRepository.existsBySchoolIdAndClassIdAndAcademicYearIdAndCategoryId(
                schoolId, request.classId(), request.academicYearId(), request.categoryId())) {
            throw new BusinessException("fee.structure_exists");
        }
        classRepository.findByIdAndSchoolId(request.classId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("class", request.classId()));
        academicYearRepository.findByIdAndSchoolId(request.academicYearId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", request.academicYearId()));
        FeeCategory category = categoryRepository.findByIdAndSchoolId(request.categoryId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("fee_category", request.categoryId()));

        FeeStructure structure = new FeeStructure();
        structure.setSchoolId(schoolId);
        structure.setClassId(request.classId());
        structure.setAcademicYearId(request.academicYearId());
        structure.setCategoryId(category.getId());
        structure.setAmount(request.amount());
        structure.setFrequency(parseFrequency(request.frequency()));
        structure.setDueDay(request.dueDay());
        structure.setApplicableFrom(request.applicableFrom());
        structure.setApplicableTo(request.applicableTo());
        return toStructureDto(structureRepository.save(structure), schoolId);
    }

    // ---- Assignments -----------------------------------------------------------
    @Transactional
    public FeeAssignmentDto assign(FeeAssignmentRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Student student = studentRepository.findByIdAndSchoolId(request.studentId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", request.studentId()));
        FeeStructure structure = structureRepository.findByIdAndSchoolId(request.feeStructureId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("fee_structure", request.feeStructureId()));
        if (assignmentRepository.existsByStudentIdAndFeeStructureId(student.getId(), structure.getId())) {
            throw new BusinessException("error.conflict");
        }

        StudentFeeAssignment assignment = new StudentFeeAssignment();
        assignment.setSchoolId(schoolId);
        assignment.setStudentId(student.getId());
        assignment.setFeeStructureId(structure.getId());
        assignment.setDiscountType(request.discountType());
        assignment.setDiscountAmount(request.discountAmount());
        assignment.setStatus("ACTIVE");
        assignment = assignmentRepository.save(assignment);

        generateInstallments(assignment, structure);
        return toAssignmentDto(assignment, schoolId);
    }

    @Transactional(readOnly = true)
    public List<FeeAssignmentDto> listAssignments(UUID studentId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", studentId));
        return assignmentRepository.findByStudentIdAndSchoolId(studentId, schoolId).stream()
                .map(a -> toAssignmentDto(a, schoolId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstallmentDto> listInstallments(UUID assignmentId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        assignmentRepository.findByIdAndSchoolId(assignmentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("fee_assignment", assignmentId));
        return installmentRepository.findByStudentFeeAssignmentIdOrderByDueDateAsc(assignmentId).stream()
                .map(InstallmentDto::from).toList();
    }

    // ---- Payments ---------------------------------------------------------------
    @Transactional
    public FeePaymentDto recordPayment(PaymentRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Student student = studentRepository.findByIdAndSchoolId(request.studentId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", request.studentId()));

        Optional<StudentFeeAssignment> assignment = request.studentFeeAssignmentId() == null
                ? Optional.empty()
                : assignmentRepository.findByIdAndSchoolId(request.studentFeeAssignmentId(), schoolId);

        FeePayment payment = new FeePayment();
        payment.setSchoolId(schoolId);
        payment.setReceiptNo(generateReceiptNo(schoolId));
        payment.setStudentId(student.getId());
        payment.setStudentFeeAssignmentId(assignment.map(StudentFeeAssignment::getId).orElse(null));
        payment.setAmountPaid(request.amountPaid());
        payment.setPaidAt(request.paidAt() == null ? java.time.Instant.now() : request.paidAt());
        payment.setPaymentMethod(parseMethod(request.paymentMethod()));
        payment.setReferenceNo(request.referenceNo());
        payment.setRemarks(request.remarks());
        payment.setRecordedBy(SecurityUtils.currentUserId());
        payment = paymentRepository.save(payment);

        allocatePayment(payment);
        payment.setPdfUrl(generateReceipt(payment, student));
        payment = paymentRepository.save(payment);

        return FeePaymentDto.from(payment, student.getDisplayName());
    }

    @Transactional(readOnly = true)
    public List<FeePaymentDto> listPayments(UUID studentId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", studentId));
        return paymentRepository.findByStudentIdAndSchoolIdOrderByPaidAtDesc(studentId, schoolId).stream()
                .map(p -> FeePaymentDto.from(p, student.getDisplayName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public FeePaymentDto getPayment(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        FeePayment payment = paymentRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("fee_payment", id));
        String studentName = studentRepository.findByIdAndSchoolId(payment.getStudentId(), schoolId)
                .map(Student::getDisplayName).orElse(null);
        return FeePaymentDto.from(payment, studentName);
    }

    // ---- Installment generation ---------------------------------------------------
    private void generateInstallments(StudentFeeAssignment assignment, FeeStructure structure) {
        AcademicYear year = academicYearRepository.findByIdAndSchoolId(
                        structure.getAcademicYearId(), SecurityUtils.currentSchoolId())
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", structure.getAcademicYearId()));
        List<YearMonth> periods = monthlyPeriods(year.getStartDate(), year.getEndDate());
        int step = structure.getFrequency() == FeeFrequency.MONTHLY ? 1
                : structure.getFrequency() == FeeFrequency.QUARTERLY ? 3 : periods.size();

        for (int i = 0; i < periods.size(); i += step) {
            YearMonth period = periods.get(i);
            int dueDay = structure.getDueDay() == null ? 1 : structure.getDueDay();
            int lastDay = period.lengthOfMonth();
            LocalDate dueDate = period.atDay(Math.min(dueDay, lastDay));

            FeeInstallment installment = new FeeInstallment();
            installment.setSchoolId(SecurityUtils.currentSchoolId());
            installment.setStudentFeeAssignmentId(assignment.getId());
            installment.setDueDate(dueDate);
            installment.setAmountDue(effectiveAmount(assignment, structure));
            installment.setAmountPaid(BigDecimal.ZERO);
            installment.setStatus(InstallmentStatus.PENDING);
            installmentRepository.save(installment);
        }
    }

    private List<YearMonth> monthlyPeriods(LocalDate from, LocalDate to) {
        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        List<YearMonth> periods = new java.util.ArrayList<>();
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            periods.add(ym);
        }
        return periods;
    }

    private BigDecimal effectiveAmount(StudentFeeAssignment assignment, FeeStructure structure) {
        BigDecimal amount = structure.getAmount();
        if ("PERCENT".equalsIgnoreCase(assignment.getDiscountType()) && assignment.getDiscountAmount() != null) {
            BigDecimal discount = amount.multiply(assignment.getDiscountAmount())
                    .divide(BigDecimal.valueOf(100));
            amount = amount.subtract(discount);
        } else if (assignment.getDiscountAmount() != null) {
            amount = amount.subtract(assignment.getDiscountAmount());
        }
        return amount.max(BigDecimal.ZERO);
    }

    private void allocatePayment(FeePayment payment) {
        List<FeeInstallment> installments;
        if (payment.getStudentFeeAssignmentId() != null) {
            installments = installmentRepository
                    .findByStudentFeeAssignmentIdOrderByDueDateAsc(payment.getStudentFeeAssignmentId());
        } else {
            installments = installmentRepository.findAll().stream()
                    .filter(i -> i.getSchoolId().equals(payment.getSchoolId()))
                    .filter(i -> isStudentsInstallment(i, payment.getStudentId()))
                    .sorted(java.util.Comparator.comparing(FeeInstallment::getDueDate))
                    .toList();
        }
        BigDecimal remaining = payment.getAmountPaid();
        for (FeeInstallment installment : installments) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal outstanding = installment.getAmountDue().subtract(installment.getAmountPaid());
            if (outstanding.signum() <= 0) {
                continue;
            }
            BigDecimal toPay = outstanding.min(remaining);
            installment.setAmountPaid(installment.getAmountPaid().add(toPay));
            installment.setStatus(statusOf(installment));
            installmentRepository.save(installment);
            remaining = remaining.subtract(toPay);
        }
    }

    private boolean isStudentsInstallment(FeeInstallment installment, UUID studentId) {
        return assignmentRepository.findByIdAndSchoolId(installment.getStudentFeeAssignmentId(),
                        SecurityUtils.currentSchoolId())
                .map(a -> a.getStudentId().equals(studentId)).orElse(false);
    }

    private InstallmentStatus statusOf(FeeInstallment installment) {
        if (installment.getAmountPaid().signum() == 0) {
            return InstallmentStatus.PENDING;
        }
        if (installment.getAmountPaid().compareTo(installment.getAmountDue()) >= 0) {
            return InstallmentStatus.PAID;
        }
        return InstallmentStatus.PARTIAL;
    }

    // ---- Receipt -----------------------------------------------------------------
    private String generateReceipt(FeePayment payment, Student student) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        School school = schoolRepository.findById(schoolId).orElse(null);
        String className = classNameFor(student.getId(), schoolId);

        Map<String, Object> fields = new HashMap<>();
        fields.put("schoolName", school == null ? "" : school.getName());
        fields.put("schoolAddress", school == null ? "" : school.getAddress());
        fields.put("receiptNo", payment.getReceiptNo());
        fields.put("paidAt", payment.getPaidAt().toString());
        fields.put("studentName", student.getDisplayName());
        fields.put("admissionNo", student.getAdmissionNo());
        fields.put("className", className == null ? "" : className);
        fields.put("categoryName", categoryNameFor(payment));
        fields.put("amount", payment.getAmountPaid().toPlainString());
        fields.put("paymentMethod", payment.getPaymentMethod().name());
        fields.put("referenceNo", payment.getReferenceNo() == null ? "" : payment.getReferenceNo());
        fields.put("remarks", payment.getRemarks() == null ? "" : payment.getRemarks());
        return feePdfService.generateReceipt(schoolId, fields);
    }

    private String classNameFor(UUID studentId, UUID schoolId) {
        return academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .flatMap(year -> enrollmentRepository
                        .findByStudentIdAndAcademicYearId(studentId, year.getId()))
                .flatMap(enrollment -> sectionRepository.findByIdAndSchoolId(enrollment.getSectionId(), schoolId))
                .flatMap(section -> classRepository.findByIdAndSchoolId(section.getClassId(), schoolId))
                .map(SchoolClass::getName)
                .orElse(null);
    }

    private String categoryNameFor(FeePayment payment) {
        if (payment.getStudentFeeAssignmentId() == null) {
            return "";
        }
        return assignmentRepository.findByIdAndSchoolId(payment.getStudentFeeAssignmentId(),
                        SecurityUtils.currentSchoolId())
                .flatMap(a -> structureRepository.findByIdAndSchoolId(a.getFeeStructureId(),
                        SecurityUtils.currentSchoolId()))
                .flatMap(s -> categoryRepository.findByIdAndSchoolId(s.getCategoryId(),
                        SecurityUtils.currentSchoolId()))
                .map(FeeCategory::getName)
                .orElse("");
    }

    private String generateReceiptNo(UUID schoolId) {
        String base = "RCP-" + java.time.LocalDate.now().getYear() + "-";
        long seq = paymentRepository.count();
        return base + String.format("%06d", seq + 1);
    }

    // ---- Mapping helpers ------------------------------------------------------------
    private FeeStructureDto toStructureDto(FeeStructure structure, UUID schoolId) {
        String className = classRepository.findByIdAndSchoolId(structure.getClassId(), schoolId)
                .map(SchoolClass::getName).orElse(null);
        String categoryName = categoryRepository.findByIdAndSchoolId(structure.getCategoryId(), schoolId)
                .map(FeeCategory::getName).orElse(null);
        return FeeStructureDto.from(structure, className, categoryName);
    }

    private FeeAssignmentDto toAssignmentDto(StudentFeeAssignment assignment, UUID schoolId) {
        Student student = studentRepository.findByIdAndSchoolId(assignment.getStudentId(), schoolId).orElse(null);
        FeeStructure structure = structureRepository.findByIdAndSchoolId(assignment.getFeeStructureId(), schoolId)
                .orElse(null);
        String structureName = structure == null ? null
                : categoryRepository.findByIdAndSchoolId(structure.getCategoryId(), schoolId)
                        .map(FeeCategory::getName).orElse(null);
        return new FeeAssignmentDto(
                assignment.getId(),
                assignment.getStudentId(),
                student == null ? null : student.getDisplayName(),
                student == null ? null : student.getAdmissionNo(),
                assignment.getFeeStructureId(),
                structureName,
                structure == null ? null : structure.getAmount(),
                structure == null ? null : structure.getFrequency().name(),
                assignment.getDiscountType(),
                assignment.getDiscountAmount(),
                assignment.getStatus());
    }

    private FeeFrequency parseFrequency(String value) {
        if (value == null || value.isBlank()) {
            return FeeFrequency.MONTHLY;
        }
        try {
            return FeeFrequency.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("validation.invalid");
        }
    }

    private PaymentMethod parseMethod(String value) {
        if (value == null || value.isBlank()) {
            return PaymentMethod.CASH;
        }
        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("validation.invalid");
        }
    }
}
