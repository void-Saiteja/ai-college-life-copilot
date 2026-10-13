package com.collegecopilot.attendance;

import com.collegecopilot.attendance.dto.*;
import com.collegecopilot.auth.AuthException;
import com.collegecopilot.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public ResponseEntity<AttendanceListResponse> getAttendance(
            @RequestParam(value = "target", required = false) Integer target) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        List<AttendanceRecordDto> records = attendanceService.getAttendanceRecords(principal.getId(), target);
        return ResponseEntity.ok(new AttendanceListResponse(true, records));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttendanceSingleResponse> updateAttendance(
            @PathVariable("id") String id,
            @Valid @RequestBody AttendanceUpdateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        AttendanceRecordDto updated = attendanceService.updateAttendance(principal.getId(), id, request);
        return ResponseEntity.ok(new AttendanceSingleResponse(true, updated));
    }

    @PostMapping("/projection")
    public ResponseEntity<AttendanceProjectionResponse> calculateProjection(
            @RequestBody(required = false) AttendanceProjectionRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        AttendanceProjectionRequest req = (request != null) ? request : new AttendanceProjectionRequest();
        ProjectionDataDto projectionData = attendanceService.calculateProjection(principal.getId(), req);
        return ResponseEntity.ok(new AttendanceProjectionResponse(true, projectionData));
    }

    @PostMapping("/explain")
    public ResponseEntity<AttendanceExplainResponse> explainAttendance(
            @RequestBody(required = false) AttendanceExplainRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        AttendanceExplainRequest req = (request != null) ? request : new AttendanceExplainRequest();
        ExplainDataDto explainData = attendanceService.explainAttendance(principal.getId(), req);
        return ResponseEntity.ok(new AttendanceExplainResponse(true, explainData));
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        return principal;
    }
}
