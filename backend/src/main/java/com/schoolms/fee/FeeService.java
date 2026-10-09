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
import com.schoolms.fee.dto.AdhocFeeLevyDto;
import com.schoolms.fee.dto.AdhocFeeLevyRequest;
import com.schoolms.fee.dto.CloneFeeStructureRequest;
import com.schoolms.fee.dto.CloneFeeStructureResult;
import com.schoolms.fee.dto.FeeAssignmentDto;
import com.schoolms.fee.dto.FeeAssignmentRequest;
import com.schoolms.fee.dto.FeeCategoryDto;
import com.schoolms.fee.dto.FeeCategoryRequest;
import com.schoolms.fee.dto.FeePaymentDto;
import com.schoolms.fee.dto.FeeStructureAuditDto;
import com.schoolms.fee.dto.FeeStructureDto;
import com.schoolms.fee.dto.FeeStructureRequest;
import com.schoolms.fee.dto.FeeStructureUpdateRequest;
import com.schoolms.fee.dto.InstallmentDto;
import com.schoolms.fee.dto.PaymentRequest;
import com.schoolms.fee.dto.SiblingDiscountRuleDto;
import com.schoolms.fee.dto.SiblingDiscountRuleRequest;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.security.SecurityUtils;
import com.schoolms.student.Student;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final SiblingDiscountRuleRepository siblingRuleRepository;
    private final AdhocFeeLevyRepository levyRepository;
    private final FeeStructureAuditRepository auditRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final UserRepository userRepository;

    // ---- Categories --------------------------------------------------------
    @Transactional(readOnly = true)
    public List<FeeCategoryDto> listCategories() {
        return categoryRepository.findBySchoolIdOrderByNameAsc(SecurityUtils.currentSchoolId())
                .stream().map(FeeCategoryDto::from).toList();
    }

    @Transactional
    public FeeCategoryDto createCategory(FeeCategoryRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        String code = request.code().trim().toUpperCase();
        if (categoryRepository.existsBySchoolIdAndCode(schoolId, code)) {
            throw new BusinessException("error.conflict");
        }
        FeeCategory category = new FeeCategory();
        category.setSchoolId(schoolId);
        applyCategory(category, request, code);
        return FeeCategoryDto.from(categoryRepository.save(category));
    }

    @Transactional
    public FeeCategoryDto updateCategory(UUID id, FeeCategoryRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        FeeCategory category = categoryRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("fee_category", id));
        String code = request.code().trim().toUpperCase();
        if (categoryRepository.existsBySchoolIdAndCodeAndIdNot(schoolId, code, id)) {
            throw new BusinessException("error.conflict");
        }
        applyCategory(category, request, code);
        return FeeCategoryDto.from(categoryRepository.save(category));
    }

    @Transactional
    public FeeCategoryDto deactivateCategory(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        FeeCategory category = categoryRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("fee_category", id));
        category.setStatus("INACTIVE");
        return FeeCategoryDto.from(categoryRepository.save(category));
    }

    private void applyCategory(FeeCategory category, FeeCategoryRequest request, String code) {
        category.setName(request.name().trim());
        category.setCode(code);
        category.setDescription(request.description());
        category.setOptional(Boolean.TRUE.equals(request.optional()));
        category.setRefundable(Boolean.TRUE.equals(request.refundable()));
        category.setFrequency(parseFrequency(request.frequency()));
        String status = request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status().trim().toUpperCase();
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            throw new BusinessException("validation.invalid");
        }
        category.setStatus(status);
    }

    // ---- Structures ----------------------------------------------------------
    @Transactional(readOnly = true)
    public List<FeeStructureDto> listStructures(UUID academicYearId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Map<UUID, String> classNames = classNames(schoolId);
        Map<UUID, String> categoryNames = categoryNames(schoolId);
        return structureRepository.findBySchoolIdAndAcademicYearIdOrderByClassIdAsc(schoolId, academicYearId)
                .stream().map(s -> FeeStructureDto.from(s,
                        classNames.get(s.getClassId()), categoryNames.get(s.getCategoryId())))
                .toList();
    }

    @Transactional
    public List<FeeStructureDto> createStructure(FeeStructureRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        academicYearRepository.findByIdAndSchoolId(request.academicYearId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", request.academicYearId()));
        FeeCategory category = categoryRepository.findByIdAndSchoolId(request.categoryId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("fee_category", request.categoryId()));
        if (!"ACTIVE".equals(category.getStatus())) {
            throw new BusinessException("fee.category_inactive");
        }
        List<SchoolClass> classes = resolveTargetClasses(schoolId, request.classId(), request.applyToAllClasses());
        FeeFrequency frequency = parseFrequency(request.frequency());
        List<FeeStructureDto> created = new ArrayList<>();
        for (SchoolClass schoolClass : classes) {
            if (structureRepository.existsBySchoolIdAndClassIdAndAcademicYearIdAndCategoryId(
                    schoolId, schoolClass.getId(), request.academicYearId(), category.getId())) {
                if (Boolean.TRUE.equals(request.applyToAllClasses())) {
                    continue;
                }
                throw new BusinessException("fee.structure_exists");
            }
            FeeStructure structure = new FeeStructure();
            structure.setSchoolId(schoolId);
            structure.setClassId(schoolClass.getId());
            structure.setAcademicYearId(request.academicYearId());
            structure.setCategoryId(category.getId());
            structure.setAmount(request.amount());
            structure.setFrequency(frequency);
            structure.setDueDay(request.dueDay());
            structure.setDueDate(request.dueDate());
            structure.setApplicableFrom(request.applicableFrom());
            structure.setApplicableTo(request.applicableTo());
            structure = structureRepository.save(structure);
            recordAudit(structure, "CREATE", null, null, null);
            created.add(toStructureDto(structure, schoolId));
        }
        if (created.isEmpty()) {
            throw new BusinessException("fee.structure_exists");
        }
        return created;
    }

    @Transactional
    public FeeStructureDto updateStructure(UUID id, FeeStructureUpdateRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        FeeStructure structure = structureRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("fee_structure", id));
        BigDecimal previousAmount = structure.getAmount();
        String previousFrequency = structure.getFrequency().name();
        structure.setAmount(request.amount());
        structure.setFrequency(parseFrequency(request.frequency()));
        structure.setDueDay(request.dueDay());
        structure.setDueDate(request.dueDate());
        structure.setApplicableFrom(request.applicableFrom());
        structure.setApplicableTo(request.applicableTo());
        structure = structureRepository.save(structure);
        recordAudit(structure, "UPDATE", previousAmount, previousFrequency, structure.getFrequency().name());
        return toStructureDto(structure, schoolId);
    }

    @Transactional
    public CloneFeeStructureResult cloneStructures(CloneFeeStructureRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        if (request.sourceAcademicYearId().equals(request.targetAcademicYearId())) {
            throw new BusinessException("fee.clone_same_year");
        }
        academicYearRepository.findByIdAndSchoolId(request.sourceAcademicYearId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", request.sourceAcademicYearId()));
        academicYearRepository.findByIdAndSchoolId(request.targetAcademicYearId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", request.targetAcademicYearId()));
        List<FeeStructure> source = structureRepository.findBySchoolIdAndAcademicYearIdOrderByClassIdAsc(
                schoolId, request.sourceAcademicYearId());
        int cloned = 0;
        int skipped = 0;
        for (FeeStructure row : source) {
            if (structureRepository.existsBySchoolIdAndClassIdAndAcademicYearIdAndCategoryId(
                    schoolId, row.getClassId(), request.targetAcademicYearId(), row.getCategoryId())) {
                skipped++;
                continue;
            }
            FeeStructure copy = new FeeStructure();
            copy.setSchoolId(schoolId);
            copy.setClassId(row.getClassId());
            copy.setAcademicYearId(request.targetAcademicYearId());
            copy.setCategoryId(row.getCategoryId());
            copy.setAmount(row.getAmount());
            copy.setFrequency(row.getFrequency());
            copy.setDueDay(row.getDueDay());
            copy.setDueDate(shiftDueDate(row.getDueDate(), request.sourceAcademicYearId(), request.targetAcademicYearId(), schoolId));
            copy.setApplicableFrom(row.getApplicableFrom());
            copy.setApplicableTo(row.getApplicableTo());
            copy = structureRepository.save(copy);
            recordAudit(copy, "CLONE", null, null, copy.getFrequency().name());
            cloned++;
        }
        return new CloneFeeStructureResult(cloned, skipped);
    }

    @Transactional(readOnly = true)
    public List<FeeStructureAuditDto> listAudit(UUID academicYearId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<FeeStructureAudit> rows = academicYearId == null
                ? auditRepository.findBySchoolIdOrderByChangedAtDesc(schoolId)
                : auditRepository.findBySchoolIdAndAcademicYearIdOrderByChangedAtDesc(schoolId, academicYearId);
        Map<UUID, String> classNames = classNames(schoolId);
        Map<UUID, String> categoryNames = categoryNames(schoolId);
        Map<UUID, String> yearNames = academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId).stream()
                .collect(Collectors.toMap(AcademicYear::getId, AcademicYear::getName));
        Map<UUID, String> userNames = new HashMap<>();
        return rows.stream().map(row -> FeeStructureAuditDto.from(
                row,
                yearNames.get(row.getAcademicYearId()),
                classNames.get(row.getClassId()),
                categoryNames.get(row.getCategoryId()),
                userName(row.getChangedBy(), userNames))).toList();
    }

    // ---- Sibling discounts ---------------------------------------------------
    @Transactional(readOnly = true)
    public List<SiblingDiscountRuleDto> listSiblingRules(UUID academicYearId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<SiblingDiscountRule> rules = academicYearId == null
                ? siblingRuleRepository.findBySchoolIdOrderBySiblingOrderAsc(schoolId)
                : siblingRuleRepository.findBySchoolIdAndAcademicYearIdOrderBySiblingOrderAsc(schoolId, academicYearId);
        Map<UUID, String> categoryNames = categoryNames(schoolId);
        Map<UUID, String> yearNames = academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId).stream()
                .collect(Collectors.toMap(AcademicYear::getId, AcademicYear::getName));
        return rules.stream()
                .map(r -> SiblingDiscountRuleDto.from(r, yearNames.get(r.getAcademicYearId()), categoryNames.get(r.getFeeCategoryId())))
                .toList();
    }

    @Transactional
    public SiblingDiscountRuleDto createSiblingRule(SiblingDiscountRuleRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        academicYearRepository.findByIdAndSchoolId(request.academicYearId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", request.academicYearId()));
        FeeCategory category = categoryRepository.findByIdAndSchoolId(request.feeCategoryId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("fee_category", request.feeCategoryId()));
        String type = parseDiscountType(request.discountType());
        if (siblingRuleRepository.existsBySchoolIdAndAcademicYearIdAndSiblingOrderAndFeeCategoryId(
                schoolId, request.academicYearId(), request.siblingOrder(), category.getId())) {
            throw new BusinessException("error.conflict");
        }
        SiblingDiscountRule rule = new SiblingDiscountRule();
        rule.setSchoolId(schoolId);
        rule.setAcademicYearId(request.academicYearId());
        rule.setSiblingOrder(request.siblingOrder());
        rule.setDiscountType(type);
        rule.setDiscountValue(request.discountValue());
        rule.setFeeCategoryId(category.getId());
        rule.setStatus(parseStatus(request.status()));
        rule = siblingRuleRepository.save(rule);
        return SiblingDiscountRuleDto.from(rule, yearName(rule.getAcademicYearId(), schoolId), category.getName());
    }

    @Transactional
    public SiblingDiscountRuleDto updateSiblingRule(UUID id, SiblingDiscountRuleRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SiblingDiscountRule rule = siblingRuleRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("sibling_discount_rule", id));
        FeeCategory category = categoryRepository.findByIdAndSchoolId(request.feeCategoryId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("fee_category", request.feeCategoryId()));
        if (siblingRuleRepository.existsBySchoolIdAndAcademicYearIdAndSiblingOrderAndFeeCategoryIdAndIdNot(
                schoolId, request.academicYearId(), request.siblingOrder(), category.getId(), id)) {
            throw new BusinessException("error.conflict");
        }
        rule.setAcademicYearId(request.academicYearId());
        rule.setSiblingOrder(request.siblingOrder());
        rule.setDiscountType(parseDiscountType(request.discountType()));
        rule.setDiscountValue(request.discountValue());
        rule.setFeeCategoryId(category.getId());
        rule.setStatus(parseStatus(request.status()));
        rule = siblingRuleRepository.save(rule);
        return SiblingDiscountRuleDto.from(rule, yearName(rule.getAcademicYearId(), schoolId), category.getName());
    }

    // ---- Ad-hoc levies -------------------------------------------------------
    @Transactional(readOnly = true)
    public List<AdhocFeeLevyDto> listLevies(UUID academicYearId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        AcademicYear year = resolveYear(academicYearId, schoolId);
        return levyRepository.findBySchoolIdAndAcademicYearIdOrderByCreatedAtDesc(schoolId, year.getId()).stream()
                .map(levy -> toLevyDto(levy, schoolId, year.getName()))
                .toList();
    }

    @Transactional
    public AdhocFeeLevyDto createLevy(AdhocFeeLevyRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        AcademicYear year = academicYearRepository.findByIdAndSchoolId(request.academicYearId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", request.academicYearId()));
        FeeCategory category = categoryRepository.findByIdAndSchoolId(request.feeCategoryId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("fee_category", request.feeCategoryId()));
        String scope = request.scope().trim().toUpperCase();
        if (!Set.of("GLOBAL", "CLASS", "SECTION", "STUDENT").contains(scope)) {
            throw new BusinessException("validation.invalid");
        }
        AdhocFeeLevy levy = new AdhocFeeLevy();
        levy.setSchoolId(schoolId);
        levy.setAcademicYearId(year.getId());
        levy.setFeeCategoryId(category.getId());
        levy.setAmount(request.amount());
        levy.setDueDate(request.dueDate());
        levy.setScope(scope);
        levy.setRemarks(request.remarks());
        levy.setCreatedBy(SecurityUtils.currentUserId());
        applyScope(levy, scope, request, schoolId);
        List<UUID> studentIds = resolveLevyStudents(levy, schoolId);
        levy.setAssignedCount(studentIds.size());
        levy = levyRepository.save(levy);

        for (UUID studentId : studentIds) {
            FeeStructure structure = structureForLevy(levy, studentId, schoolId, category);
            if (assignmentRepository.existsByStudentIdAndFeeStructureId(studentId, structure.getId())) {
                continue;
            }
            StudentFeeAssignment assignment = new StudentFeeAssignment();
            assignment.setSchoolId(schoolId);
            assignment.setStudentId(studentId);
            assignment.setFeeStructureId(structure.getId());
            assignment.setStatus("ACTIVE");
            assignment = assignmentRepository.save(assignment);
            generateInstallments(assignment, structure);
        }
        return toLevyDto(levy, schoolId, year.getName());
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

        String discountType = request.discountType();
        BigDecimal discountAmount = request.discountAmount();
        if ((discountType == null || discountType.isBlank()) && discountAmount == null) {
            Optional<SiblingDiscount> sibling = resolveSiblingDiscount(student.getId(), structure, schoolId);
            if (sibling.isPresent()) {
                discountType = sibling.get().type();
                discountAmount = sibling.get().value();
            }
        }

        StudentFeeAssignment assignment = new StudentFeeAssignment();
        assignment.setSchoolId(schoolId);
        assignment.setStudentId(student.getId());
        assignment.setFeeStructureId(structure.getId());
        assignment.setDiscountType(discountType);
        assignment.setDiscountAmount(discountAmount);
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
        List<LocalDate> dueDates = dueDates(structure, year);
        BigDecimal amount = effectiveAmount(assignment, structure);
        for (LocalDate dueDate : dueDates) {
            FeeInstallment installment = new FeeInstallment();
            installment.setSchoolId(SecurityUtils.currentSchoolId());
            installment.setStudentFeeAssignmentId(assignment.getId());
            installment.setDueDate(dueDate);
            installment.setAmountDue(amount);
            installment.setAmountPaid(BigDecimal.ZERO);
            installment.setStatus(InstallmentStatus.PENDING);
            installmentRepository.save(installment);
        }
    }

    private List<LocalDate> dueDates(FeeStructure structure, AcademicYear year) {
        if (structure.getFrequency() == FeeFrequency.ONE_TIME) {
            LocalDate due = structure.getDueDate() != null ? structure.getDueDate() : year.getStartDate();
            return List.of(due);
        }
        List<YearMonth> periods = monthlyPeriods(year.getStartDate(), year.getEndDate());
        int step = switch (structure.getFrequency()) {
            case MONTHLY -> 1;
            case QUARTERLY -> 3;
            case HALF_YEARLY -> 6;
            case ANNUAL -> periods.isEmpty() ? 1 : periods.size();
            default -> 1;
        };
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < periods.size(); i += step) {
            YearMonth period = periods.get(i);
            int dueDay = structure.getDueDay() == null ? 1 : structure.getDueDay();
            dates.add(period.atDay(Math.min(dueDay, period.lengthOfMonth())));
        }
        return dates;
    }

    private List<YearMonth> monthlyPeriods(LocalDate from, LocalDate to) {
        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        List<YearMonth> periods = new ArrayList<>();
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            periods.add(ym);
        }
        return periods;
    }

    private BigDecimal effectiveAmount(StudentFeeAssignment assignment, FeeStructure structure) {
        BigDecimal amount = structure.getAmount();
        if ("PERCENT".equalsIgnoreCase(assignment.getDiscountType()) && assignment.getDiscountAmount() != null) {
            BigDecimal discount = amount.multiply(assignment.getDiscountAmount())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            amount = amount.subtract(discount);
        } else if (assignment.getDiscountAmount() != null) {
            amount = amount.subtract(assignment.getDiscountAmount());
        }
        return amount.max(BigDecimal.ZERO);
    }

    Optional<SiblingDiscount> resolveSiblingDiscount(UUID studentId, FeeStructure structure, UUID schoolId) {
        List<UUID> family = studentGuardianRepository.findSiblingStudentIds(schoolId, studentId);
        if (family.size() < 2) {
            return Optional.empty();
        }
        List<Student> members = new ArrayList<>(studentRepository.findBySchoolIdAndIdIn(schoolId, family));
        members.sort(Comparator.comparing(Student::getDateOfBirth, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Student::getAdmissionDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Student::getAdmissionNo, Comparator.nullsLast(String::compareTo)));
        int index = -1;
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getId().equals(studentId)) {
                index = i;
                break;
            }
        }
        if (index < 1) {
            return Optional.empty();
        }
        int order = index + 1;
        List<SiblingDiscountRule> rules = siblingRuleRepository
                .findBySchoolIdAndAcademicYearIdAndStatusOrderBySiblingOrderAsc(
                        schoolId, structure.getAcademicYearId(), "ACTIVE");
        SiblingDiscountRule matched = null;
        for (SiblingDiscountRule rule : rules) {
            if (!rule.getFeeCategoryId().equals(structure.getCategoryId())) {
                continue;
            }
            if (rule.getSiblingOrder() <= order && (matched == null || rule.getSiblingOrder() > matched.getSiblingOrder())) {
                matched = rule;
            }
        }
        if (matched == null) {
            return Optional.empty();
        }
        return Optional.of(new SiblingDiscount(matched.getDiscountType(), matched.getDiscountValue()));
    }

    record SiblingDiscount(String type, BigDecimal value) {
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
                    .sorted(Comparator.comparing(FeeInstallment::getDueDate))
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

    private AdhocFeeLevyDto toLevyDto(AdhocFeeLevy levy, UUID schoolId, String yearName) {
        String categoryName = categoryRepository.findByIdAndSchoolId(levy.getFeeCategoryId(), schoolId)
                .map(FeeCategory::getName).orElse(null);
        String className = levy.getClassId() == null ? null
                : classRepository.findByIdAndSchoolId(levy.getClassId(), schoolId).map(SchoolClass::getName).orElse(null);
        String sectionName = levy.getSectionId() == null ? null
                : sectionRepository.findByIdAndSchoolId(levy.getSectionId(), schoolId).map(Section::getName).orElse(null);
        String studentName = levy.getStudentId() == null ? null
                : studentRepository.findByIdAndSchoolId(levy.getStudentId(), schoolId).map(Student::getDisplayName).orElse(null);
        return AdhocFeeLevyDto.from(levy, yearName, categoryName, className, sectionName, studentName);
    }

    private void recordAudit(FeeStructure structure, String action, BigDecimal previousAmount,
                             String previousFrequency, String newFrequency) {
        FeeStructureAudit audit = new FeeStructureAudit();
        audit.setSchoolId(structure.getSchoolId());
        audit.setFeeStructureId(structure.getId());
        audit.setAcademicYearId(structure.getAcademicYearId());
        audit.setClassId(structure.getClassId());
        audit.setCategoryId(structure.getCategoryId());
        audit.setAction(action);
        audit.setPreviousAmount(previousAmount);
        audit.setNewAmount(structure.getAmount());
        audit.setPreviousFrequency(previousFrequency);
        audit.setNewFrequency(newFrequency == null ? structure.getFrequency().name() : newFrequency);
        audit.setChangedBy(SecurityUtils.currentUserId());
        auditRepository.save(audit);
    }

    private List<SchoolClass> resolveTargetClasses(UUID schoolId, UUID classId, Boolean applyToAll) {
        if (Boolean.TRUE.equals(applyToAll)) {
            List<SchoolClass> classes = classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId);
            if (classes.isEmpty()) {
                throw new BusinessException("fee.no_classes");
            }
            return classes;
        }
        if (classId == null) {
            throw new BusinessException("validation.not_null");
        }
        SchoolClass schoolClass = classRepository.findByIdAndSchoolId(classId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("class", classId));
        return List.of(schoolClass);
    }

    private void applyScope(AdhocFeeLevy levy, String scope, AdhocFeeLevyRequest request, UUID schoolId) {
        switch (scope) {
            case "GLOBAL" -> {
                levy.setClassId(null);
                levy.setSectionId(null);
                levy.setStudentId(null);
            }
            case "CLASS" -> {
                if (request.classId() == null) {
                    throw new BusinessException("validation.not_null");
                }
                classRepository.findByIdAndSchoolId(request.classId(), schoolId)
                        .orElseThrow(() -> ResourceNotFoundException.of("class", request.classId()));
                levy.setClassId(request.classId());
                levy.setSectionId(null);
                levy.setStudentId(null);
            }
            case "SECTION" -> {
                if (request.sectionId() == null) {
                    throw new BusinessException("validation.not_null");
                }
                Section section = sectionRepository.findByIdAndSchoolId(request.sectionId(), schoolId)
                        .orElseThrow(() -> ResourceNotFoundException.of("section", request.sectionId()));
                levy.setSectionId(section.getId());
                levy.setClassId(section.getClassId());
                levy.setStudentId(null);
            }
            case "STUDENT" -> {
                if (request.studentId() == null) {
                    throw new BusinessException("validation.not_null");
                }
                studentRepository.findByIdAndSchoolId(request.studentId(), schoolId)
                        .orElseThrow(() -> ResourceNotFoundException.of("student", request.studentId()));
                levy.setStudentId(request.studentId());
                levy.setClassId(request.classId());
                levy.setSectionId(request.sectionId());
            }
            default -> throw new BusinessException("validation.invalid");
        }
    }

    private List<UUID> resolveLevyStudents(AdhocFeeLevy levy, UUID schoolId) {
        return switch (levy.getScope()) {
            case "STUDENT" -> List.of(levy.getStudentId());
            case "SECTION" -> enrollmentRepository
                    .findBySchoolIdAndAcademicYearIdAndStatusAndSectionId(
                            schoolId, levy.getAcademicYearId(), "ACTIVE", levy.getSectionId())
                    .stream().map(StudentEnrollment::getStudentId).toList();
            case "CLASS" -> {
                List<UUID> sectionIds = sectionRepository.findBySchoolIdAndClassIdOrderByNameAsc(schoolId, levy.getClassId())
                        .stream().map(Section::getId).toList();
                if (sectionIds.isEmpty()) {
                    yield List.of();
                }
                yield enrollmentRepository.findBySchoolIdAndAcademicYearIdAndStatusAndSectionIdIn(
                                schoolId, levy.getAcademicYearId(), "ACTIVE", sectionIds)
                        .stream().map(StudentEnrollment::getStudentId).toList();
            }
            case "GLOBAL" -> enrollmentRepository
                    .findBySchoolIdAndAcademicYearIdAndStatus(schoolId, levy.getAcademicYearId(), "ACTIVE")
                    .stream().map(StudentEnrollment::getStudentId).toList();
            default -> List.of();
        };
    }

    private FeeStructure structureForLevy(AdhocFeeLevy levy, UUID studentId, UUID schoolId, FeeCategory category) {
        UUID classId = levy.getClassId();
        if (classId == null) {
            classId = enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, levy.getAcademicYearId())
                    .flatMap(enrollment -> sectionRepository.findByIdAndSchoolId(enrollment.getSectionId(), schoolId))
                    .map(Section::getClassId)
                    .orElse(null);
        }
        if (classId == null) {
            throw new BusinessException("fee.no_enrollment");
        }
        UUID resolvedClassId = classId;
        return structureRepository.findBySchoolIdAndAcademicYearIdAndClassId(schoolId, levy.getAcademicYearId(), resolvedClassId)
                .stream()
                .filter(s -> s.getCategoryId().equals(category.getId()) && s.getAmount().compareTo(levy.getAmount()) == 0)
                .findFirst()
                .orElseGet(() -> {
                    FeeStructure structure = new FeeStructure();
                    structure.setSchoolId(schoolId);
                    structure.setClassId(resolvedClassId);
                    structure.setAcademicYearId(levy.getAcademicYearId());
                    structure.setCategoryId(category.getId());
                    structure.setAmount(levy.getAmount());
                    structure.setFrequency(FeeFrequency.ONE_TIME);
                    structure.setDueDate(levy.getDueDate());
                    structure = structureRepository.save(structure);
                    recordAudit(structure, "CREATE", null, null, "ONE_TIME");
                    return structure;
                });
    }

    private LocalDate shiftDueDate(LocalDate dueDate, UUID sourceYearId, UUID targetYearId, UUID schoolId) {
        if (dueDate == null) {
            return null;
        }
        AcademicYear source = academicYearRepository.findByIdAndSchoolId(sourceYearId, schoolId).orElse(null);
        AcademicYear target = academicYearRepository.findByIdAndSchoolId(targetYearId, schoolId).orElse(null);
        if (source == null || target == null) {
            return dueDate;
        }
        return dueDate.plusYears(target.getStartDate().getYear() - source.getStartDate().getYear());
    }

    private AcademicYear resolveYear(UUID academicYearId, UUID schoolId) {
        if (academicYearId != null) {
            return academicYearRepository.findByIdAndSchoolId(academicYearId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("academic_year", academicYearId));
        }
        return academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .orElseThrow(() -> new BusinessException("academic_year.not_found"));
    }

    private Map<UUID, String> classNames(UUID schoolId) {
        return classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId).stream()
                .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName));
    }

    private Map<UUID, String> categoryNames(UUID schoolId) {
        return categoryRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .collect(Collectors.toMap(FeeCategory::getId, FeeCategory::getName));
    }

    private String yearName(UUID yearId, UUID schoolId) {
        return academicYearRepository.findByIdAndSchoolId(yearId, schoolId).map(AcademicYear::getName).orElse(null);
    }

    private String userName(UUID userId, Map<UUID, String> cache) {
        if (userId == null) {
            return null;
        }
        return cache.computeIfAbsent(userId, id -> userRepository.findById(id).map(User::getDisplayName).orElse(null));
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

    private String parseDiscountType(String value) {
        if (value == null) {
            throw new BusinessException("validation.invalid");
        }
        String type = value.trim().toUpperCase();
        if (!type.equals("FIXED") && !type.equals("PERCENT")) {
            throw new BusinessException("validation.invalid");
        }
        return type;
    }

    private String parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return "ACTIVE";
        }
        String status = value.trim().toUpperCase();
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            throw new BusinessException("validation.invalid");
        }
        return status;
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
