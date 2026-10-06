package com.schoolms.payroll;

import com.schoolms.academics.TeacherProfile;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.common.enums.PayrollRunStatus;
import com.schoolms.common.enums.StaffType;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.RateLimitException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.payroll.dto.PayrollRunDto;
import com.schoolms.payroll.dto.PayrollRunRequest;
import com.schoolms.payroll.dto.PayslipDto;
import com.schoolms.payroll.dto.SalaryLineItemDto;
import com.schoolms.payroll.dto.SalaryStructureDto;
import com.schoolms.payroll.dto.SalaryStructureRequest;
import com.schoolms.security.SecurityUtils;
import com.schoolms.staff.NonTeachingStaff;
import com.schoolms.staff.NonTeachingStaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private static final int PDF_LIMIT_PER_MINUTE = 10;

    private final StaffSalaryStructureRepository salaryRepository;
    private final PayrollRunRepository runRepository;
    private final PayslipRepository payslipRepository;
    private final TeacherProfileRepository teacherRepository;
    private final NonTeachingStaffRepository nonTeachingStaffRepository;
    private final PayslipPdfService payslipPdfService;

    private final ConcurrentHashMap<UUID, List<Long>> pdfHits = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public List<SalaryStructureDto> listSalaryStructures() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        return salaryRepository.findBySchoolIdOrderByEffectiveFromDesc(schoolId).stream()
                .map(this::toSalaryDto)
                .toList();
    }

    @Transactional
    public SalaryStructureDto upsertSalaryStructure(SalaryStructureRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        resolveStaff(schoolId, request.staffType(), request.staffId());
        StaffSalaryStructure row = new StaffSalaryStructure();
        row.setSchoolId(schoolId);
        row.setStaffType(request.staffType());
        row.setStaffId(request.staffId());
        row.setBasic(SalaryJson.money(request.basic()));
        row.setHra(SalaryJson.money(request.hra()));
        row.setAllowancesJson(SalaryJson.toJson(request.allowances()));
        row.setDeductionsJson(SalaryJson.toJson(request.deductions()));
        row.setEffectiveFrom(request.effectiveFrom());
        row.setEffectiveTo(request.effectiveTo());
        row.setStatus("ACTIVE");
        return toSalaryDto(salaryRepository.save(row));
    }

    @Transactional(readOnly = true)
    public List<PayrollRunDto> listRuns() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        return runRepository.findBySchoolIdOrderByYearDescMonthDesc(schoolId).stream()
                .map(this::toRunDto)
                .toList();
    }

    @Transactional
    public PayrollRunDto createRun(PayrollRunRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        short year = request.year().shortValue();
        short month = request.month().shortValue();
        if (runRepository.existsBySchoolIdAndYearAndMonth(schoolId, year, month)) {
            throw new BusinessException("payroll.period_exists");
        }
        PayrollRun run = new PayrollRun();
        run.setSchoolId(schoolId);
        run.setYear(year);
        run.setMonth(month);
        run.setStatus(PayrollRunStatus.DRAFT);
        run.setNotes(request.notes());
        return toRunDto(runRepository.save(run));
    }

    @Transactional
    public PayrollRunDto process(UUID runId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        PayrollRun run = runRepository.findByIdAndSchoolId(runId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("payroll_run", runId));
        if (run.getStatus() != PayrollRunStatus.DRAFT) {
            throw new BusinessException("payroll.invalid_status");
        }
        if (payslipRepository.existsByPayrollRunId(run.getId())) {
            throw new BusinessException("payroll.already_processed");
        }

        YearMonth period = YearMonth.of(run.getYear(), run.getMonth());
        LocalDate asOf = period.atEndOfMonth();
        List<Payslip> slips = new ArrayList<>();
        for (TeacherProfile teacher : teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId)) {
            if (!"ACTIVE".equals(teacher.getStatus())) {
                continue;
            }
            salaryRepository.findFirstBySchoolIdAndStaffTypeAndStaffIdAndStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                            schoolId, StaffType.TEACHING, teacher.getId(), "ACTIVE", asOf)
                    .filter(structure -> structure.getEffectiveTo() == null || !structure.getEffectiveTo().isBefore(period.atDay(1)))
                    .ifPresent(structure -> slips.add(buildPayslip(run, StaffType.TEACHING, teacher.getId(),
                            teacher.getUserId(), teacher.getEmployeeNo(), teacher.getDisplayName(),
                            teacher.getDesignation(), teacher.getDepartment(), structure)));
        }
        for (NonTeachingStaff staff : nonTeachingStaffRepository.findBySchoolIdOrderByFirstNameAsc(schoolId)) {
            if (!"ACTIVE".equals(staff.getStatus())) {
                continue;
            }
            salaryRepository.findFirstBySchoolIdAndStaffTypeAndStaffIdAndStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                            schoolId, StaffType.NON_TEACHING, staff.getId(), "ACTIVE", asOf)
                    .filter(structure -> structure.getEffectiveTo() == null || !structure.getEffectiveTo().isBefore(period.atDay(1)))
                    .ifPresent(structure -> slips.add(buildPayslip(run, StaffType.NON_TEACHING, staff.getId(),
                            staff.getUserId(), staff.getEmployeeNo(), staff.getDisplayName(),
                            staff.getDesignation(), staff.getDepartment(), structure)));
        }
        if (slips.isEmpty()) {
            throw new BusinessException("payroll.no_salaries");
        }
        payslipRepository.saveAll(slips);
        run.setStatus(PayrollRunStatus.PROCESSED);
        run.setProcessedAt(Instant.now());
        run.setProcessedBy(SecurityUtils.currentUserId());
        return toRunDto(runRepository.save(run));
    }

    @Transactional
    public PayrollRunDto publish(UUID runId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        PayrollRun run = runRepository.findByIdAndSchoolId(runId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("payroll_run", runId));
        if (run.getStatus() != PayrollRunStatus.PROCESSED) {
            throw new BusinessException("payroll.not_processed");
        }
        run.setStatus(PayrollRunStatus.PUBLISHED);
        run.setPublishedAt(Instant.now());
        run.setPublishedBy(SecurityUtils.currentUserId());
        return toRunDto(runRepository.save(run));
    }

    @Transactional
    public PayrollRunDto markPaid(UUID runId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        PayrollRun run = runRepository.findByIdAndSchoolId(runId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("payroll_run", runId));
        if (run.getStatus() != PayrollRunStatus.PUBLISHED) {
            throw new BusinessException("payroll.not_published");
        }
        run.setStatus(PayrollRunStatus.PAID);
        run.setPaidAt(Instant.now());
        return toRunDto(runRepository.save(run));
    }

    @Transactional(readOnly = true)
    public List<PayslipDto> listPayslips(UUID runId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        PayrollRun run = runRepository.findByIdAndSchoolId(runId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("payroll_run", runId));
        return payslipRepository.findByPayrollRunIdOrderByStaffNameAsc(run.getId()).stream()
                .map(row -> toPayslipDto(row, run))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PayslipDto> myPayslips() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        Map<UUID, PayrollRun> runs = new java.util.HashMap<>();
        List<PayslipDto> result = new ArrayList<>();
        for (Payslip row : payslipRepository.findBySchoolIdAndUserIdOrderByCreatedAtDesc(schoolId, userId)) {
            PayrollRun run = runs.computeIfAbsent(row.getPayrollRunId(),
                    id -> runRepository.findByIdAndSchoolId(id, schoolId).orElse(null));
            if (run == null) {
                continue;
            }
            if (run.getStatus() != PayrollRunStatus.PUBLISHED && run.getStatus() != PayrollRunStatus.PAID) {
                continue;
            }
            result.add(toPayslipDto(row, run));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public byte[] downloadMyPayslip(UUID payslipId) {
        enforcePdfRateLimit(SecurityUtils.currentUserId());
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        Payslip payslip = payslipRepository.findByIdAndSchoolId(payslipId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("payslip", payslipId));
        if (payslip.getUserId() == null || !payslip.getUserId().equals(userId)) {
            throw AuthException.accessDenied();
        }
        PayrollRun run = runRepository.findByIdAndSchoolId(payslip.getPayrollRunId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("payroll_run", payslip.getPayrollRunId()));
        if (run.getStatus() != PayrollRunStatus.PUBLISHED && run.getStatus() != PayrollRunStatus.PAID) {
            throw new BusinessException("payroll.payslip_not_published");
        }
        return payslipPdfService.render(schoolId, run, payslip);
    }

    public HttpHeaders downloadHeaders(short month, short year) {
        String filename = "payslip_%02d_%d.pdf".formatted(month, year);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
        return headers;
    }

    public short monthOf(UUID payslipId) {
        Payslip payslip = payslipRepository.findByIdAndSchoolId(payslipId, SecurityUtils.currentSchoolId())
                .orElseThrow(() -> ResourceNotFoundException.of("payslip", payslipId));
        PayrollRun run = runRepository.findByIdAndSchoolId(payslip.getPayrollRunId(), SecurityUtils.currentSchoolId())
                .orElseThrow(() -> ResourceNotFoundException.of("payroll_run", payslip.getPayrollRunId()));
        return run.getMonth();
    }

    public short yearOf(UUID payslipId) {
        Payslip payslip = payslipRepository.findByIdAndSchoolId(payslipId, SecurityUtils.currentSchoolId())
                .orElseThrow(() -> ResourceNotFoundException.of("payslip", payslipId));
        PayrollRun run = runRepository.findByIdAndSchoolId(payslip.getPayrollRunId(), SecurityUtils.currentSchoolId())
                .orElseThrow(() -> ResourceNotFoundException.of("payroll_run", payslip.getPayrollRunId()));
        return run.getYear();
    }

    private void enforcePdfRateLimit(UUID userId) {
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000;
        List<Long> hits = pdfHits.computeIfAbsent(userId, id -> new ArrayList<>());
        synchronized (hits) {
            hits.removeIf(ts -> ts < windowStart);
            if (hits.size() >= PDF_LIMIT_PER_MINUTE) {
                throw new RateLimitException();
            }
            hits.add(now);
        }
    }

    private Payslip buildPayslip(
            PayrollRun run,
            StaffType staffType,
            UUID staffId,
            UUID userId,
            String employeeNo,
            String staffName,
            String designation,
            String department,
            StaffSalaryStructure structure) {
        List<SalaryLineItemDto> allowances = SalaryJson.fromJson(structure.getAllowancesJson());
        List<SalaryLineItemDto> deductions = SalaryJson.fromJson(structure.getDeductionsJson());
        BigDecimal basic = SalaryJson.money(structure.getBasic());
        BigDecimal hra = SalaryJson.money(structure.getHra());
        BigDecimal allowanceTotal = SalaryJson.sum(allowances);
        BigDecimal deductionTotal = SalaryJson.sum(deductions);
        BigDecimal gross = basic.add(hra).add(allowanceTotal);
        BigDecimal net = gross.subtract(deductionTotal);
        Payslip slip = new Payslip();
        slip.setSchoolId(run.getSchoolId());
        slip.setPayrollRunId(run.getId());
        slip.setStaffType(staffType);
        slip.setStaffId(staffId);
        slip.setUserId(userId);
        slip.setEmployeeNo(employeeNo);
        slip.setStaffName(staffName);
        slip.setDesignation(designation);
        slip.setDepartment(department);
        slip.setBasic(basic);
        slip.setHra(hra);
        slip.setAllowancesJson(SalaryJson.toJson(allowances));
        slip.setDeductionsJson(SalaryJson.toJson(deductions));
        slip.setGross(gross);
        slip.setTotalDeductions(deductionTotal);
        slip.setNet(net);
        return slip;
    }

    private SalaryStructureDto toSalaryDto(StaffSalaryStructure row) {
        StaffRef staff = resolveStaff(row.getSchoolId(), row.getStaffType(), row.getStaffId());
        List<SalaryLineItemDto> allowances = SalaryJson.fromJson(row.getAllowancesJson());
        List<SalaryLineItemDto> deductions = SalaryJson.fromJson(row.getDeductionsJson());
        BigDecimal basic = SalaryJson.money(row.getBasic());
        BigDecimal hra = SalaryJson.money(row.getHra());
        BigDecimal allowanceTotal = SalaryJson.sum(allowances);
        BigDecimal deductionTotal = SalaryJson.sum(deductions);
        BigDecimal gross = basic.add(hra).add(allowanceTotal);
        return SalaryStructureDto.from(row, staff.name(), staff.employeeNo(), allowances, deductions,
                gross, deductionTotal, gross.subtract(deductionTotal));
    }

    private PayrollRunDto toRunDto(PayrollRun run) {
        List<Payslip> slips = payslipRepository.findByPayrollRunIdOrderByStaffNameAsc(run.getId());
        BigDecimal netTotal = slips.stream()
                .map(Payslip::getNet)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return PayrollRunDto.from(run, slips.size(), netTotal);
    }

    private PayslipDto toPayslipDto(Payslip row, PayrollRun run) {
        return PayslipDto.from(
                row,
                run.getYear(),
                run.getMonth(),
                run.getStatus(),
                SalaryJson.fromJson(row.getAllowancesJson()),
                SalaryJson.fromJson(row.getDeductionsJson()));
    }

    private StaffRef resolveStaff(UUID schoolId, StaffType staffType, UUID staffId) {
        if (staffType == StaffType.TEACHING) {
            TeacherProfile teacher = teacherRepository.findByIdAndSchoolId(staffId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("staff", staffId));
            return new StaffRef(teacher.getDisplayName(), teacher.getEmployeeNo());
        }
        NonTeachingStaff staff = nonTeachingStaffRepository.findByIdAndSchoolId(staffId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("staff", staffId));
        return new StaffRef(staff.getDisplayName(), staff.getEmployeeNo());
    }

    private record StaffRef(String name, String employeeNo) {
    }
}
