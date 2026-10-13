package com.collegecopilot.schedule;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.schedule.dto.*;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.student.Student;
import com.collegecopilot.student.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ScheduleService {

    public static final List<String> VALID_DAYS = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

    private final ScheduleRepository scheduleRepository;
    private final StudentRepository studentRepository;

    @Autowired
    public ScheduleService(ScheduleRepository scheduleRepository, StudentRepository studentRepository) {
        this.scheduleRepository = scheduleRepository;
        this.studentRepository = studentRepository;
    }

    private String resolveStudentId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        String studentKey = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        if (studentKey == null || studentKey.isBlank()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }

        Optional<Student> direct = studentRepository.findById(studentKey);
        if (direct.isPresent()) {
            return direct.get().getId();
        }

        Optional<Student> byUser = studentRepository.findByUserId(studentKey);
        return byUser.map(Student::getId).orElse(studentKey);
    }

    public ScheduleListResponse getSchedule(UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        List<ClassSchedule> list = scheduleRepository.findByStudentIdOrderByCreatedAtAsc(studentId);
        List<ScheduleItemDto> dtoList = list.stream().map(this::toDto).toList();

        return new ScheduleListResponse(true, dtoList);
    }

    @Transactional
    public ScheduleSingleResponse addScheduleItem(ScheduleCreateRequest request, UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule payload is required");
        }

        String course = request.getCourse();
        if (course == null || course.isBlank() || course.trim().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid course name is required (1-100 characters)");
        }

        String time = request.getTime();
        if (time == null || time.isBlank() || time.trim().length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid time slot is required (1-50 characters)");
        }

        String room = request.getRoom();
        if (room != null && room.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room location must be a string up to 50 characters");
        }

        String instructor = request.getInstructor();
        if (instructor != null && instructor.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Instructor must be a string up to 100 characters");
        }

        List<String> rawDays = parseDays(request.getDays());
        if (rawDays.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one valid active day is required");
        }

        for (String day : rawDays) {
            if (!VALID_DAYS.contains(day)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid day '" + day + "'. Allowed days: " + String.join(", ", VALID_DAYS)
                );
            }
        }

        String id = "sch-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);

        ClassSchedule schedule = new ClassSchedule(
                id,
                studentId,
                course.trim(),
                (room != null && !room.isBlank()) ? room.trim() : "TBD",
                rawDays,
                time.trim(),
                (instructor != null && !instructor.isBlank()) ? instructor.trim() : "Staff"
        );

        ClassSchedule saved = scheduleRepository.save(schedule);
        return new ScheduleSingleResponse(true, toDto(saved));
    }

    @Transactional
    public ScheduleDeleteResponse deleteScheduleItem(String id, UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        ClassSchedule item = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class schedule item not found"));

        boolean isOwner = studentId.equals(item.getStudentId())
                || (item.getStudentId() == null && "std-1".equals(studentId));

        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this schedule item");
        }

        scheduleRepository.delete(item);
        return new ScheduleDeleteResponse(true, "Class schedule item removed");
    }

    private List<String> parseDays(Object rawDays) {
        List<String> result = new ArrayList<>();
        if (rawDays == null) {
            return result;
        }

        if (rawDays instanceof List<?> list) {
            for (Object obj : list) {
                if (obj != null) {
                    result.add(obj.toString().trim());
                }
            }
        } else if (rawDays instanceof String str && !str.isBlank()) {
            if (str.startsWith("[") && str.endsWith("]")) {
                String clean = str.replace("[", "").replace("]", "").replace("\"", "").replace("'", "");
                for (String part : clean.split(",")) {
                    if (!part.trim().isEmpty()) {
                        result.add(part.trim());
                    }
                }
            } else {
                result.add(str.trim());
            }
        }
        return result;
    }

    private ScheduleItemDto toDto(ClassSchedule s) {
        String createdAtStr = s.getCreatedAt() != null
                ? s.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String updatedAtStr = s.getUpdatedAt() != null
                ? s.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : createdAtStr;

        return new ScheduleItemDto(
                s.getId(),
                s.getStudentId(),
                s.getCourse(),
                s.getRoom(),
                s.getDays() != null ? s.getDays() : new ArrayList<>(),
                s.getTime(),
                s.getInstructor(),
                createdAtStr,
                updatedAtStr
        );
    }
}
