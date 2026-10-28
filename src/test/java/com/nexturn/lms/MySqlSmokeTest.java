package com.nexturn.lms;

import com.nexturn.lms.dto.ApprovalRequest;
import com.nexturn.lms.dto.LoginRequest;
import com.nexturn.lms.entity.Login;
import com.nexturn.lms.enums.LeaveStatus;
import com.nexturn.lms.enums.Role;
import com.nexturn.lms.repository.*;
import com.nexturn.lms.service.ApprovalService;
import com.nexturn.lms.service.LoginService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in check against a disposable MySQL schema loaded from schema.sql and local-fixture.sql. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named="MYSQL_SMOKE_URL", matches=".+")
class MySqlSmokeTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("MYSQL_SMOKE_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("MYSQL_SMOKE_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("MYSQL_SMOKE_PASSWORD", ""));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired LoginRepository accounts;
    @Autowired LoginService login;
    @Autowired ApprovalService approvals;
    @Autowired LeaveRepository leaves;
    @Autowired LeaveBalanceRepository balances;
    @Autowired NotificationRepository notifications;
    @Autowired AuditLogRepository audits;
    @Autowired ApprovalRepository decisions;

    /** Validates the hand-written schema, BCrypt/token mapping and complete decision against MySQL. */
    @Test @Transactional
    void schemaAuthenticationAndApprovalWorkOnMySql() throws Exception {
        accounts.saveAndFlush(new Login("smoke-manager", new BCryptPasswordEncoder(4).encode("Smoke-test-password"), Role.MANAGER,100));
        String token = login.login(new LoginRequest("smoke-manager","Smoke-test-password")).accessToken();
        Login manager = login.authenticate("Bearer " + token);
        var leave = leaves.findAll().stream().filter(l -> l.getStatus()==LeaveStatus.PENDING).findFirst().orElseThrow();
        approvals.decide(leave.getLeaveId(),new ApprovalRequest("MySQL smoke review"),true,manager);
        decisions.flush();
        assertThat(leaves.findById(leave.getLeaveId()).orElseThrow().getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(balances.findAll().getFirst().getRemainingDays()).isEqualTo(17);
        assertThat(decisions.count()).isOne();
        assertThat(notifications.count()).isOne();
        assertThat(audits.findAll().getFirst().getActorEmpId()).isEqualTo(100);
    }
}
