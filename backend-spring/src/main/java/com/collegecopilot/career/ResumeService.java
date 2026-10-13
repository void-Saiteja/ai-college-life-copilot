package com.collegecopilot.career;

import com.collegecopilot.career.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final CareerProfileRepository careerProfileRepository;
    private final CareerService careerService;
    private final GeminiResumeService geminiResumeService;

    @Autowired
    public ResumeService(ResumeRepository resumeRepository,
                         CareerProfileRepository careerProfileRepository,
                         CareerService careerService,
                         GeminiResumeService geminiResumeService) {
        this.resumeRepository = resumeRepository;
        this.careerProfileRepository = careerProfileRepository;
        this.careerService = careerService;
        this.geminiResumeService = geminiResumeService != null ? geminiResumeService : new GeminiResumeService();
    }

    @Transactional(readOnly = true)
    public ResumeListResponse getResumes(UserPrincipal principal) {
        String studentId = careerService.resolveStudentId(principal);
        List<Resume> resumes = resumeRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        List<ResumeDto> dtos = resumes.stream().map(Resume::toDto).toList();
        return new ResumeListResponse(true, dtos);
    }

    @Transactional
    public ResumeSingleResponse createResume(ResumeSaveRequest request, UserPrincipal principal) {
        String studentId = careerService.resolveStudentId(principal);

        if (request == null || request.resumeText() == null || request.resumeText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume text is required and cannot be empty");
        }

        if (request.resumeText().length() > 50000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume text exceeds maximum length of 50,000 characters");
        }

        if (request.targetRole() != null && !request.targetRole().isBlank()
                && !CareerService.ROLE_SKILL_CATALOG.containsKey(request.targetRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid target role '" + request.targetRole() + "'. Supported roles: "
                            + String.join(", ", CareerService.ROLE_SKILL_CATALOG.keySet()));
        }

        Optional<CareerProfile> profileOpt = careerProfileRepository.findByStudentId(studentId);

        String assignedRole = (request.targetRole() != null && !request.targetRole().isBlank())
                ? request.targetRole()
                : profileOpt.map(CareerProfile::getTargetRole).orElse("Full Stack Developer");

        String assignedTitle = (request.title() != null && !request.title().isBlank())
                ? request.title().trim()
                : "Resume Version (" + LocalDate.now() + ")";

        String resumeId = "res-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);

        Resume resume = new Resume(
                resumeId,
                studentId,
                assignedTitle,
                assignedRole,
                "Resume.pdf",
                request.resumeText().trim(),
                null
        );

        resume = resumeRepository.save(resume);
        return new ResumeSingleResponse(true, resume.toDto());
    }

    @Transactional(readOnly = true)
    public ResumeSingleResponse getResumeById(String id, UserPrincipal principal) {
        String studentId = careerService.resolveStudentId(principal);

        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));

        if (!resume.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: Access denied to resume");
        }

        return new ResumeSingleResponse(true, resume.toDto());
    }

    @Transactional
    public ResumeSingleResponse updateResume(String id, ResumeSaveRequest request, UserPrincipal principal) {
        String studentId = careerService.resolveStudentId(principal);

        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));

        if (!resume.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: Access denied to resume");
        }

        if (request != null) {
            if (request.resumeText() != null) {
                if (request.resumeText().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume text cannot be empty");
                }
                if (request.resumeText().length() > 50000) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume text exceeds maximum length of 50,000 characters");
                }
                resume.setExtractedText(request.resumeText().trim());
            }

            if (request.targetRole() != null) {
                if (!CareerService.ROLE_SKILL_CATALOG.containsKey(request.targetRole())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid target role '" + request.targetRole() + "'. Supported roles: "
                                    + String.join(", ", CareerService.ROLE_SKILL_CATALOG.keySet()));
                }
                resume.setTargetRole(request.targetRole());
            }

            if (request.title() != null && !request.title().isBlank()) {
                resume.setTitle(request.title().trim());
            }
        }

        resume.setUpdatedAt(LocalDateTime.now());
        resume = resumeRepository.save(resume);
        return new ResumeSingleResponse(true, resume.toDto());
    }

    @Transactional
    public ResumeDeleteResponse deleteResume(String id, UserPrincipal principal) {
        String studentId = careerService.resolveStudentId(principal);

        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));

        if (!resume.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: Access denied to resume");
        }

        resumeRepository.delete(resume);
        return new ResumeDeleteResponse(true, "Resume deleted successfully");
    }

    @Transactional
    public ResumeAnalysisResponse analyzeResume(String id, UserPrincipal principal) {
        String studentId = careerService.resolveStudentId(principal);

        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));

        if (!resume.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: Access denied to resume");
        }

        CareerProfile profile = careerProfileRepository.findByStudentId(studentId).orElse(null);
        String targetRole = (resume.getTargetRole() != null && !resume.getTargetRole().isBlank())
                ? resume.getTargetRole()
                : (profile != null && profile.getTargetRole() != null ? profile.getTargetRole() : "Full Stack Developer");

        SkillGapAnalysisDto gapAnalysis = careerService.calculateSkillGaps(
                profile != null ? profile.getSkills() : Collections.emptyList(),
                targetRole
        );

        ResumeAnalysisDto analysis = geminiResumeService.analyzeResume(
                resume.getExtractedText(),
                targetRole,
                profile,
                gapAnalysis
        );

        resume.setAnalysis(analysis);
        resume.setUpdatedAt(LocalDateTime.now());
        resumeRepository.save(resume);

        return new ResumeAnalysisResponse(true, analysis);
    }

    @Transactional(readOnly = true)
    public LegacyResumeAnalyzeResponse analyzeResumeLegacy(LegacyResumeAnalyzeRequest request, UserPrincipal principal) {
        careerService.resolveStudentId(principal);

        if (request == null || request.resumeText() == null || request.resumeText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume text is required");
        }

        String targetRole = (request.targetRole() != null && !request.targetRole().isBlank())
                ? request.targetRole()
                : "Full Stack Developer";

        SkillGapAnalysisDto gapAnalysis = careerService.calculateSkillGaps(Collections.emptyList(), targetRole);
        return new LegacyResumeAnalyzeResponse(
                true,
                new LegacyResumeAnalyzeResponse.LegacyResumeAnalyzeDataDto(targetRole, gapAnalysis)
        );
    }
}
