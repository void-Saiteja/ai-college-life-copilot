package com.collegecopilot.notification;

import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.notification.dto.*;
import com.collegecopilot.security.JwtAuthenticationFilter;
import com.collegecopilot.security.JwtTokenProvider;
import com.collegecopilot.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private JwtTokenProvider tokenProvider;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        tokenA = tokenProvider.generateToken("u-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);
        tokenB = tokenProvider.generateToken("u-2", "Bob Student", "bob@student.edu", "STUDENT", "std-2", null);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/notifications returns 200 with notification list when authenticated")
    void getNotifications_authenticated_returns200() throws Exception {
        NotificationItemDto item = new NotificationItemDto(
                "notif-1", "std-1", "u-1", "Assignment Due", "CS 101 tomorrow",
                "ASSIGNMENT", "HIGH", false, "2026-03-30T10:00:00"
        );
        NotificationListData listData = new NotificationListData(List.of(item), 1, 1);
        when(notificationService.getNotifications(any(), any(), any(UserPrincipal.class)))
                .thenReturn(new NotificationListResponse(true, listData));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(1))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.notifications[0].id").value("notif-1"))
                .andExpect(jsonPath("$.data.notifications[0].title").value("Assignment Due"))
                .andExpect(jsonPath("$.data.notifications[0].priority").value("HIGH"));
    }

    @Test
    @DisplayName("GET /api/notifications returns 401 when unauthenticated")
    void getNotifications_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count returns 200 with unread count")
    void getUnreadCount_authenticated_returns200() throws Exception {
        when(notificationService.getUnreadCount(any(UserPrincipal.class)))
                .thenReturn(new UnreadCountResponse(true, 5));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count returns 401 when unauthenticated")
    void getUnreadCount_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/notifications/{id}/read marks notification as read for owner")
    void markAsRead_owner_returns200() throws Exception {
        NotificationItemDto item = new NotificationItemDto(
                "notif-1", "std-1", "u-1", "Assignment Due", "CS 101 tomorrow",
                "ASSIGNMENT", "HIGH", true, "2026-03-30T10:00:00"
        );
        when(notificationService.markAsRead(eq("notif-1"), any(UserPrincipal.class)))
                .thenReturn(new NotificationResponse(true, "Notification marked as read", item));

        mockMvc.perform(put("/api/notifications/notif-1/read")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification marked as read"))
                .andExpect(jsonPath("$.data.is_read").value(true));
    }

    @Test
    @DisplayName("PUT /api/notifications/{id}/read returns 403 when user does not own notification")
    void markAsRead_nonOwner_returns403() throws Exception {
        when(notificationService.markAsRead(eq("notif-1"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(put("/api/notifications/notif-1/read")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/notifications/{id}/read returns 404 when notification does not exist")
    void markAsRead_notFound_returns404() throws Exception {
        when(notificationService.markAsRead(eq("notif-999"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        mockMvc.perform(put("/api/notifications/notif-999/read")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/notifications/{id}/read returns 401 when unauthenticated")
    void markAsRead_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/notifications/notif-1/read"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/notifications/read-all marks all notifications read")
    void markAllAsRead_authenticated_returns200() throws Exception {
        when(notificationService.markAllAsRead(any(UserPrincipal.class)))
                .thenReturn(new NotificationReadAllResponse(true, "All notifications marked as read", 3));

        mockMvc.perform(put("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All notifications marked as read"))
                .andExpect(jsonPath("$.updatedCount").value(3));
    }

    @Test
    @DisplayName("PUT /api/notifications/read-all returns 401 when unauthenticated")
    void markAllAsRead_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} deletes notification for owner")
    void deleteNotification_owner_returns200() throws Exception {
        when(notificationService.deleteNotification(eq("notif-1"), any(UserPrincipal.class)))
                .thenReturn(new NotificationDeleteResponse(true, "Notification deleted successfully"));

        mockMvc.perform(delete("/api/notifications/notif-1")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} returns 403 when user does not own notification")
    void deleteNotification_nonOwner_returns403() throws Exception {
        when(notificationService.deleteNotification(eq("notif-1"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(delete("/api/notifications/notif-1")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} returns 401 when unauthenticated")
    void deleteNotification_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/notifications/notif-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/notifications/generate generates smart notifications")
    void generateNotifications_authenticated_returns200() throws Exception {
        NotificationGenerateData data = new NotificationGenerateData(2, 5, 3);
        when(notificationService.generateNotifications(any(NotificationGenerateRequest.class), any(UserPrincipal.class)))
                .thenReturn(new NotificationGenerateResponse(true, "Generated 2 new notification(s)", data, 2));

        mockMvc.perform(post("/api/notifications/generate")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"student_id\":\"std-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.data.createdCount").value(2))
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    @Test
    @DisplayName("POST /api/notifications/generate returns 403 when trying to generate for different student")
    void generateNotifications_crossStudent_returns403() throws Exception {
        when(notificationService.generateNotifications(any(NotificationGenerateRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: cannot generate notifications for another student"));

        mockMvc.perform(post("/api/notifications/generate")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"student_id\":\"std-2\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/notifications/generate returns 401 when unauthenticated")
    void generateNotifications_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/notifications/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"student_id\":\"std-1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
