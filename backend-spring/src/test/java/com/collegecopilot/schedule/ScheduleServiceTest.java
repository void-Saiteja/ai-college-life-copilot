package com.collegecopilot.schedule;

import com.collegecopilot.schedule.dto.*;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private StudentRepository studentRepository;

    private ScheduleService scheduleService;

    private UserPrincipal studentA;
    private UserPrincipal studentB;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(scheduleRepository, studentRepository);

        studentA = new UserPrincipal("u-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);
        studentB = new UserPrincipal("u-2", "Bob Smith", "bob@student.edu", "STUDENT", "std-2", null);

        lenient().when(studentRepository.findById("std-1")).thenReturn(Optional.of(new Student("std-1", "u-1", "CS101", 1, "CS", null)));
        lenient().when(studentRepository.findById("std-2")).thenReturn(Optional.of(new Student("std-2", "u-2", "CS102", 1, "CS", null)));
    }

    @Test
    @DisplayName("getSchedule returns student's schedule items")
    void testGetScheduleReturnsItems() {
        ClassSchedule item = new ClassSchedule("sch-1", "std-1", "CS 201: Data Structures", "Hall B2", List.of("Mon", "Wed", "Fri"), "09:00 AM - 10:30 AM", "Dr. Alan Turing");
        when(scheduleRepository.findByStudentIdOrderByCreatedAtAsc("std-1")).thenReturn(List.of(item));

        ScheduleListResponse response = scheduleService.getSchedule(studentA);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("CS 201: Data Structures", response.getData().get(0).getCourse());
        assertEquals("Hall B2", response.getData().get(0).getRoom());
        assertEquals(List.of("Mon", "Wed", "Fri"), response.getData().get(0).getDays());
    }

    @Test
    @DisplayName("addScheduleItem succeeds with valid input and sets defaults")
    void testAddScheduleItemSuccess() {
        ScheduleCreateRequest req = new ScheduleCreateRequest("MATH 220: Calculus III", null, List.of("Tue", "Thu"), "11:00 AM - 12:30 PM", null);

        when(scheduleRepository.save(any(ClassSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleSingleResponse response = scheduleService.addScheduleItem(req, studentA);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("MATH 220: Calculus III", response.getData().getCourse());
        assertEquals("TBD", response.getData().getRoom());
        assertEquals("Staff", response.getData().getInstructor());
        assertEquals(List.of("Tue", "Thu"), response.getData().getDays());
        assertEquals("std-1", response.getData().getStudentId());
    }

    @Test
    @DisplayName("addScheduleItem throws 400 when course is missing or empty")
    void testAddScheduleItemMissingCourse() {
        ScheduleCreateRequest req = new ScheduleCreateRequest("", "Hall A", List.of("Mon"), "10:00 AM", "Staff");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                scheduleService.addScheduleItem(req, studentA));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Valid course name is required"));
    }

    @Test
    @DisplayName("addScheduleItem throws 400 when course exceeds 100 characters")
    void testAddScheduleItemCourseTooLong() {
        ScheduleCreateRequest req = new ScheduleCreateRequest("A".repeat(101), "Hall A", List.of("Mon"), "10:00 AM", "Staff");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                scheduleService.addScheduleItem(req, studentA));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Valid course name is required"));
    }

    @Test
    @DisplayName("addScheduleItem throws 400 when time is missing or empty")
    void testAddScheduleItemMissingTime() {
        ScheduleCreateRequest req = new ScheduleCreateRequest("CS 101", "Hall A", List.of("Mon"), "  ", "Staff");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                scheduleService.addScheduleItem(req, studentA));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Valid time slot is required"));
    }

    @Test
    @DisplayName("addScheduleItem throws 400 when room exceeds 50 characters")
    void testAddScheduleItemRoomTooLong() {
        ScheduleCreateRequest req = new ScheduleCreateRequest("CS 101", "R".repeat(51), List.of("Mon"), "10:00 AM", "Staff");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                scheduleService.addScheduleItem(req, studentA));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Room location must be a string up to 50 characters"));
    }

    @Test
    @DisplayName("addScheduleItem throws 400 when instructor exceeds 100 characters")
    void testAddScheduleItemInstructorTooLong() {
        ScheduleCreateRequest req = new ScheduleCreateRequest("CS 101", "Hall A", List.of("Mon"), "10:00 AM", "I".repeat(101));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                scheduleService.addScheduleItem(req, studentA));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Instructor must be a string up to 100 characters"));
    }

    @Test
    @DisplayName("addScheduleItem throws 400 when days list is empty")
    void testAddScheduleItemEmptyDays() {
        ScheduleCreateRequest req = new ScheduleCreateRequest("CS 101", "Hall A", List.of(), "10:00 AM", "Staff");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                scheduleService.addScheduleItem(req, studentA));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("At least one valid active day is required"));
    }

    @Test
    @DisplayName("addScheduleItem throws 400 when invalid weekday is provided")
    void testAddScheduleItemInvalidWeekday() {
        ScheduleCreateRequest req = new ScheduleCreateRequest("CS 101", "Hall A", List.of("Mon", "Funday"), "10:00 AM", "Staff");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                scheduleService.addScheduleItem(req, studentA));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid day 'Funday'"));
    }

    @Test
    @DisplayName("deleteScheduleItem succeeds for owner")
    void testDeleteScheduleItemOwnerSuccess() {
        ClassSchedule item = new ClassSchedule("sch-1", "std-1", "CS 201", "Hall B", List.of("Mon"), "10:00 AM", "Staff");
        when(scheduleRepository.findById("sch-1")).thenReturn(Optional.of(item));

        ScheduleDeleteResponse response = scheduleService.deleteScheduleItem("sch-1", studentA);

        assertTrue(response.isSuccess());
        assertEquals("Class schedule item removed", response.getMessage());
        verify(scheduleRepository).delete(item);
    }

    @Test
    @DisplayName("deleteScheduleItem throws 404 when item does not exist")
    void testDeleteScheduleItemNotFound() {
        when(scheduleRepository.findById("sch-999")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                scheduleService.deleteScheduleItem("sch-999", studentA));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Class schedule item not found"));
    }

    @Test
    @DisplayName("deleteScheduleItem throws 403 on cross-student deletion attempt (IDOR)")
    void testDeleteScheduleItemIdorForbidden() {
        ClassSchedule item = new ClassSchedule("sch-1", "std-1", "CS 201", "Hall B", List.of("Mon"), "10:00 AM", "Staff");
        when(scheduleRepository.findById("sch-1")).thenReturn(Optional.of(item));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                scheduleService.deleteScheduleItem("sch-1", studentB));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Access denied: You do not own this schedule item"));
        verify(scheduleRepository, never()).delete(any());
    }
}
