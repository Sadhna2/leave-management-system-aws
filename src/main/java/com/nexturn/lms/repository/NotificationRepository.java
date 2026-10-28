package com.nexturn.lms.repository;

import com.nexturn.lms.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Persistence access for notification repository. */

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    org.springframework.data.domain.Page<Notification> findByEmpId(Integer empId, org.springframework.data.domain.Pageable page);
    java.util.Optional<Notification> findByNotificationIdAndEmpId(Long notificationId, Integer empId);

    List<Notification> findByEmpIdOrderByCreatedAtDesc(Integer empId);
}
