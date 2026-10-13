package com.collegecopilot.career;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.career.dto.*;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.student.Student;
import com.collegecopilot.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.collegecopilot.student.QuizAttempt;
import com.collegecopilot.student.QuizAttemptRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareerServiceTest {

    @Mock
    private CareerProfileRepository careerProfileRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private GeminiCareerAdvisorService advisorService;

    private CareerService careerService;

    private UserPrincipal studentA;
    private UserPrincipal studentB;

    @BeforeEach
    void setUp() {
        careerService = new CareerService(careerProfileRepository, studentRepository, quizAttemptRepository, advisorService);

        studentA = new UserPrincipal("u-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);
        studentB = new UserPrincipal("u-2", "Bob Smith", "bob@student.edu", "STUDENT", "std-2", null);

        lenient().when(studentRepository.findById("std-1")).thenReturn(Optional.of(new Student("std-1", "u-1", "CS101", 1, "CS", null)));
        lenient().when(studentRepository.findById("std-2")).thenReturn(Optional.of(new Student("std-2", "u-2", "CS102", 1, "CS", null)));
    }

    @Test
    @DisplayName("getCareerProfile returns null data with helpful message when profile not found")
    void testGetCareerProfileNotFound() {
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.empty());

        CareerProfileResponse response = careerService.getCareerProfile(studentA);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        assertEquals("No career profile found for student. Please create a profile.", response.getMessage());
    }

    @Test
    @DisplayName("getCareerProfile returns profile and skill gap analysis when profile exists")
    void testGetCareerProfileSuccess() {
        CareerProfile profile = new CareerProfile(
                "cp-1", "std-1", "Become a Lead Dev", "Frontend Developer", "Software Engineering",
                List.of(new SkillItem("React", "ADVANCED"), new SkillItem("JavaScript", "INTERMEDIATE")),
                List.of("UI/UX"),
                List.of()
        );
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.of(profile));

        CareerProfileResponse response = careerService.getCareerProfile(studentA);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getProfile());
        assertEquals("Frontend Developer", response.getData().getProfile().getTargetRole());
        assertEquals(2, response.getData().getProfile().getSkills().size());

        SkillGapAnalysisDto gaps = response.getData().getSkillGapAnalysis();
        assertNotNull(gaps);
        assertEquals("Frontend Developer", gaps.getTargetRole());
        assertEquals(2, gaps.getStrongCount());
        assertTrue(gaps.getMissingList().contains("HTML"));
    }

    @Test
    @DisplayName("saveCareerProfile creates a new profile when one does not exist")
    void testSaveCareerProfileCreateNew() {
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.empty());
        when(careerProfileRepository.save(any(CareerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareerProfileSaveRequest request = new CareerProfileSaveRequest(
                "Become ML Engineer",
                "Machine Learning Engineer",
                "AI/ML",
                List.of(new SkillItemDto("Python", "ADVANCED")),
                List.of("Neural Networks"),
                List.of()
        );

        CareerProfileResponse response = careerService.saveCareerProfile(request, studentA);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("Machine Learning Engineer", response.getData().getProfile().getTargetRole());
        assertEquals("std-1", response.getData().getProfile().getStudentId());
        assertEquals(1, response.getData().getProfile().getSkills().size());
        assertEquals("Python", response.getData().getProfile().getSkills().get(0).getSkill());
    }

    @Test
    @DisplayName("saveCareerProfile updates existing profile fields")
    void testSaveCareerProfileUpdateExisting() {
        CareerProfile existing = new CareerProfile(
                "cp-1", "std-1", "Old Goal", "Full Stack Developer", "Software Development",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        );
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.of(existing));
        when(careerProfileRepository.save(any(CareerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareerProfileSaveRequest request = new CareerProfileSaveRequest(
                "New Goal",
                "Backend Developer",
                null,
                null,
                null,
                null
        );

        CareerProfileResponse response = careerService.saveCareerProfile(request, studentA);

        assertTrue(response.isSuccess());
        assertEquals("New Goal", response.getData().getProfile().getCareerGoal());
        assertEquals("Backend Developer", response.getData().getProfile().getTargetRole());
        assertEquals("Software Development", response.getData().getProfile().getDomain());
    }

    @Test
    @DisplayName("saveCareerProfile rejects unsupported target role with 400")
    void testSaveCareerProfileInvalidRole() {
        CareerProfileSaveRequest request = new CareerProfileSaveRequest(
                "Goal", "Astronaut", "Space", null, null, null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> careerService.saveCareerProfile(request, studentA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid target role"));
    }

    @Test
    @DisplayName("saveCareerProfile rejects invalid skill proficiency with 400")
    void testSaveCareerProfileInvalidProficiency() {
        CareerProfileSaveRequest request = new CareerProfileSaveRequest(
                "Goal", "Full Stack Developer", "Software Development",
                List.of(new SkillItemDto("React", "GURU")),
                null, null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> careerService.saveCareerProfile(request, studentA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid proficiency level"));
    }

    @Test
    @DisplayName("addSkill successfully adds skill and calculates skill gaps")
    void testAddSkillSuccess() {
        CareerProfile existing = new CareerProfile(
                "cp-1", "std-1", "Goal", "Full Stack Developer", "Software Development",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        );
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.of(existing));
        when(careerProfileRepository.save(any(CareerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SkillAddRequest request = new SkillAddRequest("JavaScript", "ADVANCED");
        CareerProfileResponse response = careerService.addSkill(request, studentA);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().getProfile().getSkills().size());
        assertEquals("JavaScript", response.getData().getProfile().getSkills().get(0).getSkill());
        assertEquals("ADVANCED", response.getData().getProfile().getSkills().get(0).getProficiency());
    }

    @Test
    @DisplayName("addSkill rejects duplicate skill name with 409 Conflict")
    void testAddSkillDuplicateConflict() {
        CareerProfile existing = new CareerProfile(
                "cp-1", "std-1", "Goal", "Full Stack Developer", "Software Development",
                new ArrayList<>(List.of(new SkillItem("JavaScript", "INTERMEDIATE"))),
                new ArrayList<>(), new ArrayList<>()
        );
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.of(existing));

        SkillAddRequest request = new SkillAddRequest("javascript", "ADVANCED");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> careerService.addSkill(request, studentA));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("already exists"));
    }

    @Test
    @DisplayName("addSkill rejects empty skill name or invalid proficiency with 400")
    void testAddSkillInvalidInput() {
        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class,
                () -> careerService.addSkill(new SkillAddRequest("", "INTERMEDIATE"), studentA));
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatusCode());

        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class,
                () -> careerService.addSkill(new SkillAddRequest("SQL", "EXPERT"), studentA));
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatusCode());
    }

    @Test
    @DisplayName("updateSkill updates existing skill proficiency")
    void testUpdateSkillSuccess() {
        CareerProfile existing = new CareerProfile(
                "cp-1", "std-1", "Goal", "Full Stack Developer", "Software Development",
                new ArrayList<>(List.of(new SkillItem("Docker", "BEGINNER"))),
                new ArrayList<>(), new ArrayList<>()
        );
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.of(existing));
        when(careerProfileRepository.save(any(CareerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareerProfileResponse response = careerService.updateSkill("Docker", new SkillUpdateRequest("ADVANCED"), studentA);

        assertTrue(response.isSuccess());
        assertEquals("ADVANCED", response.getData().getProfile().getSkills().get(0).getProficiency());
    }

    @Test
    @DisplayName("updateSkill throws 404 when skill not found in profile")
    void testUpdateSkillNotFound() {
        CareerProfile existing = new CareerProfile(
                "cp-1", "std-1", "Goal", "Full Stack Developer", "Software Development",
                new ArrayList<>(List.of(new SkillItem("Docker", "BEGINNER"))),
                new ArrayList<>(), new ArrayList<>()
        );
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> careerService.updateSkill("Kubernetes", new SkillUpdateRequest("ADVANCED"), studentA));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("deleteSkill removes skill from profile")
    void testDeleteSkillSuccess() {
        CareerProfile existing = new CareerProfile(
                "cp-1", "std-1", "Goal", "Full Stack Developer", "Software Development",
                new ArrayList<>(List.of(new SkillItem("Git", "INTERMEDIATE"), new SkillItem("Docker", "BEGINNER"))),
                new ArrayList<>(), new ArrayList<>()
        );
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.of(existing));
        when(careerProfileRepository.save(any(CareerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareerProfileResponse response = careerService.deleteSkill("Git", studentA);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().getProfile().getSkills().size());
        assertEquals("Docker", response.getData().getProfile().getSkills().get(0).getSkill());
    }

    @Test
    @DisplayName("deleteSkill throws 404 when skill is not present in profile")
    void testDeleteSkillNotFound() {
        CareerProfile existing = new CareerProfile(
                "cp-1", "std-1", "Goal", "Full Stack Developer", "Software Development",
                new ArrayList<>(List.of(new SkillItem("Git", "INTERMEDIATE"))),
                new ArrayList<>(), new ArrayList<>()
        );
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> careerService.deleteSkill("Docker", studentA));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("calculateSkillGaps accurately classifies STRONG, PARTIAL, and GAP skills")
    void testCalculateSkillGapsLogic() {
        List<SkillItem> skills = List.of(
                new SkillItem("JavaScript", "ADVANCED"),     // Strong
                new SkillItem("React", "BEGINNER"),          // Partial
                new SkillItem("Node.js", "INTERMEDIATE")     // Strong
                // Missing from Full Stack: REST APIs, SQL, Git, Docker (4 GAPs)
        );

        SkillGapAnalysisDto gaps = careerService.calculateSkillGaps(skills, "Full Stack Developer");

        assertEquals(7, gaps.getTotalRequired());
        assertEquals(2, gaps.getStrongCount());
        assertEquals(1, gaps.getPartialCount());
        assertEquals(4, gaps.getGapCount());
        assertEquals(42.86, gaps.getCoveragePercentage());
        assertEquals(List.of("JavaScript", "Node.js"), gaps.getStrongList());
        assertEquals(List.of("React"), gaps.getPartialList());
        assertEquals(List.of("REST APIs", "SQL", "Git", "Docker"), gaps.getMissingList());
    }

    @Test
    @DisplayName("runCareerAdvisor throws 400 when profile does not exist")
    void testRunCareerAdvisorProfileNotFound() {
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> careerService.runCareerAdvisor(studentA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Career profile required"));
    }

    @Test
    @DisplayName("runCareerAdvisor returns advisor recommendation when profile exists")
    void testRunCareerAdvisorSuccess() {
        CareerProfile profile = new CareerProfile(
                "cp-1", "std-1", "Lead Dev", "Full Stack Developer", "Software Development",
                List.of(new SkillItem("JavaScript", "ADVANCED")),
                List.of(), List.of()
        );
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.of(profile));
        when(quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(List.of());

        CareerAdvisorDataDto expectedAdvice = new CareerAdvisorDataDto(
                "Strategy", "Focus on React and Docker", null, false
        );
        when(advisorService.generateCareerAdvice(eq(profile), any(SkillGapAnalysisDto.class), anyList()))
                .thenReturn(expectedAdvice);

        CareerAdvisorResponse response = careerService.runCareerAdvisor(studentA);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("Strategy", response.getData().getSummary());
        assertEquals("Focus on React and Docker", response.getData().getRecommendation());
    }

    @Test
    @DisplayName("runLearningRoadmap throws 400 when profile does not exist")
    void testRunLearningRoadmapProfileNotFound() {
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> careerService.runLearningRoadmap(studentA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Career profile required"));
    }

    @Test
    @DisplayName("runLearningRoadmap returns roadmap phases when profile exists")
    void testRunLearningRoadmapSuccess() {
        CareerProfile profile = new CareerProfile(
                "cp-1", "std-1", "Lead Dev", "Full Stack Developer", "Software Development",
                List.of(new SkillItem("JavaScript", "ADVANCED")),
                List.of(), List.of()
        );
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.of(profile));

        CareerRoadmapDataDto expectedRoadmap = new CareerRoadmapDataDto(
                "Roadmap summary",
                List.of(new RoadmapPhaseDto("Phase 1: Foundations", List.of("React"), "HIGH", "Focus", "2 weeks")),
                false
        );
        when(advisorService.generateLearningRoadmap(eq(profile), any(SkillGapAnalysisDto.class)))
                .thenReturn(expectedRoadmap);

        CareerRoadmapResponse response = careerService.runLearningRoadmap(studentA);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("Roadmap summary", response.getData().getSummary());
        assertEquals(1, response.getData().getPhases().size());
        assertEquals("Phase 1: Foundations", response.getData().getPhases().get(0).getPhase());
    }

    @Test
    @DisplayName("Student isolation: Student A cannot run advisor with Student B's profile")
    void testAdvisorStudentIsolation() {
        // Only student B has a profile
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> careerService.runCareerAdvisor(studentA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(careerProfileRepository, never()).findByStudentId("std-2");
    }

    @Test
    @DisplayName("Unauthenticated principal throws 401 AuthException")
    void testUnauthenticatedPrincipalThrows() {
        assertThrows(AuthException.class, () -> careerService.getCareerProfile(null));
    }
}
