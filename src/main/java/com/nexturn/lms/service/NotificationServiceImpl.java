package com.nexturn.lms.service;

import com.nexturn.lms.entity.Notification;
import com.nexturn.lms.enums.NotificationType;
import com.nexturn.lms.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

/** Creates employee notifications inside the calling approval transaction. */

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;

    public NotificationServiceImpl(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notification create(
            Integer empId,
            NotificationType type,
            String message) {

        Notification n = new Notification();
        n.setEmpId(empId);
        n.setType(type);
        n.setMessage(message);

        return repository.save(n);
    }

    @Override
    /** Returns by employee. */
    public List<Notification> getByEmployee(Integer empId) {
        return repository.findByEmpIdOrderByCreatedAtDesc(empId);
    }
}
