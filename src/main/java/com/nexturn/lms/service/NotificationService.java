package com.nexturn.lms.service;

import com.nexturn.lms.entity.Notification;
import com.nexturn.lms.enums.NotificationType;
import java.util.List;

/** Contract for creating and reading persisted in-app notifications. */

public interface NotificationService {
    Notification create(Integer empId, NotificationType type, String message);
    List<Notification> getByEmployee(Integer empId);
}
