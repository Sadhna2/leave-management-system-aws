package com.nexturn.lms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Append-only decision evidence identifying the affected employee and the acting manager. */

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;

    @Column(nullable = false)
    private Integer empId;

    @Column(nullable = false)
    private Integer leaveId;

    @Column(nullable = false)
    private Integer actorEmpId;

    @Column(nullable = false, length = 50)
    private String actionStatus;

    @Column(nullable = false, length = 200)
    private String details;

    @Column(nullable = false)
    private LocalDateTime actionDate = LocalDateTime.now();

    public AuditLog() {
    }

    public AuditLog(
            Integer empId,
            Integer leaveId,
            String actionStatus,
            String details, Integer actorEmpId) {

        this.actorEmpId = actorEmpId;
        this.empId = empId;
        this.leaveId = leaveId;
        this.actionStatus = actionStatus;
        this.details = details;
    }

    /** Returns actor emp id. */
    public Integer getActorEmpId() { return actorEmpId; }

    /** Returns audit id. */
    public Long getAuditId() {
        return auditId;
    }

    /** Returns emp id. */
    public Integer getEmpId() {
        return empId;
    }

    /** Returns leave id. */
    public Integer getLeaveId() {
        return leaveId;
    }

    /** Returns action status. */
    public String getActionStatus() {
        return actionStatus;
    }

    /** Returns details. */
    public String getDetails() {
        return details;
    }

    /** Returns action date. */
    public LocalDateTime getActionDate() {
        return actionDate;
    }
}
