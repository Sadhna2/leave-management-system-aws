package com.nexturn.lms.entity;

import jakarta.persistence.*;

/** Shared yearly balance; approval updates used and remaining days under a database row lock. */

@Entity
@Table(
        name = "leave_balance",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"emp_id", "leave_type_id", "year"}
        )
)
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer balanceId;

    @Column(name = "emp_id", nullable = false)
    private Integer empId;

    @Column(name = "leave_type_id", nullable = false)
    private Integer leaveTypeId;

    @Column(nullable = false)
    private Integer totalAllocatedDays;

    @Column(nullable = false)
    private Integer usedDays = 0;

    @Column(nullable = false)
    private Integer remainingDays;

    @Column(nullable = false)
    private Integer year;

    public LeaveBalance() {
    }

    /** Returns balance id. */
    public Integer getBalanceId() {
        return balanceId;
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

    /** Returns total allocated days. */
    public Integer getTotalAllocatedDays() {
        return totalAllocatedDays;
    }

    /** Sets total allocated days. */
    public void setTotalAllocatedDays(Integer totalAllocatedDays) {
        this.totalAllocatedDays = totalAllocatedDays;
    }

    /** Returns used days. */
    public Integer getUsedDays() {
        return usedDays;
    }

    /** Sets used days. */
    public void setUsedDays(Integer usedDays) {
        this.usedDays = usedDays;
    }

    /** Returns remaining days. */
    public Integer getRemainingDays() {
        return remainingDays;
    }

    /** Sets remaining days. */
    public void setRemainingDays(Integer remainingDays) {
        this.remainingDays = remainingDays;
    }

    /** Returns year. */
    public Integer getYear() {
        return year;
    }

    /** Sets year. */
    public void setYear(Integer year) {
        this.year = year;
    }
}
