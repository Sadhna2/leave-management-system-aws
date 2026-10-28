package com.nexturn.lms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexturn.lms.Application;
import com.nexturn.lms.dto.*;
import com.nexturn.lms.entity.*;
import com.nexturn.lms.enums.*;
import com.nexturn.lms.exception.ApiException;
import com.nexturn.lms.repository.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises real transactions, authorization, HTTP validation, rollback and competing decisions. */
@SpringBootTest(classes=Application.class, properties="app.events.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApprovalServiceTest.FailureConfiguration.class)
class ApprovalServiceTest {
    @Autowired ApprovalService approvals;
    @Autowired LeaveRepository leaves;
    @Autowired EmployeeRepository employees;
    @Autowired LeaveBalanceRepository balances;
    @Autowired LeaveTypeRepository types;
    @Autowired ApprovalRepository decisions;
    @Autowired OutboxEventRepository outbox;
    @Autowired NotificationRepository notifications;
    @Autowired AuditLogRepository audits;
    @Autowired LoginRepository accounts;
    @Autowired AuthSessionRepository sessions;
    @Autowired LoginService login;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired FailpointNotifications notificationService;
    /** Injects a controlled persistence failure without instrumenting the production classes. */
    @TestConfiguration
    static class FailureConfiguration {
        @Bean @Primary
        FailpointNotifications failpointNotifications(NotificationRepository repository) {
            return new FailpointNotifications(repository);
        }
    }
    /** Normal notification persistence with a test-only failure switch for rollback verification. */
    static class FailpointNotifications extends NotificationServiceImpl {
        boolean fail;
        FailpointNotifications(NotificationRepository repository) { super(repository); }
        @Override
        public Notification create(Integer empId, NotificationType type, String message) {
            if (fail) throw new IllegalStateException("simulated persistence failure");
            return super.create(empId,type,message);
        }
    }
    private static final String PASSWORD="TestPassword-123";
    private static final String HASH=new BCryptPasswordEncoder(4).encode(PASSWORD);
    private Login manager;
    private Login employee;
    private LeaveType type;
    private Integer leaveId;

    /** Seeds only the shared records needed by this member's approval workflow. */
    @BeforeEach void seed() {
        notificationService.fail=false;
        sessions.deleteAll(); decisions.deleteAll(); notifications.deleteAll(); audits.deleteAll(); outbox.deleteAll();
        leaves.deleteAll(); balances.deleteAll(); accounts.deleteAll(); employees.deleteAll(); types.deleteAll();
        employees.save(person(100,"Manager",null));
        employees.save(person(101,"Other manager",null));
        employees.save(person(200,"Employee",100));
        manager=accounts.save(new Login("manager",HASH,Role.MANAGER,100));
        employee=accounts.save(new Login("employee",HASH,Role.EMPLOYEE,200));
        accounts.save(new Login("other",HASH,Role.MANAGER,101));
        type=new LeaveType(); type.setLeaveTypeName("Annual"); type.setDescription("Annual leave");
        type.setAnnualQuota(10); type.setAllowNegativeBalance(false); type=types.save(type);
        LeaveBalance b=new LeaveBalance(); b.setEmpId(200); b.setLeaveTypeId(type.getLeaveTypeId()); b.setYear(2026);
        b.setTotalAllocatedDays(10); b.setUsedDays(0); b.setRemainingDays(10); balances.save(b);
        leaveId=newLeave(3).getLeaveId();
    }
    private Employee person(int id,String name,Integer managerId) {
        Employee e=new Employee(); e.setEmpId(id); e.setEmpName(name); e.setEmail(id+"@example.test");
        e.setDesignation("Staff"); e.setGender("Other"); e.setAddress("Test address"); e.setManagerId(managerId); return e;
    }
    private LeaveApplication newLeave(int days) {
        LeaveApplication l=new LeaveApplication(); l.setEmpId(200); l.setLeaveTypeId(type.getLeaveTypeId());
        l.setStartDate(LocalDate.of(2026,11,2)); l.setEndDate(LocalDate.of(2026,11,20));
        l.setNoOfDaysRequested(days); l.setReason("Test leave"); return leaves.save(l);
    }
    private ApprovalRequest request() { return new ApprovalRequest("Approved after team review"); }
    private String token(String username) throws Exception { return login.login(new LoginRequest(username,PASSWORD)).accessToken(); }
    private void noSideEffects() {
        assertThat(leaves.findById(leaveId).orElseThrow().getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(balances.findAll().getFirst().getUsedDays()).isZero();
        assertThat(decisions.count()).isZero(); assertThat(notifications.count()).isZero(); assertThat(audits.count()).isZero();
        assertThat(outbox.count()).isZero();
    }
    @Test void approvalPersistsAllEffectsAndActor() {
        approvals.decide(leaveId,request(),true,manager);
        assertThat(leaves.findById(leaveId).orElseThrow().getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(balances.findAll().getFirst().getUsedDays()).isEqualTo(3);
        assertThat(balances.findAll().getFirst().getRemainingDays()).isEqualTo(7);
        assertThat(decisions.findAll().getFirst().getComment()).isEqualTo(request().comment());
        assertThat(decisions.findAll().getFirst().getManagerId()).isEqualTo(100);
        assertThat(notifications.findAll().getFirst().getType()).isEqualTo(NotificationType.LEAVE_APPROVED);
        assertThat(audits.findAll().getFirst().getActorEmpId()).isEqualTo(100);
        assertThat(outbox.count()).isOne();
    }
    @Test void rejectionDoesNotDeductBalance() {
        approvals.decide(leaveId,new ApprovalRequest("Team coverage unavailable"),false,manager);
        assertThat(leaves.findById(leaveId).orElseThrow().getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(balances.findAll().getFirst().getUsedDays()).isZero();
        assertThat(decisions.count()).isOne(); assertThat(audits.count()).isOne();
        assertThat(notifications.findAll().getFirst().getType()).isEqualTo(NotificationType.LEAVE_REJECTED);
    }
    @Test void unassignedManagerCannotDecide() {
        assertThatThrownBy(() -> approvals.decide(leaveId,request(),true,accounts.findById("other").orElseThrow()))
                .isInstanceOf(ApiException.class).hasMessageContaining("assigned manager"); noSideEffects();
    }
    @Test void employeeCannotApprove() {
        assertThatThrownBy(() -> approvals.decide(leaveId,request(),true,employee)).isInstanceOf(ApiException.class);
        noSideEffects();
    }
    @Test void managerCannotApproveOwnLeave() {
        Employee e=employees.findById(200).orElseThrow(); e.setManagerId(200); employees.save(e);
        Login self=new Login("self",HASH,Role.MANAGER,200);
        assertThatThrownBy(() -> approvals.decide(leaveId,request(),true,self)).isInstanceOf(ApiException.class); noSideEffects();
    }
    @Test void finalDecisionCannotBeRepeated() {
        approvals.decide(leaveId,request(),true,manager);
        assertThatThrownBy(() -> approvals.decide(leaveId,request(),false,manager)).isInstanceOf(ApiException.class);
        assertThat(decisions.count()).isOne(); assertThat(notifications.count()).isOne();
        assertThat(balances.findAll().getFirst().getUsedDays()).isEqualTo(3);
    }
    @Test void cancelledLeaveCannotBeDecided() {
        LeaveApplication l=leaves.findById(leaveId).orElseThrow(); l.setStatus(LeaveStatus.CANCELLED); leaves.save(l);
        assertThatThrownBy(() -> approvals.decide(leaveId,request(),true,manager)).isInstanceOf(ApiException.class);
        assertThat(decisions.count()).isZero();
    }
    @Test void insufficientBalanceRejectsWithoutEffects() {
        LeaveApplication l=leaves.findById(leaveId).orElseThrow(); l.setNoOfDaysRequested(11); leaves.save(l);
        assertThatThrownBy(() -> approvals.decide(leaveId,request(),true,manager)).hasMessageContaining("Insufficient"); noSideEffects();
    }
    @Test void policyCanExplicitlyPermitNegativeBalance() {
        type.setAllowNegativeBalance(true); types.save(type);
        LeaveApplication l=leaves.findById(leaveId).orElseThrow(); l.setNoOfDaysRequested(11); leaves.save(l);
        approvals.decide(leaveId,request(),true,manager);
        assertThat(balances.findAll().getFirst().getRemainingDays()).isEqualTo(-1);
    }
    @Test void missingBalanceNeverGetsSilentlyCreated() {
        balances.deleteAll();
        assertThatThrownBy(() -> approvals.decide(leaveId,request(),true,manager)).hasMessageContaining("not initialized");
        assertThat(decisions.count()).isZero(); assertThat(balances.count()).isZero();
    }
    @Test void crossYearApprovalNeedsUpstreamSplit() {
        LeaveApplication l=leaves.findById(leaveId).orElseThrow(); l.setEndDate(LocalDate.of(2027,1,2)); leaves.save(l);
        assertThatThrownBy(() -> approvals.decide(leaveId,request(),true,manager)).hasMessageContaining("one balance year"); noSideEffects();
    }
    @Test void zeroDaysCannotBeApproved() {
        LeaveApplication l=leaves.findById(leaveId).orElseThrow(); l.setNoOfDaysRequested(0); leaves.save(l);
        assertThatThrownBy(() -> approvals.decide(leaveId,request(),true,manager)).isInstanceOf(ApiException.class); noSideEffects();
    }
    @Test void notificationFailureRollsBackDecisionBalanceAndAudit() {
        notificationService.fail=true;
        assertThatThrownBy(() -> approvals.decide(leaveId,request(),true,manager)).isInstanceOf(IllegalStateException.class);
        noSideEffects();
    }
    @Test void competingDecisionsCreateOneDecision() {
        ExecutorService executor=Executors.newFixedThreadPool(2);
        CompletableFuture<Boolean> a=CompletableFuture.supplyAsync(() -> approvals.decide(leaveId,request(),true,manager),executor)
                .handle((result,error) -> error==null);
        CompletableFuture<Boolean> b=CompletableFuture.supplyAsync(() -> approvals.decide(leaveId,request(),true,manager),executor)
                .handle((result,error) -> error==null);
        List<Boolean> results=List.of(a.join(),b.join()); executor.shutdown();
        assertThat(results).containsExactlyInAnyOrder(true,false);
        assertThat(decisions.count()).isOne(); assertThat(notifications.count()).isOne();
        assertThat(balances.findAll().getFirst().getUsedDays()).isEqualTo(3);
    }
    @Test void differentLeavesCannotOverspendSharedBalance() {
        LeaveApplication first=leaves.findById(leaveId).orElseThrow(); first.setNoOfDaysRequested(7); leaves.save(first);
        Integer secondId=newLeave(7).getLeaveId();
        ExecutorService executor=Executors.newFixedThreadPool(2);
        CompletableFuture<Boolean> a=CompletableFuture.supplyAsync(() -> approvals.decide(leaveId,request(),true,manager),executor)
                .handle((result,error) -> error==null);
        CompletableFuture<Boolean> b=CompletableFuture.supplyAsync(() -> approvals.decide(secondId,request(),true,manager),executor)
                .handle((result,error) -> error==null);
        List<Boolean> results=List.of(a.join(),b.join()); executor.shutdown();
        assertThat(results).containsExactlyInAnyOrder(true,false);
        assertThat(decisions.count()).isOne(); assertThat(balances.findAll().getFirst().getRemainingDays()).isEqualTo(3);
    }
    @Test void unauthenticatedRequestIsJson401() throws Exception {
        mvc.perform(get("/api/manager/leaves/pending")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }
    @Test void loginReturnsTokenAndNoPassword() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
            .content(json.writeValueAsString(new LoginRequest("manager",PASSWORD))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.empId").value(100)).andExpect(jsonPath("$.password").doesNotExist());
        assertThat(sessions.findAll().getFirst().getTokenHash()).hasSize(64);
    }
    @Test void wrongPasswordIs401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
            .content(json.writeValueAsString(new LoginRequest("manager","wrong"))))
            .andExpect(status().isUnauthorized());
    }
    @Test void invalidOrMissingCommentIs400AndHasNoEffects() throws Exception {
        mvc.perform(post("/api/manager/leave/"+leaveId+"/approve").header("Authorization","Bearer "+token("manager"))
            .contentType("application/json").content(json.writeValueAsString(new ApprovalRequest(" "))))
            .andExpect(status().isBadRequest()); noSideEffects();
    }
    @Test void clientCannotSpoofManagerIdentity() throws Exception {
        mvc.perform(post("/api/manager/leave/"+leaveId+"/approve").header("Authorization","Bearer "+token("other"))
            .contentType("application/json").content(json.writeValueAsString(Map.of("managerId",100,"comment","spoof"))))
            .andExpect(status().isBadRequest()); noSideEffects();
    }
    @Test void validHttpDecisionWorks() throws Exception {
        mvc.perform(post("/api/manager/leave/"+leaveId+"/approve").header("Authorization","Bearer "+token("manager"))
            .contentType("application/json").content(json.writeValueAsString(request())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
    }
    @Test void logoutRevokesToken() throws Exception {
        String header="Bearer "+token("manager");
        mvc.perform(post("/api/auth/logout").header("Authorization",header)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization",header)).andExpect(status().isUnauthorized());
    }
    @Test void expiredTokenIsRejected() throws Exception {
        String accessToken=token("manager"); AuthSession s=sessions.findAll().getFirst();
        sessions.save(new AuthSession(s.getTokenHash(),s.getUsername(),Instant.now().minusSeconds(1)));
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+accessToken)).andExpect(status().isUnauthorized());
    }
    @Test void notificationAndAuditReadsEnforceOwnership() throws Exception {
        approvals.decide(leaveId,request(),true,manager);
        String employeeHeader="Bearer "+token("employee"); String otherHeader="Bearer "+token("other");
        Long id=notifications.findAll().getFirst().getNotificationId();
        mvc.perform(get("/api/notifications").header("Authorization",employeeHeader)).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(patch("/api/notifications/"+id+"/read").header("Authorization",otherHeader)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/notifications/"+id+"/read").header("Authorization",employeeHeader))
            .andExpect(status().isOk()).andExpect(jsonPath("$.readFlag").value(true));
        mvc.perform(get("/api/audit-logs/employee/200").header("Authorization",otherHeader)).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit-logs/employee/200").header("Authorization",employeeHeader)).andExpect(status().isOk());
    }
    @Test void managerQueueContainsOnlyAssignedPendingLeaves() throws Exception {
        mvc.perform(get("/api/manager/leaves/pending").header("Authorization","Bearer "+token("manager")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/manager/leaves/pending").header("Authorization","Bearer "+token("other")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/manager/leaves/pending").header("Authorization","Bearer "+token("employee")))
            .andExpect(status().isForbidden());
    }
}
