package com.collegecopilot.assignment;

import com.collegecopilot.assignment.dto.*;
import com.collegecopilot.auth.AuthException;
import com.collegecopilot.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public ResponseEntity<AssignmentListResponse> getAssignments(
            @RequestParam(value = "status", required = false) String status) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        AssignmentListResponse response = assignmentService.getAssignments(principal.getId(), status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssignmentSingleResponse> getAssignmentById(
            @PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        AssignmentDto assignment = assignmentService.getAssignmentById(principal.getId(), id);
        return ResponseEntity.ok(new AssignmentSingleResponse(true, assignment));
    }

    @PostMapping
    public ResponseEntity<AssignmentSingleResponse> createAssignment(
            @Valid @RequestBody AssignmentCreateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        AssignmentDto created = assignmentService.createAssignment(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AssignmentSingleResponse(true, created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssignmentSingleResponse> updateAssignment(
            @PathVariable("id") String id,
            @RequestBody AssignmentUpdateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        AssignmentDto updated = assignmentService.updateAssignment(principal.getId(), id, request);
        return ResponseEntity.ok(new AssignmentSingleResponse(true, updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AssignmentDeleteResponse> deleteAssignment(
            @PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        AssignmentDeleteResponse response = assignmentService.deleteAssignment(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/advisor")
    public ResponseEntity<AssignmentAdvisorResponse> runAssignmentAdvisor() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        AdvisorDataDto data = assignmentService.runAssignmentAdvisor(principal.getId());
        return ResponseEntity.ok(new AssignmentAdvisorResponse(true, data));
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        return principal;
    }
}
