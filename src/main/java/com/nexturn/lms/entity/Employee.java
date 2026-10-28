package com.nexturn.lms.entity;

import jakarta.persistence.*;

/** Shared employee record used to verify manager assignment; employee management belongs to the employee module. */

@Entity
@Table(name = "employee")
public class Employee {

    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer empId;

    @Column(nullable = false, length = 100)
    private String empName;

    @Column(unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 10)
    private String phoneNumber;

    @Column(nullable = false, length = 50)
    private String designation;

    private Integer deptId;

    private Integer managerId;

    @Column(nullable = false, length = 10)
    private String gender;

    @Column(nullable = false, length = 200)
    private String address;

    public Employee() {
    }

    /** Returns emp id. */
    public Integer getEmpId() {
        return empId;
    }

    /** Sets emp id. */
    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    /** Returns emp name. */
    public String getEmpName() {
        return empName;
    }

    /** Sets emp name. */
    public void setEmpName(String empName) {
        this.empName = empName;
    }

    /** Returns email. */
    public String getEmail() {
        return email;
    }

    /** Sets email. */
    public void setEmail(String email) {
        this.email = email;
    }

    /** Returns phone number. */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /** Sets phone number. */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /** Returns designation. */
    public String getDesignation() {
        return designation;
    }

    /** Sets designation. */
    public void setDesignation(String designation) {
        this.designation = designation;
    }

    /** Returns dept id. */
    public Integer getDeptId() {
        return deptId;
    }

    /** Sets dept id. */
    public void setDeptId(Integer deptId) {
        this.deptId = deptId;
    }

    /** Returns manager id. */
    public Integer getManagerId() {
        return managerId;
    }

    /** Sets manager id. */
    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    /** Returns gender. */
    public String getGender() {
        return gender;
    }

    /** Sets gender. */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /** Returns address. */
    public String getAddress() {
        return address;
    }

    /** Sets address. */
    public void setAddress(String address) {
        this.address = address;
    }
}
