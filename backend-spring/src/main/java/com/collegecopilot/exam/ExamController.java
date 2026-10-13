package com.collegecopilot.exam;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.exam.dto.*;
import com.collegecopilot.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public ResponseEntity<ExamListResponse> getExams(
            @RequestParam(value = "status", required = false) String status) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        ExamListResponse response = examService.getExams(principal.getId(), status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamSingleResponse> getExamById(
            @PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        ExamDto exam = examService.getExamById(principal.getId(), id);
        return ResponseEntity.ok(new ExamSingleResponse(true, exam));
    }

    @PostMapping
    public ResponseEntity<ExamSingleResponse> createExam(
            @Valid @RequestBody ExamCreateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        ExamDto created = examService.createExam(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ExamSingleResponse(true, created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExamSingleResponse> updateExam(
            @PathVariable("id") String id,
            @RequestBody ExamUpdateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        ExamDto updated = examService.updateExam(principal.getId(), id, request);
        return ResponseEntity.ok(new ExamSingleResponse(true, updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ExamDeleteResponse> deleteExam(
            @PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        ExamDeleteResponse response = examService.deleteExam(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/advisor")
    public ResponseEntity<ExamAdvisorResponse> runExamAdvisor() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        ExamAdvisorDataDto data = examService.runExamAdvisor(principal.getId());
        return ResponseEntity.ok(new ExamAdvisorResponse(true, data));
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        return principal;
    }
}
