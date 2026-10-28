package com.nexturn.lms.entity;

import jakarta.persistence.*;

/** Shared leave policy controlling whether approval may create a negative balance. */

@Entity
@Table(name = "leave_type")
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer leaveTypeId;

    @Column(nullable = false, length = 50)
    private String leaveTypeName;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false)
    private Integer annualQuota;

    @Column(nullable = false)
    private Boolean allowNegativeBalance = false;

    public LeaveType() {
    }

    /** Returns leave type id. */
    public Integer getLeaveTypeId() {
        return leaveTypeId;
    }

    /** Sets leave type id. */
    public void setLeaveTypeId(Integer leaveTypeId) {
        this.leaveTypeId = leaveTypeId;
    }

    /** Returns leave type name. */
    public String getLeaveTypeName() {
        return leaveTypeName;
    }

    /** Sets leave type name. */
    public void setLeaveTypeName(String leaveTypeName) {
        this.leaveTypeName = leaveTypeName;
    }

    /** Returns description. */
    public String getDescription() {
        return description;
    }

    /** Sets description. */
    public void setDescription(String description) {
        this.description = description;
    }

    /** Returns annual quota. */
    public Integer getAnnualQuota() {
        return annualQuota;
    }

    /** Sets annual quota. */
    public void setAnnualQuota(Integer annualQuota) {
        this.annualQuota = annualQuota;
    }

    /** Returns allow negative balance. */
    public Boolean getAllowNegativeBalance() {
        return allowNegativeBalance;
    }

    /** Sets allow negative balance. */
    public void setAllowNegativeBalance(Boolean allowNegativeBalance) {
        this.allowNegativeBalance = allowNegativeBalance;
    }
}
