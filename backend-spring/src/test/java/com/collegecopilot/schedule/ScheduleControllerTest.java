package com.collegecopilot.schedule;

import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.schedule.dto.*;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private ScheduleController scheduleController;

    private JwtTokenProvider tokenProvider;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        tokenA = tokenProvider.generateToken("u-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);
        tokenB = tokenProvider.generateToken("u-2", "Bob Smith", "bob@student.edu", "STUDENT", "std-2", null);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(scheduleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/schedule returns 200 with student schedule when authenticated")
    void testGetScheduleAuthenticatedReturns200() throws Exception {
        ScheduleItemDto item = new ScheduleItemDto(
                "sch-1", "std-1", "CS 201: Data Structures", "Hall B2",
                List.of("Mon", "Wed", "Fri"), "09:00 AM - 10:30 AM", "Dr. Alan Turing",
                "2026-03-30T10:00:00", "2026-03-30T10:00:00"
        );
        when(scheduleService.getSchedule(any(UserPrincipal.class)))
                .thenReturn(new ScheduleListResponse(true, List.of(item)));

        mockMvc.perform(get("/api/schedule")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("sch-1"))
                .andExpect(jsonPath("$.data[0].course").value("CS 201: Data Structures"))
                .andExpect(jsonPath("$.data[0].room").value("Hall B2"))
                .andExpect(jsonPath("$.data[0].time").value("09:00 AM - 10:30 AM"))
                .andExpect(jsonPath("$.data[0].days[0]").value("Mon"));
    }

    @Test
    @DisplayName("GET /api/schedule returns 401 when unauthenticated")
    void testGetScheduleUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/schedule"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/schedule returns 201 with created item when authenticated")
    void testAddScheduleItemAuthenticatedReturns201() throws Exception {
        ScheduleItemDto created = new ScheduleItemDto(
                "sch-2", "std-1", "MATH 220: Calculus III", "Sci 104",
                List.of("Tue", "Thu"), "11:00 AM - 12:30 PM", "Prof. Katherine Johnson",
                "2026-03-30T10:00:00", "2026-03-30T10:00:00"
        );
        when(scheduleService.addScheduleItem(any(ScheduleCreateRequest.class), any(UserPrincipal.class)))
                .thenReturn(new ScheduleSingleResponse(true, created));

        mockMvc.perform(post("/api/schedule")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"course\":\"MATH 220: Calculus III\",\"room\":\"Sci 104\",\"days\":[\"Tue\",\"Thu\"],\"time\":\"11:00 AM - 12:30 PM\",\"instructor\":\"Prof. Katherine Johnson\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("sch-2"))
                .andExpect(jsonPath("$.data.course").value("MATH 220: Calculus III"))
                .andExpect(jsonPath("$.data.student_id").value("std-1"));
    }

    @Test
    @DisplayName("POST /api/schedule returns 400 when validation fails")
    void testAddScheduleItemValidationErrorReturns400() throws Exception {
        when(scheduleService.addScheduleItem(any(ScheduleCreateRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid course name is required (1-100 characters)"));

        mockMvc.perform(post("/api/schedule")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"course\":\"\",\"time\":\"10:00 AM\",\"days\":[\"Mon\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Valid course name is required (1-100 characters)"));
    }

    @Test
    @DisplayName("POST /api/schedule returns 401 when unauthenticated")
    void testAddScheduleItemUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(post("/api/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"course\":\"CS 101\",\"time\":\"10:00 AM\",\"days\":[\"Mon\"]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/schedule/{id} returns 200 when deleting owned item")
    void testDeleteScheduleItemOwnerReturns200() throws Exception {
        when(scheduleService.deleteScheduleItem(eq("sch-1"), any(UserPrincipal.class)))
                .thenReturn(new ScheduleDeleteResponse(true, "Class schedule item removed"));

        mockMvc.perform(delete("/api/schedule/sch-1")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Class schedule item removed"));
    }

    @Test
    @DisplayName("DELETE /api/schedule/{id} returns 403 when trying to delete another student's item")
    void testDeleteScheduleItemCrossStudentReturns403() throws Exception {
        when(scheduleService.deleteScheduleItem(eq("sch-1"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this schedule item"));

        mockMvc.perform(delete("/api/schedule/sch-1")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied: You do not own this schedule item"));
    }

    @Test
    @DisplayName("DELETE /api/schedule/{id} returns 404 when item does not exist")
    void testDeleteScheduleItemNotFoundReturns404() throws Exception {
        when(scheduleService.deleteScheduleItem(eq("sch-999"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Class schedule item not found"));

        mockMvc.perform(delete("/api/schedule/sch-999")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Class schedule item not found"));
    }

    @Test
    @DisplayName("DELETE /api/schedule/{id} returns 401 when unauthenticated")
    void testDeleteScheduleItemUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(delete("/api/schedule/sch-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
