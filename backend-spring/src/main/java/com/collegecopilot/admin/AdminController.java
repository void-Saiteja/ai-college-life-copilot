package com.collegecopilot.admin;

import com.collegecopilot.admin.dto.*;
import com.collegecopilot.auth.AuthException;
import com.collegecopilot.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    private UserPrincipal getAuthenticatedAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Admin authentication required");
        }
        if (!"ADMIN".equalsIgnoreCase(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: Access restricted to [ADMIN]");
        }
        return principal;
    }

    /**
     * GET /api/admin/dashboard
     * Returns deterministic system overview statistics.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<AdminResponse<AdminDashboardDataDto>> getDashboard() {
        getAuthenticatedAdmin();
        AdminDashboardDataDto stats = adminService.getDashboardStats();
        return ResponseEntity.ok(AdminResponse.ok(stats));
    }

    /**
     * GET /api/admin/stats
     * Returns preserved summary stats, announcements, and FAQs.
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminResponse<AdminStatsDataDto>> getStats() {
        getAuthenticatedAdmin();
        AdminStatsDataDto stats = adminService.getAdminStats();
        return ResponseEntity.ok(AdminResponse.ok(stats));
    }

    /**
     * GET /api/admin/students
     * Returns student list with academic metrics, risk indicators, and query filters.
     */
    @GetMapping("/students")
    public ResponseEntity<AdminResponse<List<AdminStudentSummaryDto>>> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String attendanceRisk) {
        getAuthenticatedAdmin();
        List<AdminStudentSummaryDto> list = adminService.getStudentsList(search, semester, department, attendanceRisk);
        return ResponseEntity.ok(AdminResponse.ok(list, list.size()));
    }

    /**
     * GET /api/admin/students/{studentId}
     * Returns detailed administrative drill-down for a selected student.
     */
    @GetMapping("/students/{studentId}")
    public ResponseEntity<AdminResponse<AdminStudentDetailDto>> getStudentById(
            @PathVariable String studentId) {
        getAuthenticatedAdmin();
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid student ID is required");
        }

        AdminStudentDetailDto detail = adminService.getStudentDetail(studentId.trim());
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student with ID '" + studentId + "' not found");
        }

        return ResponseEntity.ok(AdminResponse.ok(detail));
    }

    /**
     * GET /api/admin/risks
     * Returns categorized academic risks: attendance, assignment, exam, quiz, career.
     */
    @GetMapping("/risks")
    public ResponseEntity<AdminResponse<AdminRisksDataDto>> getAcademicRisks() {
        getAuthenticatedAdmin();
        AdminRisksDataDto risks = adminService.getAcademicRisks();
        return ResponseEntity.ok(AdminResponse.ok(risks));
    }

    /**
     * POST /api/admin/insights
     * Generates AI Executive Observations & Actionable Insights based on authoritative metrics.
     */
    @PostMapping("/insights")
    public ResponseEntity<AdminResponse<AdminInsightsDataDto>> getAdminInsights() {
        getAuthenticatedAdmin();
        AdminInsightsDataDto insights = adminService.getAdminInsights();
        return ResponseEntity.ok(AdminResponse.ok(insights));
    }

    /**
     * GET /api/admin/documents
     * Returns documents with chunk count and embedding status.
     */
    @GetMapping("/documents")
    public ResponseEntity<AdminResponse<List<AdminDocumentDto>>> getAdminDocuments() {
        getAuthenticatedAdmin();
        List<AdminDocumentDto> docs = adminService.getAdminDocuments();
        return ResponseEntity.ok(AdminResponse.ok(docs));
    }

    /**
     * POST /api/admin/subjects
     * Creates a new course subject.
     */
    @PostMapping("/subjects")
    public ResponseEntity<AdminResponse<AdminSubjectDto>> createSubject(
            @Valid @RequestBody CreateSubjectRequest request) {
        getAuthenticatedAdmin();
        AdminSubjectDto subject = adminService.createSubject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminResponse.ok(subject));
    }

    /**
     * POST /api/admin/announcements
     * Creates and broadcasts a campus-wide announcement.
     */
    @PostMapping("/announcements")
    public ResponseEntity<AdminResponse<AnnouncementDto>> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request) {
        UserPrincipal principal = getAuthenticatedAdmin();
        AnnouncementDto announcement = adminService.createAnnouncement(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminResponse.ok(announcement));
    }

    /**
     * POST /api/admin/faqs
     * Creates a new FAQ entry.
     */
    @PostMapping("/faqs")
    public ResponseEntity<AdminResponse<FaqDto>> createFaq(
            @Valid @RequestBody CreateFaqRequest request) {
        getAuthenticatedAdmin();
        FaqDto faq = adminService.createFaq(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminResponse.ok(faq));
    }

    /**
     * GET /api/admin/faqs
     * Returns all FAQs.
     */
    @GetMapping("/faqs")
    public ResponseEntity<AdminResponse<List<FaqDto>>> getFaqs() {
        getAuthenticatedAdmin();
        List<FaqDto> faqs = adminService.getFaqsList();
        return ResponseEntity.ok(AdminResponse.ok(faqs, faqs.size()));
    }

    /**
     * PUT /api/admin/faqs/{id}
     * Updates an existing FAQ entry.
     */
    @PutMapping("/faqs/{id}")
    public ResponseEntity<AdminResponse<FaqDto>> updateFaq(
            @PathVariable String id,
            @Valid @RequestBody CreateFaqRequest request) {
        getAuthenticatedAdmin();
        if (id == null || id.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid FAQ ID is required");
        }
        FaqDto faq = adminService.updateFaq(id.trim(), request);
        return ResponseEntity.ok(AdminResponse.ok(faq));
    }

    /**
     * DELETE /api/admin/faqs/{id}
     * Deletes an FAQ entry.
     */
    @DeleteMapping("/faqs/{id}")
    public ResponseEntity<AdminResponse<Void>> deleteFaq(
            @PathVariable String id) {
        getAuthenticatedAdmin();
        if (id == null || id.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid FAQ ID is required");
        }
        adminService.deleteFaq(id.trim());
        return ResponseEntity.ok(AdminResponse.ok(null));
    }
}
