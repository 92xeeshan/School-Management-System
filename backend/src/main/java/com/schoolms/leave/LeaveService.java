package com.schoolms.leave;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.TeacherProfile;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.common.api.PagedResponse;
import com.schoolms.common.enums.StaffType;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.file.MinioService;
import com.schoolms.leave.dto.AdjustLeaveBalanceRequest;
import com.schoolms.leave.dto.ApplyLeaveRequest;
import com.schoolms.leave.dto.AssignLeavePolicyRequest;
import com.schoolms.leave.dto.CancelLeaveRequest;
import com.schoolms.leave.dto.LeaveAssignmentDto;
import com.schoolms.leave.dto.LeaveBalanceDto;
import com.schoolms.leave.dto.LeaveDayCountDto;
import com.schoolms.leave.dto.LeavePolicyDto;
import com.schoolms.leave.dto.LeavePolicyItemDto;
import com.schoolms.leave.dto.LeavePolicyItemRequest;
import com.schoolms.leave.dto.LeavePolicyRequest;
import com.schoolms.leave.dto.LeaveRequestDto;
import com.schoolms.leave.dto.LeaveTypeDto;
import com.schoolms.leave.dto.LeaveTypeRequest;
import com.schoolms.leave.dto.LeaveYearSettingDto;
import com.schoolms.leave.dto.LeaveYearSettingRequest;
import com.schoolms.leave.dto.RejectLeaveRequest;
import com.schoolms.leave.dto.StaffLeaveOptionDto;
import com.schoolms.notification.NotificationTriggerService;
import com.schoolms.security.SecurityUtils;
import com.schoolms.security.UserPrincipal;
import com.schoolms.staff.NonTeachingStaff;
import com.schoolms.staff.NonTeachingStaffRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private static final Set<LeaveRequestStatus> BLOCKING_STATUSES =
            Set.of(LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED);
    private static final BigDecimal ZERO = new BigDecimal("0.0");

    private final LeaveTypeRepository typeRepository;
    private final LeavePolicyRepository policyRepository;
    private final LeavePolicyItemRepository policyItemRepository;
    private final LeaveYearSettingRepository yearSettingRepository;
    private final LeaveAssignmentRepository assignmentRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final LeaveBalanceLedgerRepository ledgerRepository;
    private final LeaveRequestRepository requestRepository;
    private final LeaveAuditRepository auditRepository;
    private final LeaveDayCalculator dayCalculator;
    private final TeacherProfileRepository teacherRepository;
    private final NonTeachingStaffRepository nonTeachingStaffRepository;
    private final AcademicYearRepository academicYearRepository;
    private final MinioService minioService;
    private final NotificationTriggerService notifications;

    @Transactional(readOnly = true)
    public List<LeaveTypeDto> listTypes() {
        denyLearners();
        return typeRepository.findBySchoolIdOrderByCodeAsc(SecurityUtils.currentSchoolId())
                .stream().map(LeaveTypeDto::from).toList();
    }

    @Transactional
    public LeaveTypeDto createType(LeaveTypeRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        String code = request.code().trim().toUpperCase();
        if (typeRepository.existsBySchoolIdAndCodeIgnoreCase(schoolId, code)) {
            throw new BusinessException("leave.type_code_exists");
        }
        LeaveType type = new LeaveType();
        type.setSchoolId(schoolId);
        applyType(type, request, code);
        LeaveType saved = typeRepository.save(type);
        audit(schoolId, "LEAVE_TYPE", saved.getId(), "CREATE", saved.getCode());
        return LeaveTypeDto.from(saved);
    }

    @Transactional
    public LeaveTypeDto updateType(UUID id, LeaveTypeRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        LeaveType type = typeRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("leave_type", id));
        String code = request.code().trim().toUpperCase();
        if (typeRepository.existsBySchoolIdAndCodeIgnoreCaseAndIdNot(schoolId, code, id)) {
            throw new BusinessException("leave.type_code_exists");
        }
        applyType(type, request, code);
        LeaveType saved = typeRepository.save(type);
        audit(schoolId, "LEAVE_TYPE", saved.getId(), "UPDATE", saved.getCode());
        return LeaveTypeDto.from(saved);
    }

    @Transactional
    public LeaveTypeDto deactivateType(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        LeaveType type = typeRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("leave_type", id));
        type.setActive(false);
        LeaveType saved = typeRepository.save(type);
        audit(schoolId, "LEAVE_TYPE", saved.getId(), "DEACTIVATE", saved.getCode());
        return LeaveTypeDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<LeavePolicyDto> listPolicies() {
        denyLearners();
        UUID schoolId = SecurityUtils.currentSchoolId();
        Map<UUID, LeaveType> types = typesById(schoolId);
        return policyRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .map(policy -> toPolicyDto(policy, types))
                .toList();
    }

    @Transactional
    public LeavePolicyDto createPolicy(LeavePolicyRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        if (policyRepository.existsBySchoolIdAndNameIgnoreCase(schoolId, request.name().trim())) {
            throw new BusinessException("leave.policy_name_exists");
        }
        LeavePolicy policy = new LeavePolicy();
        policy.setSchoolId(schoolId);
        policy.setName(request.name().trim());
        policy.setStaffCategory(request.staffCategory());
        policy.setActive(request.active() == null || request.active());
        LeavePolicy saved = policyRepository.save(policy);
        replaceItems(saved, request.items());
        audit(schoolId, "LEAVE_POLICY", saved.getId(), "CREATE", saved.getName());
        return toPolicyDto(saved, typesById(schoolId));
    }

    @Transactional
    public LeavePolicyDto updatePolicy(UUID id, LeavePolicyRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        LeavePolicy policy = policyRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("leave_policy", id));
        if (policyRepository.existsBySchoolIdAndNameIgnoreCaseAndIdNot(schoolId, request.name().trim(), id)) {
            throw new BusinessException("leave.policy_name_exists");
        }
        policy.setName(request.name().trim());
        policy.setStaffCategory(request.staffCategory());
        if (request.active() != null) {
            policy.setActive(request.active());
        }
        LeavePolicy saved = policyRepository.save(policy);
        replaceItems(saved, request.items());
        audit(schoolId, "LEAVE_POLICY", saved.getId(), "UPDATE", saved.getName());
        return toPolicyDto(saved, typesById(schoolId));
    }

    @Transactional(readOnly = true)
    public LeaveYearSettingDto yearSetting() {
        denyLearners();
        UUID schoolId = SecurityUtils.currentSchoolId();
        YearWindow window = yearWindow(schoolId);
        LeaveYearSetting setting = yearSettingRepository.findBySchoolId(schoolId).orElse(null);
        return new LeaveYearSettingDto(
                setting == null ? null : setting.getId(),
                window.mode(),
                window.start(),
                window.end());
    }

    @Transactional
    public LeaveYearSettingDto updateYearSetting(LeaveYearSettingRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        LeaveYearSetting setting = yearSettingRepository.findBySchoolId(schoolId).orElseGet(() -> {
            LeaveYearSetting created = new LeaveYearSetting();
            created.setSchoolId(schoolId);
            return created;
        });
        setting.setMode(request.mode());
        yearSettingRepository.save(setting);
        audit(schoolId, "LEAVE_YEAR", setting.getId(), "UPDATE", request.mode().name());
        return yearSetting();
    }

    @Transactional(readOnly = true)
    public List<LeaveAssignmentDto> listAssignments() {
        denyLearners();
        UUID schoolId = SecurityUtils.currentSchoolId();
        Map<UUID, LeavePolicy> policies = policyRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .collect(Collectors.toMap(LeavePolicy::getId, Function.identity()));
        return assignmentRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId).stream()
                .map(row -> {
                    StaffRef staff = resolveStaff(schoolId, row.getStaffType(), row.getStaffId());
                    LeavePolicy policy = policies.get(row.getPolicyId());
                    return new LeaveAssignmentDto(
                            row.getId(),
                            row.getStaffType(),
                            row.getStaffId(),
                            staff.name(),
                            staff.employeeNo(),
                            row.getPolicyId(),
                            policy == null ? "" : policy.getName(),
                            row.getYearStart());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffLeaveOptionDto> listStaffOptions() {
        denyLearners();
        UUID schoolId = SecurityUtils.currentSchoolId();
        Map<String, LeaveAssignment> assignments = assignmentRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId)
                .stream()
                .collect(Collectors.toMap(row -> row.getStaffType() + ":" + row.getStaffId(), Function.identity(), (a, b) -> a));
        Map<UUID, String> policyNames = policyRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .collect(Collectors.toMap(LeavePolicy::getId, LeavePolicy::getName));
        List<StaffLeaveOptionDto> options = new ArrayList<>();
        for (TeacherProfile teacher : teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId)) {
            LeaveAssignment assignment = assignments.get(StaffType.TEACHING + ":" + teacher.getId());
            options.add(new StaffLeaveOptionDto(
                    StaffType.TEACHING,
                    teacher.getId(),
                    teacher.getDisplayName(),
                    teacher.getEmployeeNo(),
                    assignment == null ? null : assignment.getPolicyId(),
                    assignment == null ? null : policyNames.get(assignment.getPolicyId())));
        }
        for (NonTeachingStaff staff : nonTeachingStaffRepository.findBySchoolIdOrderByFirstNameAsc(schoolId)) {
            LeaveAssignment assignment = assignments.get(StaffType.NON_TEACHING + ":" + staff.getId());
            options.add(new StaffLeaveOptionDto(
                    StaffType.NON_TEACHING,
                    staff.getId(),
                    staff.getDisplayName(),
                    staff.getEmployeeNo(),
                    assignment == null ? null : assignment.getPolicyId(),
                    assignment == null ? null : policyNames.get(assignment.getPolicyId())));
        }
        return options;
    }

    @Transactional
    public List<LeaveAssignmentDto> assignPolicy(AssignLeavePolicyRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        LeavePolicy policy = policyRepository.findByIdAndSchoolId(request.policyId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("leave_policy", request.policyId()));
        if (!policy.isActive()) {
            throw new BusinessException("leave.policy_inactive");
        }
        YearWindow window = yearWindow(schoolId);
        List<LeaveAssignmentDto> result = new ArrayList<>();
        for (UUID staffId : request.staffIds()) {
            StaffType staffType = request.staffType();
            StaffRef staff = resolveStaff(schoolId, staffType, staffId);
            if (policy.getStaffCategory() != LeaveStaffCategory.ALL
                    && !policy.getStaffCategory().name().equals(staffType.name())) {
                throw new BusinessException("leave.policy_category_mismatch");
            }
            LeaveAssignment assignment = assignmentRepository
                    .findBySchoolIdAndStaffTypeAndStaffId(schoolId, staffType, staffId)
                    .orElseGet(LeaveAssignment::new);
            assignment.setSchoolId(schoolId);
            assignment.setStaffType(staffType);
            assignment.setStaffId(staffId);
            assignment.setPolicyId(policy.getId());
            assignment.setYearStart(window.start());
            assignment.setAssignedBy(SecurityUtils.currentUserId());
            assignmentRepository.save(assignment);
            creditPolicy(schoolId, staff, policy, window);
            audit(schoolId, "LEAVE_ASSIGNMENT", assignment.getId(), "ASSIGN", staff.name());
            result.add(new LeaveAssignmentDto(
                    assignment.getId(), staffType, staffId, staff.name(), staff.employeeNo(),
                    policy.getId(), policy.getName(), window.start()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> myBalances() {
        StaffRef staff = currentStaff();
        return balancesFor(SecurityUtils.currentSchoolId(), staff);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> staffBalances(StaffType staffType, UUID staffId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        StaffRef staff = resolveStaff(schoolId, staffType, staffId);
        return balancesFor(schoolId, staff);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> balanceReport() {
        denyLearners();
        UUID schoolId = SecurityUtils.currentSchoolId();
        YearWindow window = yearWindow(schoolId);
        Map<UUID, LeaveType> types = typesById(schoolId);
        List<LeaveBalanceDto> rows = new ArrayList<>();
        for (LeaveBalance balance : balanceRepository.findBySchoolIdAndYearStart(schoolId, window.start())) {
            StaffRef staff = resolveStaff(schoolId, balance.getStaffType(), balance.getStaffId());
            rows.add(toBalanceDto(balance, types.get(balance.getLeaveTypeId()), staff));
        }
        rows.sort(Comparator.comparing(LeaveBalanceDto::staffName).thenComparing(LeaveBalanceDto::leaveTypeCode));
        return rows;
    }

    @Transactional
    public LeaveBalanceDto adjustBalance(AdjustLeaveBalanceRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        StaffRef staff = resolveStaff(schoolId, request.staffType(), request.staffId());
        YearWindow window = yearWindow(schoolId);
        LeaveType type = typeRepository.findByIdAndSchoolId(request.leaveTypeId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("leave_type", request.leaveTypeId()));
        LeaveBalance balance = lockBalance(schoolId, staff, type.getId(), window.start());
        BigDecimal days = request.days().setScale(1, RoundingMode.HALF_UP);
        BigDecimal entitled = balance.getEntitled().add(days);
        if (entitled.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("leave.balance_negative");
        }
        if (available(entitled, balance.getUsed(), balance.getPending()).compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("leave.balance_negative");
        }
        balance.setEntitled(entitled);
        balanceRepository.save(balance);
        writeLedger(schoolId, staff, type.getId(), window.start(), LeaveLedgerType.ADJUST, days, null, request.reason());
        audit(schoolId, "LEAVE_BALANCE", balance.getId(), "ADJUST", request.reason());
        return toBalanceDto(balance, type, staff);
    }

    @Transactional(readOnly = true)
    public LeaveDayCountDto previewDays(LocalDate from, LocalDate to) {
        denyLearners();
        if (from == null || to == null || to.isBefore(from)) {
            throw new BusinessException("leave.invalid_dates");
        }
        return new LeaveDayCountDto(from, to, dayCalculator.countWorkingDays(SecurityUtils.currentSchoolId(), from, to));
    }

    @Transactional
    public LeaveRequestDto apply(ApplyLeaveRequest body, MultipartFile file) {
        StaffRef staff = currentStaff();
        UUID schoolId = staff.schoolId();
        if (body.toDate().isBefore(body.fromDate())) {
            throw new BusinessException("leave.invalid_dates");
        }
        LeaveType type = typeRepository.findByIdAndSchoolId(body.leaveTypeId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("leave_type", body.leaveTypeId()));
        if (!type.isActive()) {
            throw new BusinessException("leave.type_inactive");
        }
        if (type.isReasonMandatory() && !StringUtils.hasText(body.reason())) {
            throw new BusinessException("leave.reason_required");
        }
        LeaveAssignment assignment = assignmentRepository
                .findBySchoolIdAndStaffTypeAndStaffId(schoolId, staff.staffType(), staff.staffId())
                .orElseThrow(() -> new BusinessException("leave.no_policy"));
        boolean eligible = policyItemRepository.findByPolicyIdOrderByCreatedAtAsc(assignment.getPolicyId()).stream()
                .anyMatch(item -> item.getLeaveTypeId().equals(type.getId()));
        if (!eligible) {
            throw new BusinessException("leave.type_ineligible");
        }
        BigDecimal days = dayCalculator.countWorkingDays(schoolId, body.fromDate(), body.toDate());
        if (days.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("leave.no_working_days");
        }
        List<LeaveRequest> overlapping = requestRepository.findOverlapping(
                schoolId, staff.staffType(), staff.staffId(), body.fromDate(), body.toDate(), BLOCKING_STATUSES);
        if (!overlapping.isEmpty()) {
            throw new BusinessException("leave.overlap");
        }
        if (requestRepository.existsBySchoolIdAndStaffTypeAndStaffIdAndLeaveTypeIdAndFromDateAndToDateAndStatus(
                schoolId, staff.staffType(), staff.staffId(), type.getId(),
                body.fromDate(), body.toDate(), LeaveRequestStatus.PENDING)) {
            throw new BusinessException("leave.duplicate");
        }
        YearWindow window = yearWindow(schoolId);
        LeaveBalance balance = lockBalance(schoolId, staff, type.getId(), window.start());
        if (available(balance).compareTo(days) < 0) {
            throw new BusinessException("leave.insufficient_balance");
        }
        LeaveRequest request = new LeaveRequest();
        request.setSchoolId(schoolId);
        request.setStaffType(staff.staffType());
        request.setStaffId(staff.staffId());
        request.setUserId(staff.userId());
        request.setLeaveTypeId(type.getId());
        request.setFromDate(body.fromDate());
        request.setToDate(body.toDate());
        request.setDays(days);
        request.setReason(body.reason().trim());
        request.setStatus(LeaveRequestStatus.PENDING);
        if (file != null && !file.isEmpty()) {
            request.setSupportingDocKey(minioService.upload(schoolId, "leave-docs", file));
            request.setSupportingDocName(file.getOriginalFilename());
        }
        LeaveRequest saved = requestRepository.save(request);
        balance.setPending(balance.getPending().add(days));
        balanceRepository.save(balance);
        writeLedger(schoolId, staff, type.getId(), window.start(), LeaveLedgerType.PENDING, days, saved.getId(), saved.getReason());
        audit(schoolId, "LEAVE_REQUEST", saved.getId(), "APPLY", saved.getReason());
        notifySafely(() -> notifications.onLeaveRequested(
                schoolId, staff.name(), type.getName(), saved.getFromDate(), saved.getToDate(),
                days, saved.getReason(), SecurityUtils.currentUserId()));
        return toRequestDto(saved, type, staff, toBalanceDto(balance, type, staff), true, false);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeaveRequestDto> myRequests(LeaveRequestStatus status, UUID leaveTypeId, int page, int size) {
        StaffRef staff = currentStaff();
        UUID schoolId = staff.schoolId();
        Map<UUID, LeaveType> types = typesById(schoolId);
        List<LeaveRequestDto> rows = requestRepository
                .findBySchoolIdAndStaffTypeAndStaffIdOrderByFromDateDesc(schoolId, staff.staffType(), staff.staffId())
                .stream()
                .filter(row -> status == null || row.getStatus() == status)
                .filter(row -> leaveTypeId == null || leaveTypeId.equals(row.getLeaveTypeId()))
                .map(row -> toRequestDto(row, types.get(row.getLeaveTypeId()), staff, null, true, false))
                .toList();
        return pageOf(rows, page, size);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeaveRequestDto> listRequests(LeaveRequestStatus status, int page, int size) {
        denyLearners();
        UUID schoolId = SecurityUtils.currentSchoolId();
        Map<UUID, LeaveType> types = typesById(schoolId);
        List<LeaveRequest> source = status == null
                ? requestRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId)
                : requestRepository.findBySchoolIdAndStatusOrderByCreatedAtDesc(schoolId, status);
        boolean canApprove = canApprove();
        List<LeaveRequestDto> rows = source.stream()
                .map(row -> {
                    StaffRef staff = resolveStaff(schoolId, row.getStaffType(), row.getStaffId());
                    LeaveBalanceDto snapshot = balanceSnapshot(schoolId, staff, row.getLeaveTypeId());
                    return toRequestDto(row, types.get(row.getLeaveTypeId()), staff, snapshot, false, canApprove);
                })
                .toList();
        return pageOf(rows, page, size);
    }

    @Transactional
    public LeaveRequestDto approve(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        LeaveRequest request = requestRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("leave_request", id));
        if (request.getStatus() != LeaveRequestStatus.PENDING) {
            throw new BusinessException("leave.already_actioned");
        }
        if (SecurityUtils.currentUserId().equals(request.getUserId())) {
            throw new BusinessException("leave.self_approve");
        }
        StaffRef staff = resolveStaff(schoolId, request.getStaffType(), request.getStaffId());
        YearWindow window = yearWindow(schoolId);
        LeaveBalance balance = lockBalance(schoolId, staff, request.getLeaveTypeId(), window.start());
        if (balance.getPending().compareTo(request.getDays()) < 0) {
            throw new BusinessException("leave.balance_mismatch");
        }
        balance.setPending(balance.getPending().subtract(request.getDays()));
        balance.setUsed(balance.getUsed().add(request.getDays()));
        balanceRepository.save(balance);
        request.setStatus(LeaveRequestStatus.APPROVED);
        request.setDecidedBy(SecurityUtils.currentUserId());
        request.setDecidedAt(Instant.now());
        requestRepository.save(request);
        writeLedger(schoolId, staff, request.getLeaveTypeId(), window.start(),
                LeaveLedgerType.USED, request.getDays(), request.getId(), "Approved");
        audit(schoolId, "LEAVE_REQUEST", request.getId(), "APPROVE", null);
        LeaveType type = typeRepository.findByIdAndSchoolId(request.getLeaveTypeId(), schoolId).orElse(null);
        notifySafely(() -> notifications.onLeaveDecision(
                schoolId, request.getUserId(), true, type == null ? "Leave" : type.getName(),
                request.getFromDate(), request.getToDate(), null));
        return toRequestDto(request, type, staff, toBalanceDto(balance, type, staff), false, true);
    }

    @Transactional
    public LeaveRequestDto reject(UUID id, RejectLeaveRequest body) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        LeaveRequest request = requestRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("leave_request", id));
        if (request.getStatus() != LeaveRequestStatus.PENDING) {
            throw new BusinessException("leave.already_actioned");
        }
        StaffRef staff = resolveStaff(schoolId, request.getStaffType(), request.getStaffId());
        YearWindow window = yearWindow(schoolId);
        LeaveBalance balance = lockBalance(schoolId, staff, request.getLeaveTypeId(), window.start());
        releasePending(balance, request.getDays());
        balanceRepository.save(balance);
        request.setStatus(LeaveRequestStatus.REJECTED);
        request.setRejectionReason(body.rejectionReason().trim());
        request.setDecidedBy(SecurityUtils.currentUserId());
        request.setDecidedAt(Instant.now());
        requestRepository.save(request);
        writeLedger(schoolId, staff, request.getLeaveTypeId(), window.start(),
                LeaveLedgerType.RELEASE, request.getDays(), request.getId(), body.rejectionReason());
        audit(schoolId, "LEAVE_REQUEST", request.getId(), "REJECT", body.rejectionReason());
        LeaveType type = typeRepository.findByIdAndSchoolId(request.getLeaveTypeId(), schoolId).orElse(null);
        notifySafely(() -> notifications.onLeaveDecision(
                schoolId, request.getUserId(), false, type == null ? "Leave" : type.getName(),
                request.getFromDate(), request.getToDate(), body.rejectionReason()));
        return toRequestDto(request, type, staff, toBalanceDto(balance, type, staff), false, true);
    }

    @Transactional
    public LeaveRequestDto cancel(UUID id, CancelLeaveRequest body) {
        StaffRef actor = currentStaff();
        UUID schoolId = actor.schoolId();
        LeaveRequest request = requestRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("leave_request", id));
        boolean owner = actor.staffType() == request.getStaffType() && actor.staffId().equals(request.getStaffId());
        if (!owner && !canManage()) {
            throw new AccessDeniedException("forbidden");
        }
        if (request.getStatus() == LeaveRequestStatus.REJECTED || request.getStatus() == LeaveRequestStatus.CANCELLED) {
            throw new BusinessException("leave.already_actioned");
        }
        if (request.getStatus() == LeaveRequestStatus.APPROVED && !LocalDate.now().isBefore(request.getFromDate())) {
            throw new BusinessException("leave.cancel_started");
        }
        StaffRef staff = resolveStaff(schoolId, request.getStaffType(), request.getStaffId());
        YearWindow window = yearWindow(schoolId);
        LeaveBalance balance = lockBalance(schoolId, staff, request.getLeaveTypeId(), window.start());
        LeaveLedgerType ledgerType;
        if (request.getStatus() == LeaveRequestStatus.PENDING) {
            releasePending(balance, request.getDays());
            ledgerType = LeaveLedgerType.RELEASE;
        } else {
            if (balance.getUsed().compareTo(request.getDays()) < 0) {
                throw new BusinessException("leave.balance_mismatch");
            }
            balance.setUsed(balance.getUsed().subtract(request.getDays()));
            ledgerType = LeaveLedgerType.RELEASE;
        }
        balanceRepository.save(balance);
        request.setStatus(LeaveRequestStatus.CANCELLED);
        request.setCancelReason(body.reason().trim());
        request.setCancelledBy(SecurityUtils.currentUserId());
        request.setCancelledAt(Instant.now());
        requestRepository.save(request);
        writeLedger(schoolId, staff, request.getLeaveTypeId(), window.start(),
                ledgerType, request.getDays(), request.getId(), body.reason());
        audit(schoolId, "LEAVE_REQUEST", request.getId(), "CANCEL", body.reason());
        LeaveType type = typeRepository.findByIdAndSchoolId(request.getLeaveTypeId(), schoolId).orElse(null);
        notifySafely(() -> notifications.onLeaveCancelled(
                schoolId, staff.name(), type == null ? "Leave" : type.getName(),
                request.getFromDate(), request.getToDate(), body.reason(), SecurityUtils.currentUserId()));
        return toRequestDto(request, type, staff, toBalanceDto(balance, type, staff), true, canApprove());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> onLeaveToday() {
        denyLearners();
        UUID schoolId = SecurityUtils.currentSchoolId();
        Map<UUID, LeaveType> types = typesById(schoolId);
        return requestRepository.findOnLeave(schoolId, LeaveRequestStatus.APPROVED, LocalDate.now()).stream()
                .map(row -> {
                    StaffRef staff = resolveStaff(schoolId, row.getStaffType(), row.getStaffId());
                    return toRequestDto(row, types.get(row.getLeaveTypeId()), staff, null, false, false);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> leaveTaken() {
        denyLearners();
        UUID schoolId = SecurityUtils.currentSchoolId();
        YearWindow window = yearWindow(schoolId);
        Map<UUID, LeaveType> types = typesById(schoolId);
        return requestRepository.findBySchoolIdAndStatusOrderByCreatedAtDesc(schoolId, LeaveRequestStatus.APPROVED)
                .stream()
                .filter(row -> !row.getToDate().isBefore(window.start()) && !row.getFromDate().isAfter(window.end()))
                .map(row -> {
                    StaffRef staff = resolveStaff(schoolId, row.getStaffType(), row.getStaffId());
                    return toRequestDto(row, types.get(row.getLeaveTypeId()), staff, null, false, false);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportBalances() {
        return excel("leave-balances.xlsx", "Balances",
                new String[]{"Employee", "Employee No", "Category", "Leave type", "Entitled", "Used", "Pending", "Available"},
                balanceReport().stream().map(row -> new String[]{
                        row.staffName(),
                        "",
                        row.staffType().name(),
                        row.leaveTypeName(),
                        n(row.entitled()),
                        n(row.used()),
                        n(row.pending()),
                        n(row.available())
                }).toList());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportTaken() {
        return excel("leave-taken.xlsx", "Leave taken",
                new String[]{"Employee", "Employee No", "Type", "From", "To", "Days", "Reason", "Status"},
                leaveTaken().stream().map(row -> new String[]{
                        row.staffName(),
                        row.employeeNo(),
                        row.leaveTypeName(),
                        String.valueOf(row.fromDate()),
                        String.valueOf(row.toDate()),
                        n(row.days()),
                        row.reason(),
                        row.status().name()
                }).toList());
    }

    private void applyType(LeaveType type, LeaveTypeRequest request, String code) {
        type.setCode(code);
        type.setName(request.name().trim());
        type.setYearlyQuota(request.yearlyQuota().setScale(1, RoundingMode.HALF_UP));
        type.setPaid(Boolean.TRUE.equals(request.paid()));
        type.setReasonMandatory(Boolean.TRUE.equals(request.reasonMandatory()));
        type.setActive(request.active() == null || request.active());
    }

    private void replaceItems(LeavePolicy policy, List<LeavePolicyItemRequest> items) {
        policyItemRepository.deleteByPolicyId(policy.getId());
        if (items == null) {
            return;
        }
        for (LeavePolicyItemRequest item : items) {
            LeaveType type = typeRepository.findByIdAndSchoolId(item.leaveTypeId(), policy.getSchoolId())
                    .orElseThrow(() -> ResourceNotFoundException.of("leave_type", item.leaveTypeId()));
            LeavePolicyItem row = new LeavePolicyItem();
            row.setSchoolId(policy.getSchoolId());
            row.setPolicyId(policy.getId());
            row.setLeaveTypeId(type.getId());
            row.setYearlyQuota(item.yearlyQuota().setScale(1, RoundingMode.HALF_UP));
            policyItemRepository.save(row);
        }
    }

    private void creditPolicy(UUID schoolId, StaffRef staff, LeavePolicy policy, YearWindow window) {
        Map<UUID, LeaveType> types = typesById(schoolId);
        for (LeavePolicyItem item : policyItemRepository.findByPolicyIdOrderByCreatedAtAsc(policy.getId())) {
            LeaveBalance existing = balanceRepository.findBySchoolIdAndStaffTypeAndStaffIdAndLeaveTypeIdAndYearStart(
                    schoolId, staff.staffType(), staff.staffId(), item.getLeaveTypeId(), window.start()).orElse(null);
            if (existing != null) {
                continue;
            }
            BigDecimal entitled = proRata(item.getYearlyQuota(), staff.joinDate(), window);
            LeaveBalance balance = new LeaveBalance();
            balance.setSchoolId(schoolId);
            balance.setStaffType(staff.staffType());
            balance.setStaffId(staff.staffId());
            balance.setLeaveTypeId(item.getLeaveTypeId());
            balance.setYearStart(window.start());
            balance.setEntitled(entitled);
            balance.setUsed(ZERO);
            balance.setPending(ZERO);
            balanceRepository.save(balance);
            LeaveType type = types.get(item.getLeaveTypeId());
            writeLedger(schoolId, staff, item.getLeaveTypeId(), window.start(),
                    LeaveLedgerType.CREDIT, entitled, null,
                    "Policy credit" + (type == null ? "" : " — " + type.getName()));
        }
    }

    private BigDecimal proRata(BigDecimal quota, LocalDate joinDate, YearWindow window) {
        if (quota == null || quota.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }
        LocalDate start = window.start();
        if (joinDate != null && joinDate.isAfter(window.end())) {
            return ZERO;
        }
        if (joinDate != null && joinDate.isAfter(start)) {
            start = joinDate;
        }
        long total = ChronoUnit.DAYS.between(window.start(), window.end()) + 1;
        long remaining = ChronoUnit.DAYS.between(start, window.end()) + 1;
        if (total <= 0 || remaining <= 0) {
            return ZERO;
        }
        return quota.multiply(BigDecimal.valueOf(remaining))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private LeaveBalance lockBalance(UUID schoolId, StaffRef staff, UUID leaveTypeId, LocalDate yearStart) {
        return balanceRepository.lockByStaffAndType(schoolId, staff.staffType(), staff.staffId(), leaveTypeId, yearStart)
                .orElseThrow(() -> new BusinessException("leave.type_ineligible"));
    }

    private void releasePending(LeaveBalance balance, BigDecimal days) {
        if (balance.getPending().compareTo(days) < 0) {
            throw new BusinessException("leave.balance_mismatch");
        }
        balance.setPending(balance.getPending().subtract(days));
    }

    private void writeLedger(UUID schoolId, StaffRef staff, UUID leaveTypeId, LocalDate yearStart,
                             LeaveLedgerType type, BigDecimal days, UUID requestId, String reason) {
        LeaveBalanceLedger ledger = new LeaveBalanceLedger();
        ledger.setSchoolId(schoolId);
        ledger.setStaffType(staff.staffType());
        ledger.setStaffId(staff.staffId());
        ledger.setLeaveTypeId(leaveTypeId);
        ledger.setYearStart(yearStart);
        ledger.setEntryType(type);
        ledger.setDays(days);
        ledger.setRequestId(requestId);
        ledger.setReason(reason);
        ledger.setCreatedBy(SecurityUtils.currentUserId());
        ledgerRepository.save(ledger);
    }

    private void audit(UUID schoolId, String entityType, UUID entityId, String action, String details) {
        LeaveAudit audit = new LeaveAudit();
        audit.setSchoolId(schoolId);
        audit.setEntityType(entityType);
        audit.setEntityId(entityId);
        audit.setAction(action);
        audit.setDetails(details);
        audit.setChangedBy(SecurityUtils.currentUserId());
        auditRepository.save(audit);
    }

    private List<LeaveBalanceDto> balancesFor(UUID schoolId, StaffRef staff) {
        YearWindow window = yearWindow(schoolId);
        Map<UUID, LeaveType> types = typesById(schoolId);
        return balanceRepository.findBySchoolIdAndStaffTypeAndStaffIdAndYearStart(
                        schoolId, staff.staffType(), staff.staffId(), window.start())
                .stream()
                .map(row -> toBalanceDto(row, types.get(row.getLeaveTypeId()), staff))
                .toList();
    }

    private LeaveBalanceDto balanceSnapshot(UUID schoolId, StaffRef staff, UUID leaveTypeId) {
        YearWindow window = yearWindow(schoolId);
        LeaveBalance balance = balanceRepository.findBySchoolIdAndStaffTypeAndStaffIdAndLeaveTypeIdAndYearStart(
                schoolId, staff.staffType(), staff.staffId(), leaveTypeId, window.start()).orElse(null);
        if (balance == null) {
            return null;
        }
        LeaveType type = typeRepository.findByIdAndSchoolId(leaveTypeId, schoolId).orElse(null);
        return toBalanceDto(balance, type, staff);
    }

    private LeaveBalanceDto toBalanceDto(LeaveBalance balance, LeaveType type, StaffRef staff) {
        return new LeaveBalanceDto(
                balance.getLeaveTypeId(),
                type == null ? "" : type.getCode(),
                type == null ? "" : type.getName(),
                type != null && type.isPaid(),
                balance.getStaffType(),
                balance.getStaffId(),
                staff.name(),
                balance.getYearStart(),
                balance.getEntitled(),
                balance.getUsed(),
                balance.getPending(),
                available(balance));
    }

    private LeavePolicyDto toPolicyDto(LeavePolicy policy, Map<UUID, LeaveType> types) {
        List<LeavePolicyItemDto> items = policyItemRepository.findByPolicyIdOrderByCreatedAtAsc(policy.getId()).stream()
                .map(item -> {
                    LeaveType type = types.get(item.getLeaveTypeId());
                    return new LeavePolicyItemDto(
                            item.getLeaveTypeId(),
                            type == null ? "" : type.getCode(),
                            type == null ? "" : type.getName(),
                            item.getYearlyQuota());
                })
                .toList();
        return new LeavePolicyDto(policy.getId(), policy.getName(), policy.getStaffCategory(), policy.isActive(), items);
    }

    private LeaveRequestDto toRequestDto(LeaveRequest request, LeaveType type, StaffRef staff,
                                         LeaveBalanceDto snapshot, boolean owner, boolean approver) {
        boolean pending = request.getStatus() == LeaveRequestStatus.PENDING;
        boolean approvedFuture = request.getStatus() == LeaveRequestStatus.APPROVED
                && LocalDate.now().isBefore(request.getFromDate());
        return new LeaveRequestDto(
                request.getId(),
                request.getStaffType(),
                request.getStaffId(),
                staff.name(),
                staff.employeeNo(),
                request.getLeaveTypeId(),
                type == null ? "" : type.getCode(),
                type == null ? "" : type.getName(),
                request.getFromDate(),
                request.getToDate(),
                request.getDays(),
                request.getReason(),
                request.getStatus(),
                request.getSupportingDocName(),
                request.getRejectionReason(),
                request.getCancelReason(),
                request.getCreatedAt(),
                request.getDecidedAt(),
                owner && (pending || approvedFuture),
                approver && pending && !SecurityUtils.currentUserId().equals(request.getUserId()),
                snapshot);
    }

    private Map<UUID, LeaveType> typesById(UUID schoolId) {
        return typeRepository.findBySchoolIdOrderByCodeAsc(schoolId).stream()
                .collect(Collectors.toMap(LeaveType::getId, Function.identity()));
    }

    private YearWindow yearWindow(UUID schoolId) {
        LeaveYearMode mode = yearSettingRepository.findBySchoolId(schoolId)
                .map(LeaveYearSetting::getMode)
                .orElse(LeaveYearMode.ACADEMIC);
        if (mode == LeaveYearMode.CALENDAR) {
            int year = LocalDate.now().getYear();
            return new YearWindow(mode, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        }
        AcademicYear academicYear = academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId).orElse(null);
        if (academicYear == null) {
            int year = LocalDate.now().getYear();
            return new YearWindow(mode, LocalDate.of(year, 4, 1), LocalDate.of(year + 1, 3, 31));
        }
        return new YearWindow(mode, academicYear.getStartDate(), academicYear.getEndDate());
    }

    private StaffRef currentStaff() {
        denyLearners();
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        TeacherProfile teacher = teacherRepository.findBySchoolIdAndUserId(schoolId, userId).orElse(null);
        if (teacher != null) {
            return new StaffRef(schoolId, StaffType.TEACHING, teacher.getId(), teacher.getUserId(),
                    teacher.getDisplayName(), teacher.getEmployeeNo(), teacher.getJoinDate());
        }
        NonTeachingStaff staff = nonTeachingStaffRepository.findBySchoolIdAndUserId(schoolId, userId).orElse(null);
        if (staff != null) {
            return new StaffRef(schoolId, StaffType.NON_TEACHING, staff.getId(), staff.getUserId(),
                    staff.getDisplayName(), staff.getEmployeeNo(), staff.getJoinDate());
        }
        throw new BusinessException("leave.not_staff");
    }

    private StaffRef resolveStaff(UUID schoolId, StaffType staffType, UUID staffId) {
        if (staffType == StaffType.TEACHING) {
            TeacherProfile teacher = teacherRepository.findByIdAndSchoolId(staffId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("staff", staffId));
            return new StaffRef(schoolId, StaffType.TEACHING, teacher.getId(), teacher.getUserId(),
                    teacher.getDisplayName(), teacher.getEmployeeNo(), teacher.getJoinDate());
        }
        NonTeachingStaff staff = nonTeachingStaffRepository.findByIdAndSchoolId(staffId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("staff", staffId));
        return new StaffRef(schoolId, StaffType.NON_TEACHING, staff.getId(), staff.getUserId(),
                staff.getDisplayName(), staff.getEmployeeNo(), staff.getJoinDate());
    }

    private void denyLearners() {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (principal.hasRole("STUDENT") || principal.hasRole("PARENT")) {
            throw new AccessDeniedException("forbidden");
        }
    }

    private boolean canManage() {
        return SecurityUtils.currentPrincipal().permissions().contains("LEAVE_MANAGE");
    }

    private boolean canApprove() {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        return principal.permissions().contains("LEAVE_APPROVE")
                || principal.hasRole("ADMIN")
                || principal.hasRole("SUPER_ADMIN");
    }

    private static BigDecimal available(LeaveBalance balance) {
        return available(balance.getEntitled(), balance.getUsed(), balance.getPending());
    }

    private static BigDecimal available(BigDecimal entitled, BigDecimal used, BigDecimal pending) {
        return entitled.subtract(used).subtract(pending);
    }

    private static PagedResponse<LeaveRequestDto> pageOf(List<LeaveRequestDto> rows, int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        int from = Math.min(p * s, rows.size());
        int to = Math.min(from + s, rows.size());
        return PagedResponse.from(new PageImpl<>(rows.subList(from, to), PageRequest.of(p, s), rows.size()));
    }

    private static String n(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private ResponseEntity<byte[]> excel(String filename, String sheetName, String[] headers, List<String[]> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int r = 1;
            for (String[] rowValues : rows) {
                Row row = sheet.createRow(r++);
                for (int i = 0; i < rowValues.length; i++) {
                    row.createCell(i).setCellValue(rowValues[i] == null ? "" : rowValues[i]);
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        } catch (IOException e) {
            throw new BusinessException("leave.export_failed");
        }
    }

    private void notifySafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ignored) {
            // A notification failure must not fail the leave request.
        }
    }

    private record StaffRef(
            UUID schoolId,
            StaffType staffType,
            UUID staffId,
            UUID userId,
            String name,
            String employeeNo,
            LocalDate joinDate
    ) {
    }

    private record YearWindow(LeaveYearMode mode, LocalDate start, LocalDate end) {
    }
}
