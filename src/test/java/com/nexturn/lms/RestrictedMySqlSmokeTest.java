package com.nexturn.lms;
import com.nexturn.lms.dto.*;
import com.nexturn.lms.repository.*;
import com.nexturn.lms.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

/** Regression check for MySQL shared assignment locks using the deployment's restricted application grants. */
@SpringBootTest(properties="app.events.enabled=true")
@EnabledIfEnvironmentVariable(named="MYSQL_RESTRICTED_URL",matches="jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):.+")
class RestrictedMySqlSmokeTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",()->System.getenv("MYSQL_RESTRICTED_URL"));
        registry.add("spring.datasource.username",()->"lms_app");
        registry.add("spring.datasource.password",()->System.getenv("MYSQL_RESTRICTED_PASSWORD"));
        registry.add("spring.jpa.hibernate.ddl-auto",()->"validate");
    }
    @Autowired LoginService login;
    @Autowired ApprovalService approvals;
    @Autowired LeaveBalanceRepository balances;
    @Autowired ApprovalRepository decisions;
    @Autowired OutboxEventRepository outbox;
    /** Uses the disposable local fixture; rollback preserves the pending request for browser checks. */
    @Test @Transactional void selectOnlyEmployeeGrantsSupportAnApproval() throws Exception {
        var session=login.login(new LoginRequest("manager",System.getenv("MYSQL_RESTRICTED_MANAGER_PASSWORD")));
        var manager=login.authenticate("Bearer "+session.accessToken());
        var before=balances.findAll().get(0).getUsedDays();
        var leave=approvals.pending(manager,org.springframework.data.domain.PageRequest.of(0,20)).getContent().get(0);
        approvals.decide(leave.getLeaveId(),new ApprovalRequest("Restricted MySQL regression check"),true,manager);
        decisions.flush();
        assertThat(balances.findAll().get(0).getUsedDays()).isEqualTo(before+leave.getNoOfDaysRequested());
        assertThat(outbox.count()).isOne();
    }
}
