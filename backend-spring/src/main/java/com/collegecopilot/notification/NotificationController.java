package com.collegecopilot.notification;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.notification.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        return principal;
    }

    // GET /api/notifications
    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(notificationService.getNotifications(status, type, principal));
    }

    // GET /api/notifications/unread-count
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(notificationService.getUnreadCount(principal));
    }

    // PUT /api/notifications/read-all
    @PutMapping("/read-all")
    public ResponseEntity<NotificationReadAllResponse> markAllAsRead() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(notificationService.markAllAsRead(principal));
    }

    // PUT /api/notifications/{id}/read
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(notificationService.markAsRead(id, principal));
    }

    // DELETE /api/notifications/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<NotificationDeleteResponse> deleteNotification(@PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(notificationService.deleteNotification(id, principal));
    }

    // POST /api/notifications/generate
    @PostMapping("/generate")
    public ResponseEntity<NotificationGenerateResponse> generateNotifications(
            @RequestBody(required = false) NotificationGenerateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(notificationService.generateNotifications(request, principal));
    }
}
