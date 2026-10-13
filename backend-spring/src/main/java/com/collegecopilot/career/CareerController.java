package com.collegecopilot.career;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.career.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/career")
@CrossOrigin(origins = "*")
public class CareerController {

    private final CareerService careerService;
    private final ResumeService resumeService;
    private final InterviewService interviewService;

    public CareerController(CareerService careerService) {
        this(careerService, null, null);
    }

    public CareerController(CareerService careerService, ResumeService resumeService) {
        this(careerService, resumeService, null);
    }

    @Autowired
    public CareerController(CareerService careerService,
                            @Autowired(required = false) ResumeService resumeService,
                            @Autowired(required = false) InterviewService interviewService) {
        this.careerService = careerService;
        this.resumeService = resumeService;
        this.interviewService = interviewService;
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        return principal;
    }

    // GET /api/career/profile
    @GetMapping("/profile")
    public ResponseEntity<CareerProfileResponse> getCareerProfile() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(careerService.getCareerProfile(principal));
    }

    // POST /api/career/profile
    @PostMapping("/profile")
    public ResponseEntity<CareerProfileResponse> saveCareerProfilePost(@RequestBody CareerProfileSaveRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(careerService.saveCareerProfile(request, principal));
    }

    // PUT /api/career/profile (legacy alias)
    @PutMapping("/profile")
    public ResponseEntity<CareerProfileResponse> saveCareerProfilePut(@RequestBody CareerProfileSaveRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(careerService.saveCareerProfile(request, principal));
    }

    // POST /api/career/skills
    @PostMapping("/skills")
    public ResponseEntity<CareerProfileResponse> addSkill(@RequestBody SkillAddRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        CareerProfileResponse response = careerService.addSkill(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // PUT /api/career/skills/{skillName}
    @PutMapping("/skills/{skillName}")
    public ResponseEntity<CareerProfileResponse> updateSkill(
            @PathVariable("skillName") String skillName,
            @RequestBody SkillUpdateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(careerService.updateSkill(skillName, request, principal));
    }

    // DELETE /api/career/skills/{skillName}
    @DeleteMapping("/skills/{skillName}")
    public ResponseEntity<CareerProfileResponse> deleteSkill(
            @PathVariable("skillName") String skillName) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(careerService.deleteSkill(skillName, principal));
    }

    // POST /api/career/advisor
    @PostMapping("/advisor")
    public ResponseEntity<CareerAdvisorResponse> runCareerAdvisor() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(careerService.runCareerAdvisor(principal));
    }

    // POST /api/career/roadmap
    @PostMapping("/roadmap")
    public ResponseEntity<CareerRoadmapResponse> runLearningRoadmap() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(careerService.runLearningRoadmap(principal));
    }

    // ==========================================
    // RESUME INTELLIGENCE ENDPOINTS
    // ==========================================

    // GET /api/career/resumes
    @GetMapping("/resumes")
    public ResponseEntity<ResumeListResponse> getResumes() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(resumeService.getResumes(principal));
    }

    // POST /api/career/resumes
    @PostMapping("/resumes")
    public ResponseEntity<ResumeSingleResponse> createResume(@RequestBody(required = false) ResumeSaveRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        ResumeSingleResponse response = resumeService.createResume(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/career/resumes/{id}
    @GetMapping("/resumes/{id}")
    public ResponseEntity<ResumeSingleResponse> getResumeById(@PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(resumeService.getResumeById(id, principal));
    }

    // PUT /api/career/resumes/{id}
    @PutMapping("/resumes/{id}")
    public ResponseEntity<ResumeSingleResponse> updateResume(
            @PathVariable("id") String id,
            @RequestBody(required = false) ResumeSaveRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(resumeService.updateResume(id, request, principal));
    }

    // DELETE /api/career/resumes/{id}
    @DeleteMapping("/resumes/{id}")
    public ResponseEntity<ResumeDeleteResponse> deleteResume(@PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(resumeService.deleteResume(id, principal));
    }

    // POST /api/career/resumes/{id}/analyze
    @PostMapping("/resumes/{id}/analyze")
    public ResponseEntity<ResumeAnalysisResponse> analyzeResume(@PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(resumeService.analyzeResume(id, principal));
    }

    // POST /api/career/analyze-resume (backward compatibility endpoint)
    @PostMapping("/analyze-resume")
    public ResponseEntity<LegacyResumeAnalyzeResponse> analyzeResumeLegacy(
            @RequestBody(required = false) LegacyResumeAnalyzeRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(resumeService.analyzeResumeLegacy(request, principal));
    }

    // ==========================================
    // INTERVIEW INTELLIGENCE ENDPOINTS
    // ==========================================

    // POST /api/career/interview/generate
    @PostMapping("/interview/generate")
    public ResponseEntity<InterviewQuestionResponse> generateInterviewQuestion(
            @RequestBody(required = false) InterviewQuestionGenerateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        InterviewQuestionResponse response = interviewService.generateQuestion(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/career/interview/{id}/answer
    @PostMapping("/interview/{id}/answer")
    public ResponseEntity<InterviewPracticeResponse> submitInterviewAnswer(
            @PathVariable("id") String id,
            @RequestBody(required = false) InterviewAnswerSubmitRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        InterviewPracticeResponse response = interviewService.submitAnswer(id, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/career/interview/history
    @GetMapping("/interview/history")
    public ResponseEntity<InterviewHistoryResponse> getInterviewHistory() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(interviewService.getHistory(principal));
    }

    // POST /api/career/interview-practice (backward compatibility endpoint)
    @PostMapping("/interview-practice")
    public ResponseEntity<LegacyInterviewPracticeResponse> practiceInterviewLegacy(
            @RequestBody(required = false) LegacyInterviewPracticeRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(interviewService.practiceInterviewLegacy(request, principal));
    }
}
