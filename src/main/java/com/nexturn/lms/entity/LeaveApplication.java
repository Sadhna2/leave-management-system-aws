package com.nexturn.lms.entity;

import com.nexturn.lms.enums.LeaveStatus;
import jakarta.persistence.*;
import java.time.LocalDate;

/** Shared pending leave record; dates and working-day counts are supplied by the leave application module. */

@Entity
@Table(name = "leave_application")
public class LeaveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer leaveId;

    @Column(nullable = false)
    private Integer empId;

    @Column(nullable = false)
    private Integer leaveTypeId;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer noOfDaysRequested;

    @Column(nullable = false, length = 200)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;

    public LeaveApplication() {
    }

    /** Returns leave id. */
    public Integer getLeaveId() {
        return leaveId;
    }

    /** Returns emp id. */
    public Integer getEmpId() {
        return empId;
    }

    /** Sets emp id. */
    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    /** Returns leave type id. */
    public Integer getLeaveTypeId() {
        return leaveTypeId;
    }

    /** Sets leave type id. */
    public void setLeaveTypeId(Integer leaveTypeId) {
        this.leaveTypeId = leaveTypeId;
    }

    /** Returns start date. */
    public LocalDate getStartDate() {
        return startDate;
    }

    /** Sets start date. */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /** Returns end date. */
    public LocalDate getEndDate() {
        return endDate;
    }

    /** Sets end date. */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /** Returns no of days requested. */
    public Integer getNoOfDaysRequested() {
        return noOfDaysRequested;
    }

    /** Sets no of days requested. */
    public void setNoOfDaysRequested(Integer noOfDaysRequested) {
        this.noOfDaysRequested = noOfDaysRequested;
    }

    /** Returns reason. */
    public String getReason() {
        return reason;
    }

    /** Sets reason. */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /** Returns status. */
    public LeaveStatus getStatus() {
        return status;
    }

    /** Sets status. */
    public void setStatus(LeaveStatus status) {
        this.status = status;
    }
}
