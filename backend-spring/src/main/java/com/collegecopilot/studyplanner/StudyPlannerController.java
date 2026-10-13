package com.collegecopilot.studyplanner;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.studyplanner.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/planner")
public class StudyPlannerController {

    private final StudyPlannerService studyPlannerService;

    public StudyPlannerController(StudyPlannerService studyPlannerService) {
        this.studyPlannerService = studyPlannerService;
    }

    @GetMapping
    public ResponseEntity<PlannerGetResponse> getStudyPlan() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String studentIdOrUserId = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        PlannerGetResponse response = studyPlannerService.getStudyPlan(studentIdOrUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate")
    public ResponseEntity<PlannerGenerateResponse> generateStudyPlan(
            @RequestBody(required = false) PlannerGenerateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String studentIdOrUserId = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        PlannerGenerateRequest req = request != null ? request : new PlannerGenerateRequest();
        PlannerGenerateResponse response = studyPlannerService.generateStudyPlan(studentIdOrUserId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/session/{id}")
    public ResponseEntity<SessionUpdateResponse> updateSessionStatus(
            @PathVariable("id") String id,
            @RequestBody SessionStatusUpdateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String studentIdOrUserId = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        SessionUpdateResponse response = studyPlannerService.updateSessionStatus(studentIdOrUserId, id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/replan")
    public ResponseEntity<PlannerReplanResponse> replanSchedule() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String studentIdOrUserId = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        PlannerReplanResponse response = studyPlannerService.replanSchedule(studentIdOrUserId);
        return ResponseEntity.ok(response);
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        return principal;
    }
}
