package com.nexturn.lms.controller;
import com.nexturn.lms.entity.*;
import com.nexturn.lms.dto.PageResponse;
import com.nexturn.lms.exception.ApiException;
import com.nexturn.lms.repository.NotificationRepository;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
/** Lets authenticated employees list and mark only their own in-app notifications. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository notifications;
    public NotificationController(NotificationRepository notifications) { this.notifications = notifications; }
    @GetMapping
    public PageResponse<Notification> list(@RequestAttribute("authenticatedUser") Login actor,
            @PageableDefault(size=20, sort="createdAt", direction=Sort.Direction.DESC) Pageable page) {
        return PageResponse.of(notifications.findByEmpId(actor.getEmpId(), page));
    }
    @PatchMapping("/{id}/read") @Transactional
    public Notification read(@PathVariable Long id, @RequestAttribute("authenticatedUser") Login actor) {
        Notification notification = notifications.findByNotificationIdAndEmpId(id, actor.getEmpId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setReadFlag(true);
        return notifications.save(notification);
    }
}
