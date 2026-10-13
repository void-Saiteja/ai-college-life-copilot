package com.collegecopilot.studyplanner;

import com.collegecopilot.studyplanner.dto.DailyPlanDto;
import com.collegecopilot.studyplanner.dto.PlannerGenerateRequest;
import com.collegecopilot.studyplanner.dto.StructuredPlanDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeminiStudyPlannerServiceTest {

    @Test
    @DisplayName("generateStructuredPlan uses local deterministic fallback when API key is missing or mock")
    void generateStructuredPlan_whenNoApiKey_usesFallback() {
        GeminiStudyPlannerService service = new GeminiStudyPlannerService("");

        StudentPlannerContext context = new StudentPlannerContext();
        context.setStudentName("Alex Mercer");
        context.setSubjectNames(List.of("Data Structures & Algorithms", "Database Systems"));
        context.setExams(List.of(new StudentPlannerContext.ExamSummary("Midterm Exam", "Data Structures & Algorithms", "2026-10-15", "10:00", "Hall A")));
        context.setAssignments(List.of(new StudentPlannerContext.AssignmentSummary("Lab 4", "Database Systems", "2026-10-12", "High")));
        context.setAttendance(List.of(new StudentPlannerContext.AttendanceSummary("Data Structures & Algorithms", "CS101", 20, 14, 70, "Low Attendance Warning")));

        PlannerGenerateRequest request = new PlannerGenerateRequest("Midterm Sprint", 3.0, "16:00", "21:00", List.of("Mon", "Tue"), "");

        StructuredPlanDto result = service.generateStructuredPlan(context, request);

        assertNotNull(result);
        assertNotNull(result.getSummary());
        assertTrue(result.getSummary().contains("Alex Mercer"));
        assertTrue(result.getSummary().contains("1 pending assignments"));
        assertTrue(result.getSummary().contains("1 upcoming exams"));

        assertNotNull(result.getPriorities());
        assertEquals(2, result.getPriorities().size());
        assertEquals("Data Structures & Algorithms", result.getPriorities().get(0).getSubject());
        assertTrue(result.getPriorities().get(0).getReason().contains("Midterm Exam"));
        assertTrue(result.getPriorities().get(0).getReason().contains("10:00"));
        assertTrue(result.getPriorities().get(0).getReason().contains("Hall A"));

        assertNotNull(result.getDailyPlan());
        assertEquals(2, result.getDailyPlan().size());

        DailyPlanDto day1 = result.getDailyPlan().get(0);
        assertEquals(2, day1.getTasks().size());
        assertEquals("16:00", day1.getTasks().get(0).getStartTime());
        assertEquals("18:00", day1.getTasks().get(0).getEndTime());

        DailyPlanDto day2 = result.getDailyPlan().get(1);
        assertEquals(1, day2.getTasks().size());

        assertNotNull(result.getRecommendations());
        assertEquals(3, result.getRecommendations().size());
    }

    @Test
    @DisplayName("generateLocalFallbackStudyPlan handles empty academic context safely")
    void generateLocalFallbackStudyPlan_emptyContext_succeedsSafely() {
        GeminiStudyPlannerService service = new GeminiStudyPlannerService("");

        StructuredPlanDto result = service.generateLocalFallbackStudyPlan(null, null);

        assertNotNull(result);
        assertNotNull(result.getSummary());
        assertNotNull(result.getPriorities());
        assertNotNull(result.getDailyPlan());
        assertNotNull(result.getRecommendations());
        assertFalse(result.getDailyPlan().isEmpty());
    }
}
