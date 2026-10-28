package com.nexturn.lms.entity;
import jakarta.persistence.*;
import java.time.Instant;
/** Durable notification event committed alongside the approval; delivery is retried separately. */
@Entity @Table(name="outbox_event")
public class OutboxEvent {
    @Id @Column(length=36) private String eventId;
    @Column(nullable=false, columnDefinition="text") private String payload;
    @Column(nullable=false) private Instant createdAt;
    private Instant publishedAt;
    protected OutboxEvent() { }
    public OutboxEvent(String eventId, String payload, Instant createdAt) {
        this.eventId=eventId; this.payload=payload; this.createdAt=createdAt;
    }
}
