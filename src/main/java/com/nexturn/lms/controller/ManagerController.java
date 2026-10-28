package com.nexturn.lms.controller;
import com.nexturn.lms.dto.ApprovalRequest;
import com.nexturn.lms.dto.PageResponse;
import com.nexturn.lms.entity.*;
import com.nexturn.lms.service.ApprovalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
/** Exposes the authenticated manager's queue and decision endpoints. */
@RestController @Validated
@RequestMapping("/api/manager")
public class ManagerController {
    private final ApprovalService approvals;
    public ManagerController(ApprovalService approvals) { this.approvals = approvals; }
    @GetMapping("/leaves/pending")
    public PageResponse<LeaveApplication> pending(@RequestAttribute("authenticatedUser") Login actor,
            @PageableDefault(size=20, sort="leaveId") Pageable page) { return PageResponse.of(approvals.pending(actor, page)); }
    @PostMapping("/leave/{leaveId}/approve")
    public LeaveApplication approve(@PathVariable @Positive Integer leaveId, @Valid @RequestBody ApprovalRequest request,
            @RequestAttribute("authenticatedUser") Login actor) { return approvals.decide(leaveId, request, true, actor); }
    @PostMapping("/leave/{leaveId}/reject")
    public LeaveApplication reject(@PathVariable @Positive Integer leaveId, @Valid @RequestBody ApprovalRequest request,
            @RequestAttribute("authenticatedUser") Login actor) { return approvals.decide(leaveId, request, false, actor); }
}
