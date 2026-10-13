package com.collegecopilot.attendance;

import com.collegecopilot.attendance.dto.*;
import com.collegecopilot.student.Attendance;
import com.collegecopilot.student.AttendanceRepository;
import com.collegecopilot.student.Student;
import com.collegecopilot.student.StudentRepository;
import com.collegecopilot.student.Subject;
import com.collegecopilot.student.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final GeminiService geminiService;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            StudentRepository studentRepository,
            SubjectRepository subjectRepository,
            GeminiService geminiService) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.geminiService = geminiService;
    }

    public AttendanceMetrics calculateAttendanceMetrics(int conducted, int attended, int targetPercentage) {
        int total = Math.max(0, conducted);
        int att = Math.max(0, attended);
        int target = Math.max(1, Math.min(100, targetPercentage));

        if (total == 0) {
            return new AttendanceMetrics(
                    0, 0, 0.00, target, 0, 0,
                    "No Classes Conducted", "NO_CLASSES"
            );
        }

        double rawPercentage = ((double) att / total) * 100.0;
        double percentage = BigDecimal.valueOf(rawPercentage).setScale(2, RoundingMode.HALF_UP).doubleValue();
        double targetDecimal = (double) target / 100.0;

        int maxCanMiss;
        int minRequiredToAttend;

        if (percentage >= target) {
            maxCanMiss = Math.max(0, (int) Math.floor(((double) att / targetDecimal) - total + 1e-9));
            minRequiredToAttend = 0;
        } else {
            maxCanMiss = 0;
            double required = ((targetDecimal * total) - att) / (1.0 - targetDecimal);
            minRequiredToAttend = Math.max(0, (int) Math.ceil(required - 1e-9));
        }

        String riskClassification;
        String status;

        if (percentage < target) {
            riskClassification = "CRITICAL";
            status = "High Risk (<" + target + "%)";
        } else if (percentage < target + 5) {
            riskClassification = "WARNING";
            status = "At Risk / Warning (" + target + "-" + (target + 5) + "%)";
        } else {
            riskClassification = "SAFE";
            status = "Safe / Good (>=" + (target + 5) + "%)";
        }

        return new AttendanceMetrics(
                total, att, percentage, target,
                maxCanMiss, minRequiredToAttend, status, riskClassification
        );
    }

    public ProjectionDataDto calculateProjectionMetrics(
            int conducted,
            int attended,
            int futureMissed,
            int futureAttended,
            int targetPercentage) {

        int target = targetPercentage <= 0 ? 75 : targetPercentage;
        AttendanceMetrics current = calculateAttendanceMetrics(conducted, attended, target);

        int futAtt = Math.max(0, futureAttended);
        int futMiss = Math.max(0, futureMissed);

        int projectedConducted = current.getConducted() + futAtt + futMiss;
        int projectedAttended = current.getAttended() + futAtt;

        AttendanceMetrics projected = calculateAttendanceMetrics(projectedConducted, projectedAttended, target);

        Map<String, Object> futureInputMap = new LinkedHashMap<>();
        futureInputMap.put("futureAttended", futAtt);
        futureInputMap.put("futureMissed", futMiss);

        Map<String, Object> projectedMap = new LinkedHashMap<>();
        projectedMap.put("conducted", projected.getConducted());
        projectedMap.put("attended", projected.getAttended());
        projectedMap.put("percentage", projected.getPercentage());
        projectedMap.put("status", projected.getStatus());
        projectedMap.put("riskClassification", projected.getRiskClassification());
        projectedMap.put("maxCanMiss", projected.getMaxCanMiss());
        projectedMap.put("minRequiredToAttend", projected.getMinRequiredToAttend());

        String disclaimer = "Note: These calculations are mathematical projections for academic planning.";

        Map<String, Object> fullProjection = new LinkedHashMap<>();
        fullProjection.put("current", current);
        fullProjection.put("futureInput", futureInputMap);
        fullProjection.put("projected", projectedMap);
        fullProjection.put("disclaimer", disclaimer);

        // ifMissNext scenario
        int missConducted = current.getConducted() + futMiss;
        double missPctRaw = ((double) current.getAttended() / (missConducted > 0 ? missConducted : 1)) * 100.0;
        double missPercentage = BigDecimal.valueOf(missPctRaw).setScale(2, RoundingMode.HALF_UP).doubleValue();

        ProjectionScenarioDto ifMissNext = new ProjectionScenarioDto();
        ifMissNext.setAdditionalMissed(futMiss);
        ifMissNext.setProjectedConducted(projected.getConducted());
        ifMissNext.setProjectedAttended(current.getAttended());
        ifMissNext.setProjectedPercentage(missPercentage);
        ifMissNext.setStatus(projected.getStatus());
        ifMissNext.setRiskClassification(projected.getRiskClassification());
        ifMissNext.setMaxCanMiss(projected.getMaxCanMiss());
        ifMissNext.setMinRequiredToAttend(projected.getMinRequiredToAttend());

        // ifAttendNext scenario
        int attendConducted = current.getConducted() + futAtt;
        int attendAttended = current.getAttended() + futAtt;
        double attendPctRaw = ((double) attendAttended / (attendConducted > 0 ? attendConducted : 1)) * 100.0;
        double attendPercentage = BigDecimal.valueOf(attendPctRaw).setScale(2, RoundingMode.HALF_UP).doubleValue();

        ProjectionScenarioDto ifAttendNext = new ProjectionScenarioDto();
        ifAttendNext.setAdditionalAttended(futAtt);
        ifAttendNext.setProjectedConducted(attendConducted);
        ifAttendNext.setProjectedAttended(attendAttended);
        ifAttendNext.setProjectedPercentage(attendPercentage);
        ifAttendNext.setStatus(projected.getStatus());
        ifAttendNext.setRiskClassification(projected.getRiskClassification());
        ifAttendNext.setMaxCanMiss(projected.getMaxCanMiss());
        ifAttendNext.setMinRequiredToAttend(projected.getMinRequiredToAttend());

        ProjectionDataDto responseData = new ProjectionDataDto();
        responseData.setCurrent(current);
        responseData.setIfMissNext(ifMissNext);
        responseData.setIfAttendNext(ifAttendNext);
        responseData.setFullProjection(fullProjection);
        responseData.setDisclaimer(disclaimer);

        return responseData;
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getAttendanceRecords(String userId, Integer target) {
        Student student = getStudentByUserId(userId);
        int targetPercentage = (target != null && target > 0) ? target : 75;

        List<Attendance> records = attendanceRepository.findByStudentId(student.getId());
        List<AttendanceRecordDto> dtoList = new ArrayList<>();

        for (Attendance a : records) {
            Subject subject = subjectRepository.findById(a.getSubjectId()).orElse(null);
            AttendanceMetrics metrics = calculateAttendanceMetrics(
                    a.getClassesConducted(),
                    a.getClassesAttended(),
                    targetPercentage
            );

            AttendanceRecordDto dto = new AttendanceRecordDto(
                    a.getId(),
                    a.getSubjectId(),
                    subject != null ? subject.getCode() : "CS",
                    subject != null ? subject.getName() : "Subject",
                    subject != null ? subject.getInstructor() : "Faculty",
                    metrics
            );
            dtoList.add(dto);
        }

        return dtoList;
    }

    @Transactional
    public AttendanceRecordDto updateAttendance(String userId, String attendanceId, AttendanceUpdateRequest request) {
        Student student = getStudentByUserId(userId);

        Attendance record = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance record not found"));

        if (!record.getStudentId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this attendance record");
        }

        record.setClassesConducted(request.getClassesConducted());
        record.setClassesAttended(request.getClassesAttended());
        attendanceRepository.save(record);

        Subject subject = subjectRepository.findById(record.getSubjectId()).orElse(null);
        AttendanceMetrics metrics = calculateAttendanceMetrics(
                record.getClassesConducted(),
                record.getClassesAttended(),
                75
        );

        return new AttendanceRecordDto(
                record.getId(),
                record.getSubjectId(),
                subject != null ? subject.getCode() : "CS",
                subject != null ? subject.getName() : "Subject",
                subject != null ? subject.getInstructor() : null,
                metrics
        );
    }

    @Transactional(readOnly = true)
    public ProjectionDataDto calculateProjection(String userId, AttendanceProjectionRequest req) {
        getStudentByUserId(userId);

        return calculateProjectionMetrics(
                req.getConducted() != null ? req.getConducted() : 0,
                req.getAttended() != null ? req.getAttended() : 0,
                req.getMissClasses() != null ? req.getMissClasses() : 0,
                req.getAttendClasses() != null ? req.getAttendClasses() : 0,
                req.getTargetPercentage() != null ? req.getTargetPercentage() : 75
        );
    }

    @Transactional(readOnly = true)
    public ExplainDataDto explainAttendance(String userId, AttendanceExplainRequest req) {
        Student student = getStudentByUserId(userId);

        String subjectName = "General Subject";
        String subjectCode = "CS";

        if (req.getSubjectId() != null && !req.getSubjectId().isBlank()) {
            Subject subject = subjectRepository.findById(req.getSubjectId()).orElse(null);
            Attendance record = attendanceRepository.findBySubjectIdAndStudentId(req.getSubjectId(), student.getId()).orElse(null);

            if (subject == null || record == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject or attendance record not found for student");
            }

            subjectName = subject.getName();
            subjectCode = subject.getCode();
        }

        int target = req.getTargetPercentage() != null ? req.getTargetPercentage() : 75;
        AttendanceMetrics metrics = calculateAttendanceMetrics(
                req.getConducted() != null ? req.getConducted() : 0,
                req.getAttended() != null ? req.getAttended() : 0,
                target
        );

        GeminiService.ExplanationResult aiResult = geminiService.generateExplanation(subjectCode, subjectName, metrics);

        return new ExplainDataDto(metrics, aiResult.explanation(), aiResult.isFallback());
    }

    private Student getStudentByUserId(String userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required"));
    }
}
