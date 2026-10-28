package com.nexturn.lms.service;
import com.nexturn.lms.dto.ApprovalRequest;
import com.nexturn.lms.entity.*;
import com.nexturn.lms.enums.*;
import com.nexturn.lms.exception.ApiException;
import com.nexturn.lms.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** Commits the decision, balance, notification and audit atomically; any failure rolls all back. */
@Service
public class ApprovalServiceImpl implements ApprovalService {
    private final ApprovalRepository approvals;
    private final LeaveRepository leaves;
    private final EmployeeRepository employees;
    private final LeaveBalanceRepository balances;
    private final LeaveTypeRepository types;
    private final AuditLogService audit;
    private final NotificationService notifications;
    private final OutboxService outbox;
    public ApprovalServiceImpl(ApprovalRepository approvals, LeaveRepository leaves, EmployeeRepository employees,
            LeaveBalanceRepository balances, LeaveTypeRepository types, AuditLogService audit, NotificationService notifications, OutboxService outbox) {
        this.approvals=approvals; this.leaves=leaves; this.employees=employees; this.balances=balances;
        this.types=types; this.audit=audit; this.notifications=notifications; this.outbox=outbox;
    }
    @Override
    @Transactional
    public LeaveApplication decide(Integer leaveId, ApprovalRequest request, boolean approve, Login actor) {
        requireManager(actor);
        if (request == null || request.comment() == null || request.comment().isBlank() || request.comment().length() > 200)
            throw new ApiException(HttpStatus.BAD_REQUEST, "A comment of 1 to 200 characters is required");
        LeaveApplication leave = leaves.findLockedById(leaveId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Leave request not found"));
        Employee employee = employees.findLockedById(leave.getEmpId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employee not found"));
        if (actor.getEmpId().equals(employee.getEmpId()) || !actor.getEmpId().equals(employee.getManagerId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the employee's assigned manager can decide this leave");
        if (leave.getStatus() != LeaveStatus.PENDING)
            throw new ApiException(HttpStatus.CONFLICT, "Only pending leave requests can be decided");
        if (approve) deductBalance(leave);
        LeaveStatus decision = approve ? LeaveStatus.APPROVED : LeaveStatus.REJECTED;
        leave.setStatus(decision);
        Approval approval = new Approval();
        approval.setLeaveId(leaveId); approval.setManagerId(actor.getEmpId());
        approval.setDecision(decision); approval.setComment(request.comment().trim());
        approvals.save(approval);
        audit.log(leave.getEmpId(), leaveId, decision.name(), request.comment().trim(), actor.getEmpId());
        notifications.create(leave.getEmpId(), approve ? NotificationType.LEAVE_APPROVED : NotificationType.LEAVE_REJECTED,
                "Your leave request #" + leaveId + " was " + decision.name().toLowerCase(java.util.Locale.ROOT));
        outbox.enqueue(leave,actor,request.comment().trim());
        return leaves.save(leave);
    }
    /** Uses the stored working-day count supplied by the leave module; never recomputes holidays. */
    private void deductBalance(LeaveApplication leave) {
        if (leave.getStartDate() == null || leave.getEndDate() == null || leave.getEndDate().isBefore(leave.getStartDate())
                || leave.getStartDate().getYear() != leave.getEndDate().getYear()
                || leave.getNoOfDaysRequested() == null || leave.getNoOfDaysRequested() <= 0)
            throw new ApiException(HttpStatus.CONFLICT, "Leave must have valid dates and positive days within one balance year");
        LeaveType type = types.findById(leave.getLeaveTypeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Leave type not found"));
        LeaveBalance balance = balances.findLocked(leave.getEmpId(), leave.getLeaveTypeId(), leave.getStartDate().getYear())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Leave balance not initialized"));
        int remaining = balance.getTotalAllocatedDays() - balance.getUsedDays();
        if (balance.getUsedDays() < 0 || balance.getTotalAllocatedDays() < 0 || balance.getRemainingDays() != remaining)
            throw new ApiException(HttpStatus.CONFLICT, "Leave balance is inconsistent; contact HR");
        if (!Boolean.TRUE.equals(type.getAllowNegativeBalance()) && remaining < leave.getNoOfDaysRequested())
            throw new ApiException(HttpStatus.CONFLICT, "Insufficient leave balance");
        balance.setUsedDays(Math.addExact(balance.getUsedDays(), leave.getNoOfDaysRequested()));
        balance.setRemainingDays(balance.getTotalAllocatedDays() - balance.getUsedDays());
        balances.save(balance);
    }
    @Override
    @Transactional(readOnly=true)
    public Page<LeaveApplication> pending(Login actor, Pageable page) {
        requireManager(actor);
        return leaves.findTeamLeaves(actor.getEmpId(), LeaveStatus.PENDING, page);
    }
    private void requireManager(Login actor) {
        if (actor.getRole() != Role.MANAGER) throw new ApiException(HttpStatus.FORBIDDEN, "Manager role required");
    }
}
