package com.collegecopilot.quiz;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.quiz.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/quiz", "/api/quizzes"})
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/generate")
    public ResponseEntity<QuizGenerateResponse> generateQuiz(
            @RequestBody(required = false) QuizGenerateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String studentIdOrUserId = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        QuizGenerateRequest req = request != null ? request : new QuizGenerateRequest();
        QuizGenerateResponse response = quizService.generateQuiz(studentIdOrUserId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<QuizHistoryResponse> getQuizzes() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String studentIdOrUserId = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        QuizHistoryResponse response = quizService.getQuizzesAndHistory(studentIdOrUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<QuizHistoryResponse> getQuizHistory() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String studentIdOrUserId = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        QuizHistoryResponse response = quizService.getQuizzesAndHistory(studentIdOrUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizDetailResponse> getQuizById(@PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String studentIdOrUserId = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        QuizDetailResponse response = quizService.getQuizById(studentIdOrUserId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<QuizSubmitResponse> submitQuizWithId(
            @PathVariable("id") String id,
            @RequestBody QuizSubmitRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String studentIdOrUserId = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        QuizSubmitResponse response = quizService.submitQuiz(studentIdOrUserId, id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/submit")
    public ResponseEntity<QuizSubmitResponse> submitQuizDirect(
            @RequestBody QuizSubmitRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String studentIdOrUserId = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        QuizSubmitResponse response = quizService.submitQuiz(studentIdOrUserId, null, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<QuizDeleteResponse> deleteQuiz(@PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String studentIdOrUserId = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        QuizDeleteResponse response = quizService.deleteQuiz(studentIdOrUserId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/advisor")
    public ResponseEntity<QuizAdvisorResponse> runQuizAdvisor() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String studentIdOrUserId = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        QuizAdvisorResponse response = quizService.runQuizAdvisor(studentIdOrUserId);
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
