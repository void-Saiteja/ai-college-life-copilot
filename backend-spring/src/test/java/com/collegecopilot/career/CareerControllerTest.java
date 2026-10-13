package com.collegecopilot.career;

import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.career.dto.*;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CareerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CareerService careerService;

    @Mock
    private ResumeService resumeService;

    @Mock
    private InterviewService interviewService;

    @InjectMocks
    private CareerController careerController;

    private JwtTokenProvider tokenProvider;
    private String tokenA;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        tokenA = tokenProvider.generateToken("u-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(careerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/career/profile requires JWT token and returns 401 if missing")
    void testGetCareerProfileUnauthorized() throws Exception {
        mockMvc.perform(get("/api/career/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/career/profile returns 200 OK with profile and skillGapAnalysis")
    void testGetCareerProfileSuccess() throws Exception {
        CareerProfileDto profileDto = new CareerProfileDto(
                "cp-1", "std-1", "Goal", "Full Stack Developer", "Software Development",
                List.of(new SkillItemDto("JavaScript", "ADVANCED")),
                List.of("AI"),
                List.of(),
                "2026-10-10T10:00:00",
                "2026-10-10T10:00:00"
        );
        SkillGapAnalysisDto gapDto = new SkillGapAnalysisDto(
                "Full Stack Developer", 7, 1, 0, 6, 14.29,
                List.of("JavaScript"), List.of(), List.of("React", "Node.js"), List.of()
        );

        when(careerService.getCareerProfile(any(UserPrincipal.class)))
                .thenReturn(new CareerProfileResponse(true, new CareerProfileDataDto(profileDto, gapDto)));

        mockMvc.perform(get("/api/career/profile")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.target_role").value("Full Stack Developer"))
                .andExpect(jsonPath("$.data.skillGapAnalysis.coveragePercentage").value(14.29));
    }

    @Test
    @DisplayName("POST /api/career/profile updates or creates profile and returns 200 OK")
    void testPostCareerProfileSuccess() throws Exception {
        CareerProfileDto profileDto = new CareerProfileDto(
                "cp-1", "std-1", "Become Data Scientist", "Data Scientist", "Analytics",
                List.of(), List.of(), List.of(), null, null
        );

        when(careerService.saveCareerProfile(any(CareerProfileSaveRequest.class), any(UserPrincipal.class)))
                .thenReturn(new CareerProfileResponse(true, new CareerProfileDataDto(profileDto, null)));

        String json = """
                {
                  "career_goal": "Become Data Scientist",
                  "target_role": "Data Scientist",
                  "domain": "Analytics"
                }
                """;

        mockMvc.perform(post("/api/career/profile")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.target_role").value("Data Scientist"));
    }

    @Test
    @DisplayName("PUT /api/career/profile (legacy alias) updates profile and returns 200 OK")
    void testPutCareerProfileLegacyAlias() throws Exception {
        CareerProfileDto profileDto = new CareerProfileDto(
                "cp-1", "std-1", "Updated Goal", "Full Stack Developer", "Software Development",
                List.of(), List.of(), List.of(), null, null
        );

        when(careerService.saveCareerProfile(any(CareerProfileSaveRequest.class), any(UserPrincipal.class)))
                .thenReturn(new CareerProfileResponse(true, new CareerProfileDataDto(profileDto, null)));

        String json = """
                {
                  "career_goal": "Updated Goal"
                }
                """;

        mockMvc.perform(put("/api/career/profile")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.career_goal").value("Updated Goal"));
    }

    @Test
    @DisplayName("POST /api/career/profile returns 400 on invalid target role")
    void testPostCareerProfileInvalidRole() throws Exception {
        when(careerService.saveCareerProfile(any(CareerProfileSaveRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target role 'Pilot'. Supported roles: ..."));

        String json = """
                {
                  "target_role": "Pilot"
                }
                """;

        mockMvc.perform(post("/api/career/profile")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid target role 'Pilot'. Supported roles: ..."));
    }

    @Test
    @DisplayName("POST /api/career/skills adds skill and returns 201 Created")
    void testAddSkillSuccess() throws Exception {
        CareerProfileDto profileDto = new CareerProfileDto(
                "cp-1", "std-1", "Goal", "Full Stack Developer", "Software Development",
                List.of(new SkillItemDto("React", "INTERMEDIATE")),
                List.of(), List.of(), null, null
        );

        when(careerService.addSkill(any(SkillAddRequest.class), any(UserPrincipal.class)))
                .thenReturn(new CareerProfileResponse(true, new CareerProfileDataDto(profileDto, null)));

        String json = """
                {
                  "skill": "React",
                  "proficiency": "INTERMEDIATE"
                }
                """;

        mockMvc.perform(post("/api/career/skills")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.skills[0].skill").value("React"));
    }

    @Test
    @DisplayName("POST /api/career/skills returns 409 Conflict on duplicate skill")
    void testAddSkillDuplicateConflict() throws Exception {
        when(careerService.addSkill(any(SkillAddRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Skill 'React' already exists in your career profile"));

        String json = """
                {
                  "skill": "React",
                  "proficiency": "INTERMEDIATE"
                }
                """;

        mockMvc.perform(post("/api/career/skills")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Skill 'React' already exists in your career profile"));
    }

    @Test
    @DisplayName("PUT /api/career/skills/{skillName} updates proficiency and returns 200 OK")
    void testUpdateSkillSuccess() throws Exception {
        CareerProfileDto profileDto = new CareerProfileDto(
                "cp-1", "std-1", "Goal", "Full Stack Developer", "Software Development",
                List.of(new SkillItemDto("Docker", "ADVANCED")),
                List.of(), List.of(), null, null
        );

        when(careerService.updateSkill(eq("Docker"), any(SkillUpdateRequest.class), any(UserPrincipal.class)))
                .thenReturn(new CareerProfileResponse(true, new CareerProfileDataDto(profileDto, null)));

        String json = """
                {
                  "proficiency": "ADVANCED"
                }
                """;

        mockMvc.perform(put("/api/career/skills/Docker")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.skills[0].proficiency").value("ADVANCED"));
    }

    @Test
    @DisplayName("PUT /api/career/skills/{skillName} returns 404 when skill not found")
    void testUpdateSkillNotFound() throws Exception {
        when(careerService.updateSkill(eq("Kubernetes"), any(SkillUpdateRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill 'Kubernetes' not found in profile"));

        String json = """
                {
                  "proficiency": "ADVANCED"
                }
                """;

        mockMvc.perform(put("/api/career/skills/Kubernetes")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Skill 'Kubernetes' not found in profile"));
    }

    @Test
    @DisplayName("DELETE /api/career/skills/{skillName} removes skill and returns 200 OK")
    void testDeleteSkillSuccess() throws Exception {
        CareerProfileDto profileDto = new CareerProfileDto(
                "cp-1", "std-1", "Goal", "Full Stack Developer", "Software Development",
                List.of(), List.of(), List.of(), null, null
        );

        when(careerService.deleteSkill(eq("React"), any(UserPrincipal.class)))
                .thenReturn(new CareerProfileResponse(true, new CareerProfileDataDto(profileDto, null)));

        mockMvc.perform(delete("/api/career/skills/React")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/career/skills/{skillName} returns 404 when skill not found")
    void testDeleteSkillNotFound() throws Exception {
        when(careerService.deleteSkill(eq("NonExistent"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill 'NonExistent' not found in profile"));

        mockMvc.perform(delete("/api/career/skills/NonExistent")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Skill 'NonExistent' not found in profile"));
    }

    @Test
    @DisplayName("POST /api/career/advisor requires JWT token and returns 401 if missing")
    void testPostCareerAdvisorUnauthorized() throws Exception {
        mockMvc.perform(post("/api/career/advisor"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/career/advisor returns 200 OK with advice data")
    void testPostCareerAdvisorSuccess() throws Exception {
        SkillGapAnalysisDto gapDto = new SkillGapAnalysisDto(
                "Full Stack Developer", 7, 1, 0, 6, 14.29,
                List.of("JavaScript"), List.of(), List.of("React", "Node.js"), List.of()
        );
        CareerAdvisorDataDto advisorData = new CareerAdvisorDataDto(
                "Summary strategy", "Actionable recommendations", gapDto, false
        );

        when(careerService.runCareerAdvisor(any(UserPrincipal.class)))
                .thenReturn(new CareerAdvisorResponse(true, advisorData));

        mockMvc.perform(post("/api/career/advisor")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary").value("Summary strategy"))
                .andExpect(jsonPath("$.data.recommendation").value("Actionable recommendations"))
                .andExpect(jsonPath("$.data.isFallback").value(false));
    }

    @Test
    @DisplayName("POST /api/career/advisor returns 400 Bad Request when career profile is missing")
    void testPostCareerAdvisorMissingProfile() throws Exception {
        when(careerService.runCareerAdvisor(any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Career profile required. Please setup your target role and skills first."));

        mockMvc.perform(post("/api/career/advisor")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Career profile required. Please setup your target role and skills first."));
    }

    @Test
    @DisplayName("POST /api/career/roadmap requires JWT token and returns 401 if missing")
    void testPostCareerRoadmapUnauthorized() throws Exception {
        mockMvc.perform(post("/api/career/roadmap"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/career/roadmap returns 200 OK with phases and summary")
    void testPostCareerRoadmapSuccess() throws Exception {
        List<RoadmapPhaseDto> phases = List.of(
                new RoadmapPhaseDto("Phase 1: Foundations", List.of("JavaScript"), "HIGH", "Core principles", "2-4 weeks")
        );
        CareerRoadmapDataDto roadmapData = new CareerRoadmapDataDto("Custom roadmap", phases, false);

        when(careerService.runLearningRoadmap(any(UserPrincipal.class)))
                .thenReturn(new CareerRoadmapResponse(true, roadmapData));

        mockMvc.perform(post("/api/career/roadmap")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary").value("Custom roadmap"))
                .andExpect(jsonPath("$.data.phases[0].phase").value("Phase 1: Foundations"))
                .andExpect(jsonPath("$.data.isFallback").value(false));
    }

    @Test
    @DisplayName("POST /api/career/roadmap returns 400 Bad Request when career profile is missing")
    void testPostCareerRoadmapMissingProfile() throws Exception {
        when(careerService.runLearningRoadmap(any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Career profile required. Please setup your target role and skills first."));

        mockMvc.perform(post("/api/career/roadmap")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Career profile required. Please setup your target role and skills first."));
    }

    // ==========================================
    // RESUME ENDPOINT TESTS
    // ==========================================

    @Test
    @DisplayName("GET /api/career/resumes requires JWT token and returns 401 if missing")
    void testGetResumesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/career/resumes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/career/resumes returns 200 OK with list of resumes")
    void testGetResumesSuccess() throws Exception {
        ResumeDto resumeDto = new ResumeDto(
                "res-1", "std-1", "My Resume", "Full Stack Developer", "Resume.pdf",
                "Java React SQL", "Java React SQL", "2026-10-10T10:00:00", "2026-10-10T10:00:00", null
        );

        when(resumeService.getResumes(any(UserPrincipal.class)))
                .thenReturn(new ResumeListResponse(true, List.of(resumeDto)));

        mockMvc.perform(get("/api/career/resumes")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("res-1"))
                .andExpect(jsonPath("$.data[0].resume_text").value("Java React SQL"));
    }

    @Test
    @DisplayName("POST /api/career/resumes requires JWT token and returns 401 if missing")
    void testPostResumeUnauthorized() throws Exception {
        mockMvc.perform(post("/api/career/resumes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/career/resumes returns 201 Created on valid input")
    void testPostResumeSuccess() throws Exception {
        ResumeDto resumeDto = new ResumeDto(
                "res-1", "std-1", "My Resume", "Full Stack Developer", "Resume.pdf",
                "Java React SQL", "Java React SQL", "2026-10-10T10:00:00", "2026-10-10T10:00:00", null
        );

        when(resumeService.createResume(any(ResumeSaveRequest.class), any(UserPrincipal.class)))
                .thenReturn(new ResumeSingleResponse(true, resumeDto));

        String json = """
                {
                  "title": "My Resume",
                  "resume_text": "Java React SQL",
                  "target_role": "Full Stack Developer"
                }
                """;

        mockMvc.perform(post("/api/career/resumes")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("res-1"))
                .andExpect(jsonPath("$.data.title").value("My Resume"));
    }

    @Test
    @DisplayName("GET /api/career/resumes/{id} returns 200 OK for owner")
    void testGetResumeByIdSuccess() throws Exception {
        ResumeDto resumeDto = new ResumeDto(
                "res-1", "std-1", "My Resume", "Full Stack Developer", "Resume.pdf",
                "Java React SQL", "Java React SQL", "2026-10-10T10:00:00", "2026-10-10T10:00:00", null
        );

        when(resumeService.getResumeById(eq("res-1"), any(UserPrincipal.class)))
                .thenReturn(new ResumeSingleResponse(true, resumeDto));

        mockMvc.perform(get("/api/career/resumes/res-1")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("res-1"));
    }

    @Test
    @DisplayName("GET /api/career/resumes/{id} returns 404 when resume not found")
    void testGetResumeByIdNotFound() throws Exception {
        when(resumeService.getResumeById(eq("res-999"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));

        mockMvc.perform(get("/api/career/resumes/res-999")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Resume not found"));
    }

    @Test
    @DisplayName("GET /api/career/resumes/{id} returns 403 when access denied to other student resume")
    void testGetResumeByIdForbidden() throws Exception {
        when(resumeService.getResumeById(eq("res-other"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: Access denied to resume"));

        mockMvc.perform(get("/api/career/resumes/res-other")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Forbidden: Access denied to resume"));
    }

    @Test
    @DisplayName("PUT /api/career/resumes/{id} returns 200 OK on update")
    void testUpdateResumeSuccess() throws Exception {
        ResumeDto resumeDto = new ResumeDto(
                "res-1", "std-1", "Updated Title", "Full Stack Developer", "Resume.pdf",
                "Updated text", "Updated text", "2026-10-10T10:00:00", "2026-10-10T11:00:00", null
        );

        when(resumeService.updateResume(eq("res-1"), any(ResumeSaveRequest.class), any(UserPrincipal.class)))
                .thenReturn(new ResumeSingleResponse(true, resumeDto));

        String json = """
                {
                  "title": "Updated Title",
                  "resume_text": "Updated text"
                }
                """;

        mockMvc.perform(put("/api/career/resumes/res-1")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Title"));
    }

    @Test
    @DisplayName("DELETE /api/career/resumes/{id} returns 200 OK on delete")
    void testDeleteResumeSuccess() throws Exception {
        when(resumeService.deleteResume(eq("res-1"), any(UserPrincipal.class)))
                .thenReturn(new ResumeDeleteResponse(true, "Resume deleted successfully"));

        mockMvc.perform(delete("/api/career/resumes/res-1")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Resume deleted successfully"));
    }

    @Test
    @DisplayName("POST /api/career/resumes/{id}/analyze returns 200 OK with analysis data")
    void testAnalyzeResumeSuccess() throws Exception {
        ResumeCoverageDto coverage = new ResumeCoverageDto(
                "Full Stack Developer", 7, 3, 4, 42.86,
                List.of("JavaScript", "React", "Git"), List.of("Docker", "SQL", "REST APIs", "Node.js"), "formula"
        );
        ResumeAiSuggestionsDto suggestions = new ResumeAiSuggestionsDto(
                List.of("Good frontend skills"), List.of("Add backend skills"), List.of("Learn Node.js"), false, "disclaimer"
        );
        ResumeDocumentedFactsDto facts = new ResumeDocumentedFactsDto("Full Stack Developer", 3, 42.86, List.of("JavaScript"), List.of("SQL"));
        ResumeAnalysisDto analysisDto = new ResumeAnalysisDto("Full Stack Developer", coverage, List.of(), null, facts, suggestions);

        when(resumeService.analyzeResume(eq("res-1"), any(UserPrincipal.class)))
                .thenReturn(new ResumeAnalysisResponse(true, analysisDto));

        mockMvc.perform(post("/api/career/resumes/res-1/analyze")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetRole").value("Full Stack Developer"))
                .andExpect(jsonPath("$.data.coverage.coveragePercentage").value(42.86));
    }

    @Test
    @DisplayName("POST /api/career/analyze-resume (legacy) returns 200 OK with gap analysis")
    void testLegacyAnalyzeResumeSuccess() throws Exception {
        SkillGapAnalysisDto gapDto = new SkillGapAnalysisDto(
                "Full Stack Developer", 7, 0, 0, 7, 0.0,
                List.of(), List.of(), List.of(), List.of()
        );
        LegacyResumeAnalyzeResponse response = new LegacyResumeAnalyzeResponse(
                true,
                new LegacyResumeAnalyzeResponse.LegacyResumeAnalyzeDataDto("Full Stack Developer", gapDto)
        );

        when(resumeService.analyzeResumeLegacy(any(LegacyResumeAnalyzeRequest.class), any(UserPrincipal.class)))
                .thenReturn(response);

        String json = """
                {
                  "resumeText": "Experienced in React and JavaScript",
                  "targetRole": "Full Stack Developer"
                }
                """;

        mockMvc.perform(post("/api/career/analyze-resume")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetRole").value("Full Stack Developer"));
    }

    // ==========================================
    // INTERVIEW ENDPOINT TESTS
    // ==========================================

    @Test
    @DisplayName("POST /api/career/interview/generate requires JWT and returns 401 if missing")
    void testGenerateInterviewQuestionUnauthorized() throws Exception {
        mockMvc.perform(post("/api/career/interview/generate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/career/interview/generate returns 201 Created with generated question")
    void testGenerateInterviewQuestionSuccess() throws Exception {
        InterviewQuestionDto qDto = new InterviewQuestionDto(
                "iq-1", "std-1", "Explain the event loop in Node.js", "TECHNICAL", "MEDIUM",
                "Node.js", "Full Stack Developer", "Tips", false, "2026-10-11T10:00:00"
        );

        when(interviewService.generateQuestion(any(InterviewQuestionGenerateRequest.class), any(UserPrincipal.class)))
                .thenReturn(new InterviewQuestionResponse(true, qDto));

        String json = """
                {
                  "category": "TECHNICAL",
                  "difficulty": "MEDIUM",
                  "skill": "Node.js"
                }
                """;

        mockMvc.perform(post("/api/career/interview/generate")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("iq-1"))
                .andExpect(jsonPath("$.data.question").value("Explain the event loop in Node.js"));
    }

    @Test
    @DisplayName("POST /api/career/interview/{id}/answer requires JWT and returns 401 if missing")
    void testSubmitInterviewAnswerUnauthorized() throws Exception {
        mockMvc.perform(post("/api/career/interview/iq-1/answer"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/career/interview/{id}/answer returns 201 Created with evaluated response")
    void testSubmitInterviewAnswerSuccess() throws Exception {
        InterviewScoresDto scores = new InterviewScoresDto(4, 4, 4, 4, 16, 20, 80.0, "formula");
        InterviewFeedbackDto feedback = new InterviewFeedbackDto(List.of("Good explanation"), List.of("Add more context"), "Overall solid", "Tip", false);
        InterviewPracticeDto pDto = new InterviewPracticeDto(
                "ip-1", "std-1", "iq-1", "Question text", "TECHNICAL", "MEDIUM", "Node.js",
                "Full Stack Developer", "My technical answer text", scores, feedback, "2026-10-11T10:00:00"
        );

        when(interviewService.submitAnswer(eq("iq-1"), any(InterviewAnswerSubmitRequest.class), any(UserPrincipal.class)))
                .thenReturn(new InterviewPracticeResponse(true, pDto));

        String json = """
                {
                  "answer": "My technical answer text"
                }
                """;

        mockMvc.perform(post("/api/career/interview/iq-1/answer")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scores.percentage").value(80.0));
    }

    @Test
    @DisplayName("GET /api/career/interview/history requires JWT and returns 401 if missing")
    void testGetInterviewHistoryUnauthorized() throws Exception {
        mockMvc.perform(get("/api/career/interview/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/career/interview/history returns 200 OK with history and weak areas")
    void testGetInterviewHistorySuccess() throws Exception {
        InterviewHistoryDataDto historyData = new InterviewHistoryDataDto(
                List.of(), 0, 0.0, Collections.emptyMap(), Collections.emptyMap(), List.of(), List.of()
        );

        when(interviewService.getHistory(any(UserPrincipal.class)))
                .thenReturn(new InterviewHistoryResponse(true, historyData));

        mockMvc.perform(get("/api/career/interview/history")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total_attempts").value(0));
    }

    @Test
    @DisplayName("POST /api/career/interview-practice (legacy) returns 200 OK with evaluated response")
    void testPracticeInterviewLegacySuccess() throws Exception {
        LegacyInterviewPracticeResponse response = new LegacyInterviewPracticeResponse(
                true,
                new LegacyInterviewPracticeResponse.LegacyInterviewPracticeDataDto(
                        "Full Stack Developer", "Technical question", "Sample answer", 85, "Feedback", "Follow-up"
                )
        );

        when(interviewService.practiceInterviewLegacy(any(LegacyInterviewPracticeRequest.class), any(UserPrincipal.class)))
                .thenReturn(response);

        String json = """
                {
                  "targetRole": "Full Stack Developer",
                  "question": "Technical question",
                  "answer": "Sample answer"
                }
                """;

        mockMvc.perform(post("/api/career/interview-practice")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clarityScore").value(85));
    }
}
