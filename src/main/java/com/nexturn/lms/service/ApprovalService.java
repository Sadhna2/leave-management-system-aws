package com.nexturn.lms.service;
import com.nexturn.lms.dto.ApprovalRequest;
import com.nexturn.lms.entity.*;
import org.springframework.data.domain.*;
/** Member 3 contract for authorized pending-leave decisions. */
public interface ApprovalService {
    LeaveApplication decide(Integer leaveId, ApprovalRequest request, boolean approve, Login actor);
    Page<LeaveApplication> pending(Login actor, Pageable page);
}
