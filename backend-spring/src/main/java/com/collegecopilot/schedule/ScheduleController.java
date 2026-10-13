package com.collegecopilot.schedule;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.schedule.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Autowired
    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        return principal;
    }

    // GET /api/schedule
    @GetMapping
    public ResponseEntity<ScheduleListResponse> getSchedule() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(scheduleService.getSchedule(principal));
    }

    // POST /api/schedule
    @PostMapping
    public ResponseEntity<ScheduleSingleResponse> addScheduleItem(@RequestBody ScheduleCreateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        ScheduleSingleResponse response = scheduleService.addScheduleItem(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // DELETE /api/schedule/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<ScheduleDeleteResponse> deleteScheduleItem(@PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(scheduleService.deleteScheduleItem(id, principal));
    }
}
