package com.nexturn.lms.entity;

import com.nexturn.lms.enums.LeaveStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Final manager decision with required comment and decision timestamp. */

@Entity
@Table(name = "approval", uniqueConstraints = @UniqueConstraint(columnNames = "leaveId"))
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long approvalId;

    @Column(nullable = false)
    private Integer leaveId;

    @Column(nullable = false)
    private Integer managerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus decision;

    @Column(nullable = false, length = 200)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime actionDate = LocalDateTime.now();

    public Approval() {
    }

    /** Returns approval id. */
    public Long getApprovalId() {
        return approvalId;
    }

    /** Returns leave id. */
    public Integer getLeaveId() {
        return leaveId;
    }

    /** Sets leave id. */
    public void setLeaveId(Integer leaveId) {
        this.leaveId = leaveId;
    }

    /** Returns manager id. */
    public Integer getManagerId() {
        return managerId;
    }

    /** Sets manager id. */
    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    /** Returns decision. */
    public LeaveStatus getDecision() {
        return decision;
    }

    /** Sets decision. */
    public void setDecision(LeaveStatus decision) {
        this.decision = decision;
    }

    /** Returns comment. */
    public String getComment() {
        return comment;
    }

    /** Sets comment. */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /** Returns action date. */
    public LocalDateTime getActionDate() {
        return actionDate;
    }
}
