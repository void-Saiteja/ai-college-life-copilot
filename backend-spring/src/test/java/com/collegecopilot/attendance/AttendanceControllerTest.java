package com.collegecopilot.attendance;

import com.collegecopilot.attendance.dto.*;
import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.security.JwtAuthenticationFilter;
import com.collegecopilot.security.JwtTokenProvider;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AttendanceService attendanceService;

    @InjectMocks
    private AttendanceController attendanceController;

    private JwtTokenProvider tokenProvider;
    private String validToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        validToken = tokenProvider.generateToken("user-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(attendanceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/attendance with valid JWT returns 200 and student attendance records")
    void testGetAttendanceSuccess() throws Exception {
        AttendanceMetrics metrics = new AttendanceMetrics(40, 36, 90.00, 75, 8, 0, "Safe / Good (>=80%)", "SAFE");
        AttendanceRecordDto record = new AttendanceRecordDto("att-1", "sub-1", "CS101", "Calculus", "Dr. Ramanujan", metrics);

        when(attendanceService.getAttendanceRecords("user-1", 75)).thenReturn(List.of(record));

        mockMvc.perform(get("/api/attendance?target=75")
                        .header("Authorization", "Bearer " + validToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("att-1"))
                .andExpect(jsonPath("$.data[0].subjectId").value("sub-1"))
                .andExpect(jsonPath("$.data[0].subjectCode").value("CS101"))
                .andExpect(jsonPath("$.data[0].percentage").value(90.0))
                .andExpect(jsonPath("$.data[0].riskClassification").value("SAFE"))
                .andExpect(jsonPath("$.data[0].maxCanMiss").value(8));
    }

    @Test
    @DisplayName("GET /api/attendance without JWT returns 401 Unauthorized")
    void testGetAttendanceUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/attendance")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized: Student authentication required"));
    }

    @Test
    @DisplayName("PUT /api/attendance/{id} updating own record returns 200")
    void testUpdateAttendanceSuccess() throws Exception {
        AttendanceMetrics metrics = new AttendanceMetrics(45, 40, 88.89, 75, 8, 0, "Safe / Good (>=80%)", "SAFE");
        AttendanceRecordDto updatedRecord = new AttendanceRecordDto("att-1", "sub-1", "CS101", "Calculus", null, metrics);

        when(attendanceService.updateAttendance(eq("user-1"), eq("att-1"), any(AttendanceUpdateRequest.class)))
                .thenReturn(updatedRecord);

        mockMvc.perform(put("/api/attendance/att-1")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classes_conducted\": 45, \"classes_attended\": 40}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("att-1"))
                .andExpect(jsonPath("$.data.conducted").value(45))
                .andExpect(jsonPath("$.data.attended").value(40))
                .andExpect(jsonPath("$.data.percentage").value(88.89));
    }

    @Test
    @DisplayName("PUT /api/attendance/{id} cross-student update returns 403 Forbidden")
    void testUpdateAttendanceCrossStudentForbidden() throws Exception {
        when(attendanceService.updateAttendance(eq("user-1"), eq("att-other"), any(AttendanceUpdateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this attendance record"));

        mockMvc.perform(put("/api/attendance/att-other")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classes_conducted\": 40, \"classes_attended\": 30}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied: You do not own this attendance record"));
    }

    @Test
    @DisplayName("PUT /api/attendance/{id} missing record returns 404 Not Found")
    void testUpdateAttendanceNotFound() throws Exception {
        when(attendanceService.updateAttendance(eq("user-1"), eq("att-missing"), any(AttendanceUpdateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance record not found"));

        mockMvc.perform(put("/api/attendance/att-missing")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classes_conducted\": 40, \"classes_attended\": 30}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Attendance record not found"));
    }

    @Test
    @DisplayName("PUT /api/attendance/{id} with invalid payload (negative numbers) returns 400 Bad Request")
    void testUpdateAttendanceValidationFailure() throws Exception {
        mockMvc.perform(put("/api/attendance/att-1")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classes_conducted\": -5, \"classes_attended\": -2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/attendance/projection returns 200 with calculation scenarios")
    void testProjectionSuccess() throws Exception {
        AttendanceMetrics current = new AttendanceMetrics(40, 30, 75.0, 75, 0, 0, "Safe / Good (>=80%)", "SAFE");
        ProjectionDataDto projectionData = new ProjectionDataDto();
        projectionData.setCurrent(current);

        ProjectionScenarioDto ifMiss = new ProjectionScenarioDto();
        ifMiss.setAdditionalMissed(2);
        ifMiss.setProjectedPercentage(71.43);
        projectionData.setIfMissNext(ifMiss);

        ProjectionScenarioDto ifAttend = new ProjectionScenarioDto();
        ifAttend.setAdditionalAttended(4);
        ifAttend.setProjectedPercentage(77.27);
        projectionData.setIfAttendNext(ifAttend);

        when(attendanceService.calculateProjection(eq("user-1"), any(AttendanceProjectionRequest.class)))
                .thenReturn(projectionData);

        mockMvc.perform(post("/api/attendance/projection")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conducted\": 40, \"attended\": 30, \"missClasses\": 2, \"attendClasses\": 4, \"targetPercentage\": 75}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.current.percentage").value(75.0))
                .andExpect(jsonPath("$.data.ifMissNext.additionalMissed").value(2))
                .andExpect(jsonPath("$.data.ifMissNext.projectedPercentage").value(71.43))
                .andExpect(jsonPath("$.data.ifAttendNext.additionalAttended").value(4))
                .andExpect(jsonPath("$.data.ifAttendNext.projectedPercentage").value(77.27))
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/attendance/projection without JWT returns 401")
    void testProjectionUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/attendance/projection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conducted\": 40, \"attended\": 30}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized: Student authentication required"));
    }

    @Test
    @DisplayName("POST /api/attendance/explain returns 200 with explanation and isFallback flag")
    void testExplainAttendanceSuccess() throws Exception {
        AttendanceMetrics metrics = new AttendanceMetrics(40, 26, 65.0, 75, 0, 16, "High Risk (<75%)", "CRITICAL");
        ExplainDataDto data = new ExplainDataDto(metrics, "Your attendance in Calculus is below target...", true);

        when(attendanceService.explainAttendance(eq("user-1"), any(AttendanceExplainRequest.class)))
                .thenReturn(data);

        mockMvc.perform(post("/api/attendance/explain")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\": \"sub-1\", \"conducted\": 40, \"attended\": 26, \"targetPercentage\": 75}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metrics.percentage").value(65.0))
                .andExpect(jsonPath("$.data.metrics.riskClassification").value("CRITICAL"))
                .andExpect(jsonPath("$.data.explanation").isNotEmpty())
                .andExpect(jsonPath("$.data.isFallback").value(true));
    }

    @Test
    @DisplayName("POST /api/attendance/explain without JWT returns 401")
    void testExplainAttendanceUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/attendance/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\": \"sub-1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized: Student authentication required"));
    }
}
