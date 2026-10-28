package com.nexturn.lms.entity;

import com.nexturn.lms.enums.NotificationType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Persistent in-app message addressed to one employee. */

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Column(nullable = false)
    private Integer empId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private Boolean readFlag = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {
    }

    /** Returns notification id. */
    public Long getNotificationId() {
        return notificationId;
    }

    /** Returns emp id. */
    public Integer getEmpId() {
        return empId;
    }

    /** Sets emp id. */
    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    /** Returns type. */
    public NotificationType getType() {
        return type;
    }

    /** Sets type. */
    public void setType(NotificationType type) {
        this.type = type;
    }

    /** Returns message. */
    public String getMessage() {
        return message;
    }

    /** Sets message. */
    public void setMessage(String message) {
        this.message = message;
    }

    /** Returns read flag. */
    public Boolean getReadFlag() {
        return readFlag;
    }

    /** Sets read flag. */
    public void setReadFlag(Boolean readFlag) {
        this.readFlag = readFlag;
    }

    /** Returns created at. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
