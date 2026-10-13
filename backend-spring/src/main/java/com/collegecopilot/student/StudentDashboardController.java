package com.collegecopilot.student;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.student.dto.StudentDashboardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
public class StudentDashboardController {

    private final StudentDashboardService dashboardService;

    public StudentDashboardController(StudentDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<StudentDashboardResponse> getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }

        StudentDashboardResponse response = dashboardService.getStudentDashboard(principal.getId());
        return ResponseEntity.ok(response);
    }
}
