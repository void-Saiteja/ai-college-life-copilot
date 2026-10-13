package com.collegecopilot.career;

import com.collegecopilot.career.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private CareerProfileRepository careerProfileRepository;

    @Mock
    private CareerService careerService;

    @Mock
    private GeminiResumeService geminiResumeService;

    @InjectMocks
    private ResumeService resumeService;

    private UserPrincipal principalA;
    private UserPrincipal principalB;

    @BeforeEach
    void setUp() {
        principalA = new UserPrincipal("u-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);
        principalB = new UserPrincipal("u-2", "Bob Smith", "bob@student.edu", "STUDENT", "std-2", null);
    }

    @Test
    @DisplayName("getResumes returns list of resumes owned by student")
    void testGetResumes() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");

        Resume r1 = new Resume("res-1", "std-1", "Resume 1", "Full Stack Developer", "Resume.pdf", "Text 1", null);
        when(resumeRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(List.of(r1));

        ResumeListResponse response = resumeService.getResumes(principalA);
        assertTrue(response.success());
        assertEquals(1, response.data().size());
        assertEquals("res-1", response.data().get(0).id());
        assertEquals("Text 1", response.data().get(0).resumeText());
    }

    @Test
    @DisplayName("createResume succeeds with valid data and default values")
    void testCreateResumeSuccess() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.empty());
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResumeSaveRequest req = new ResumeSaveRequest(null, "Experienced in Java, Spring Boot and React", null);
        ResumeSingleResponse response = resumeService.createResume(req, principalA);

        assertTrue(response.success());
        assertNotNull(response.data());
        assertEquals("Full Stack Developer", response.data().targetRole());
        assertTrue(response.data().title().startsWith("Resume Version ("));
        assertEquals("Experienced in Java, Spring Boot and React", response.data().resumeText());
        verify(resumeRepository, times(1)).save(any(Resume.class));
    }

    @Test
    @DisplayName("createResume throws 400 when resumeText is empty")
    void testCreateResumeEmptyText() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");

        ResumeSaveRequest req = new ResumeSaveRequest("Title", "   ", "Full Stack Developer");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resumeService.createResume(req, principalA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Resume text is required"));
    }

    @Test
    @DisplayName("createResume throws 400 when resumeText exceeds 50000 characters")
    void testCreateResumeExceedsMaxLength() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");

        String longText = "a".repeat(50001);
        ResumeSaveRequest req = new ResumeSaveRequest("Title", longText, "Full Stack Developer");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resumeService.createResume(req, principalA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("exceeds maximum length of 50,000 characters"));
    }

    @Test
    @DisplayName("createResume throws 400 when targetRole is invalid")
    void testCreateResumeInvalidRole() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");

        ResumeSaveRequest req = new ResumeSaveRequest("Title", "Valid resume text", "Astronaut");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resumeService.createResume(req, principalA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid target role 'Astronaut'"));
    }

    @Test
    @DisplayName("getResumeById returns resume for owner")
    void testGetResumeByIdSuccess() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");
        Resume r = new Resume("res-1", "std-1", "Resume 1", "Full Stack Developer", "Resume.pdf", "Text", null);
        when(resumeRepository.findById("res-1")).thenReturn(Optional.of(r));

        ResumeSingleResponse response = resumeService.getResumeById("res-1", principalA);
        assertTrue(response.success());
        assertEquals("res-1", response.data().id());
    }

    @Test
    @DisplayName("getResumeById throws 404 when not found")
    void testGetResumeByIdNotFound() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");
        when(resumeRepository.findById("res-unknown")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resumeService.getResumeById("res-unknown", principalA));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("getResumeById enforces student isolation and throws 403 when owned by another student")
    void testGetResumeByIdForbiddenForOtherStudent() {
        when(careerService.resolveStudentId(principalB)).thenReturn("std-2");
        Resume r = new Resume("res-1", "std-1", "Resume 1", "Full Stack Developer", "Resume.pdf", "Text", null);
        when(resumeRepository.findById("res-1")).thenReturn(Optional.of(r));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resumeService.getResumeById("res-1", principalB));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Forbidden: Access denied to resume"));
    }

    @Test
    @DisplayName("updateResume updates fields and saves for owner")
    void testUpdateResumeSuccess() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");
        Resume r = new Resume("res-1", "std-1", "Old Title", "Full Stack Developer", "Resume.pdf", "Old Text", null);
        when(resumeRepository.findById("res-1")).thenReturn(Optional.of(r));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResumeSaveRequest req = new ResumeSaveRequest("Updated Title", "Updated Text", "Backend Developer");
        ResumeSingleResponse response = resumeService.updateResume("res-1", req, principalA);

        assertTrue(response.success());
        assertEquals("Updated Title", response.data().title());
        assertEquals("Updated Text", response.data().resumeText());
        assertEquals("Backend Developer", response.data().targetRole());
    }

    @Test
    @DisplayName("updateResume enforces student isolation and throws 403 for non-owner")
    void testUpdateResumeForbiddenForOtherStudent() {
        when(careerService.resolveStudentId(principalB)).thenReturn("std-2");
        Resume r = new Resume("res-1", "std-1", "Title", "Full Stack Developer", "Resume.pdf", "Text", null);
        when(resumeRepository.findById("res-1")).thenReturn(Optional.of(r));

        ResumeSaveRequest req = new ResumeSaveRequest("New Title", "New Text", null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resumeService.updateResume("res-1", req, principalB));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("deleteResume deletes resume for owner")
    void testDeleteResumeSuccess() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");
        Resume r = new Resume("res-1", "std-1", "Title", "Full Stack Developer", "Resume.pdf", "Text", null);
        when(resumeRepository.findById("res-1")).thenReturn(Optional.of(r));

        ResumeDeleteResponse response = resumeService.deleteResume("res-1", principalA);
        assertTrue(response.success());
        assertEquals("Resume deleted successfully", response.message());
        verify(resumeRepository, times(1)).delete(r);
    }

    @Test
    @DisplayName("deleteResume enforces student isolation and throws 403 for non-owner")
    void testDeleteResumeForbiddenForOtherStudent() {
        when(careerService.resolveStudentId(principalB)).thenReturn("std-2");
        Resume r = new Resume("res-1", "std-1", "Title", "Full Stack Developer", "Resume.pdf", "Text", null);
        when(resumeRepository.findById("res-1")).thenReturn(Optional.of(r));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resumeService.deleteResume("res-1", principalB));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(resumeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("analyzeResume performs analysis, updates resume, and returns analysis response")
    void testAnalyzeResumeSuccess() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");
        Resume r = new Resume("res-1", "std-1", "Title", "Full Stack Developer", "Resume.pdf", "Java, SQL", null);
        when(resumeRepository.findById("res-1")).thenReturn(Optional.of(r));
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.empty());

        SkillGapAnalysisDto gapDto = new SkillGapAnalysisDto("Full Stack Developer", 7, 0, 0, 7, 0.0,
                List.of(), List.of(), List.of(), List.of());
        when(careerService.calculateSkillGaps(any(), eq("Full Stack Developer"))).thenReturn(gapDto);

        ResumeCoverageDto coverageDto = new ResumeCoverageDto("Full Stack Developer", 7, 1, 6, 14.29,
                List.of("SQL"), List.of("JavaScript"), "formula");
        ResumeAiSuggestionsDto aiDto = new ResumeAiSuggestionsDto(List.of("Good SQL"), List.of("Missing React"), List.of("Learn React"), false, "disclaimer");
        ResumeDocumentedFactsDto factsDto = new ResumeDocumentedFactsDto("Full Stack Developer", 2, 14.29, List.of("SQL"), List.of("JavaScript"));
        ResumeAnalysisDto analysisDto = new ResumeAnalysisDto("Full Stack Developer", coverageDto, List.of(), gapDto, factsDto, aiDto);

        when(geminiResumeService.analyzeResume(eq("Java, SQL"), eq("Full Stack Developer"), any(), eq(gapDto)))
                .thenReturn(analysisDto);
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResumeAnalysisResponse response = resumeService.analyzeResume("res-1", principalA);
        assertTrue(response.success());
        assertNotNull(response.data());
        assertEquals("Full Stack Developer", response.data().targetRole());
        assertEquals(14.29, response.data().coverage().coveragePercentage());
        assertNotNull(r.getAnalysis());
        verify(resumeRepository, times(1)).save(r);
    }

    @Test
    @DisplayName("analyzeResume enforces student isolation and throws 403 for non-owner")
    void testAnalyzeResumeForbiddenForOtherStudent() {
        when(careerService.resolveStudentId(principalB)).thenReturn("std-2");
        Resume r = new Resume("res-1", "std-1", "Title", "Full Stack Developer", "Resume.pdf", "Java, SQL", null);
        when(resumeRepository.findById("res-1")).thenReturn(Optional.of(r));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resumeService.analyzeResume("res-1", principalB));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("analyzeResumeLegacy succeeds for valid resume text")
    void testAnalyzeResumeLegacySuccess() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");
        SkillGapAnalysisDto gapDto = new SkillGapAnalysisDto("Full Stack Developer", 7, 0, 0, 7, 0.0,
                List.of(), List.of(), List.of(), List.of());
        when(careerService.calculateSkillGaps(Collections.emptyList(), "Full Stack Developer")).thenReturn(gapDto);

        LegacyResumeAnalyzeRequest req = new LegacyResumeAnalyzeRequest("Sample resume text", "Full Stack Developer");
        LegacyResumeAnalyzeResponse response = resumeService.analyzeResumeLegacy(req, principalA);

        assertTrue(response.success());
        assertEquals("Full Stack Developer", response.data().targetRole());
        assertNotNull(response.data().skillGapAnalysis());
    }

    @Test
    @DisplayName("analyzeResumeLegacy throws 400 when resumeText is missing")
    void testAnalyzeResumeLegacyEmptyText() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");

        LegacyResumeAnalyzeRequest req = new LegacyResumeAnalyzeRequest("", "Full Stack Developer");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resumeService.analyzeResumeLegacy(req, principalA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
