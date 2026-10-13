package com.collegecopilot.admin;

import com.collegecopilot.admin.dto.AdminDashboardDataDto;
import com.collegecopilot.admin.dto.AdminInsightsDataDto;
import com.collegecopilot.admin.dto.AdminRisksDataDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiAdminInsightsServiceTest {

    @Test
    @DisplayName("generateInsights returns deterministic fallback when API key is missing or mock")
    void generateInsights_returnsDeterministicFallback() {
        GeminiAdminInsightsService service = new GeminiAdminInsightsService("mock_key");

        AdminDashboardDataDto.StudentsStats students = new AdminDashboardDataDto.StudentsStats(50, 45);
        AdminDashboardDataDto.AttendanceStats attendance = new AdminDashboardDataDto.AttendanceStats(45, 82.5, 3, 5);
        AdminDashboardDataDto.AssignmentsStats assignments = new AdminDashboardDataDto.AssignmentsStats(20, 8, 12, 2, 4);
        AdminDashboardDataDto.ExamsStats exams = new AdminDashboardDataDto.ExamsStats(6, 3, 1, 3);
        AdminDashboardDataDto.QuizzesStats quizzes = new AdminDashboardDataDto.QuizzesStats(30, 75.0, 4, 2);
        AdminDashboardDataDto.CareerStats career = new AdminDashboardDataDto.CareerStats(40, 10,
                List.of(new AdminDashboardDataDto.SkillGapCount("Docker", 8), new AdminDashboardDataDto.SkillGapCount("SQL", 6)));
        AdminDashboardDataDto.DocumentsStats documents = new AdminDashboardDataDto.DocumentsStats(5, 50, "Google Gemini gemini-embedding-2", "Synchronized");

        AdminDashboardDataDto stats = new AdminDashboardDataDto(
                students, attendance, assignments, exams, quizzes, career, documents
        );

        AdminRisksDataDto.AttendanceRiskItem attRisk = new AdminRisksDataDto.AttendanceRiskItem("std-1", "John", "CS", 1, 68.0, 20, 13, "CRITICAL");
        AdminRisksDataDto.AssignmentRiskItem asgRisk = new AdminRisksDataDto.AssignmentRiskItem("std-2", "Jane", "CS", 2, 3);
        AdminRisksDataDto.ExamRiskItem exRisk = new AdminRisksDataDto.ExamRiskItem("std-3", "Bob", "CS", 1, true);

        AdminRisksDataDto risks = new AdminRisksDataDto(
                List.of(attRisk),
                List.of(asgRisk),
                List.of(exRisk),
                List.of(),
                List.of()
        );

        AdminInsightsDataDto insights = service.generateInsights(stats, risks);

        assertThat(insights).isNotNull();
        assertThat(insights.getIsAiGenerated()).isFalse();
        assertThat(insights.getKeyObservations()).hasSize(5);
        assertThat(insights.getKeyObservations().get(0)).contains("50 total students enrolled");
        assertThat(insights.getAreasRequiringAttention()).hasSize(3);
        assertThat(insights.getAreasRequiringAttention().get(0)).contains("1 student(s) currently breach the 75% attendance policy");
        assertThat(insights.getAreasRequiringAttention().get(1)).contains("1 student(s) have overdue assignment deadlines");
        assertThat(insights.getAreasRequiringAttention().get(2)).contains("1 student(s) have exams scheduled today");
        assertThat(insights.getSuggestedAdministrativeActions()).isNotEmpty();
        assertThat(insights.getCaveatsAndLimitations()).contains("Deterministic summary");
    }

    @Test
    @DisplayName("generateDeterministicFallback handles null stats and risks gracefully")
    void generateDeterministicFallback_nullStats_handlesGracefully() {
        GeminiAdminInsightsService service = new GeminiAdminInsightsService("");

        AdminInsightsDataDto fallback = service.generateDeterministicFallbackInsights(null, null);

        assertThat(fallback).isNotNull();
        assertThat(fallback.getKeyObservations()).isNotEmpty();
        assertThat(fallback.getAreasRequiringAttention()).contains("All enrolled students currently meet baseline academic engagement benchmarks.");
        assertThat(fallback.getIsAiGenerated()).isFalse();
    }
}
