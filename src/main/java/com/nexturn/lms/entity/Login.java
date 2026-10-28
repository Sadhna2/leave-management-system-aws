package com.nexturn.lms.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nexturn.lms.enums.Role;
import jakarta.persistence.*;
/** Provisioned account explicitly linked to an employee; passwords contain BCrypt hashes only. */
@Entity
@Table(name = "login")
public class Login {
    @Id @Column(length = 50) private String username;
    @JsonIgnore @Column(nullable = false, length = 255) private String password;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role;
    @Column(nullable = false, unique = true) private Integer empId;
    public Login() { }
    public Login(String username, String password, Role role, Integer empId) {
        this.username = username; this.password = password; this.role = role; this.empId = empId;
    }
    /** Returns username. */
    public String getUsername() { return username; }
    /** Returns password. */
    public String getPassword() { return password; }
    /** Returns role. */
    public Role getRole() { return role; }
    /** Returns emp id. */
    public Integer getEmpId() { return empId; }
}
