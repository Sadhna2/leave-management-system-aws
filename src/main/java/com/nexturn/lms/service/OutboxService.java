package com.nexturn.lms.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexturn.lms.entity.*;
import com.nexturn.lms.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
/** Enqueues cloud events locally so SNS outages cannot invalidate a committed manager decision. */
@Service
public class OutboxService {
    private final OutboxEventRepository events;
    private final ObjectMapper json;
    private final boolean enabled;
    public OutboxService(OutboxEventRepository events,ObjectMapper json,@Value("${app.events.enabled:false}") boolean enabled) {
        this.events=events; this.json=json; this.enabled=enabled;
    }
    /** Runs inside ApprovalService's transaction; no AWS call is made on the request thread. */
    public void enqueue(LeaveApplication leave,Login actor,String comment) {
        if (!enabled) return;
        String id=UUID.randomUUID().toString();
        Instant now=Instant.now();
        String payload=json.valueToTree(Map.of("eventId",id,"eventType","LEAVE_"+leave.getStatus().name(),
                "leaveId",leave.getLeaveId(),"employeeId",leave.getEmpId(),"managerId",actor.getEmpId(),
                "comment",comment,"occurredAt",now.toString())).toString();
        events.save(new OutboxEvent(id,payload,now));
    }
}
