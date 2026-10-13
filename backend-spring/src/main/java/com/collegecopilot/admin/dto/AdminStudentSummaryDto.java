package com.collegecopilot.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminStudentSummaryDto {

    private String id;
    private String userId;
    private String name;
    private String email;
    private String studentCode;
    private Integer semester;
    private String department;
    private BigDecimal gpa;
    private AttendanceSummary attendance;
    private int pendingAssignmentsCount;
    private int overdueAssignmentsCount;
    private int upcomingExamsCount;
    private int quizAttemptsCount;
    private Double averageQuizScore;
    private String targetRole;
    private Double skillCoverage;
    private List<String> riskIndicators;

    public AdminStudentSummaryDto() {
    }

    public AdminStudentSummaryDto(String id, String userId, String name, String email, String studentCode,
                                  Integer semester, String department, BigDecimal gpa,
                                  AttendanceSummary attendance, int pendingAssignmentsCount,
                                  int overdueAssignmentsCount, int upcomingExamsCount,
                                  int quizAttemptsCount, Double averageQuizScore,
                                  String targetRole, Double skillCoverage,
                                  List<String> riskIndicators) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.studentCode = studentCode;
        this.semester = semester;
        this.department = department;
        this.gpa = gpa;
        this.attendance = attendance;
        this.pendingAssignmentsCount = pendingAssignmentsCount;
        this.overdueAssignmentsCount = overdueAssignmentsCount;
        this.upcomingExamsCount = upcomingExamsCount;
        this.quizAttemptsCount = quizAttemptsCount;
        this.averageQuizScore = averageQuizScore;
        this.targetRole = targetRole;
        this.skillCoverage = skillCoverage;
        this.riskIndicators = riskIndicators;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public BigDecimal getGpa() { return gpa; }
    public void setGpa(BigDecimal gpa) { this.gpa = gpa; }

    public AttendanceSummary getAttendance() { return attendance; }
    public void setAttendance(AttendanceSummary attendance) { this.attendance = attendance; }

    public int getPendingAssignmentsCount() { return pendingAssignmentsCount; }
    public void setPendingAssignmentsCount(int pendingAssignmentsCount) { this.pendingAssignmentsCount = pendingAssignmentsCount; }

    public int getOverdueAssignmentsCount() { return overdueAssignmentsCount; }
    public void setOverdueAssignmentsCount(int overdueAssignmentsCount) { this.overdueAssignmentsCount = overdueAssignmentsCount; }

    public int getUpcomingExamsCount() { return upcomingExamsCount; }
    public void setUpcomingExamsCount(int upcomingExamsCount) { this.upcomingExamsCount = upcomingExamsCount; }

    public int getQuizAttemptsCount() { return quizAttemptsCount; }
    public void setQuizAttemptsCount(int quizAttemptsCount) { this.quizAttemptsCount = quizAttemptsCount; }

    public Double getAverageQuizScore() { return averageQuizScore; }
    public void setAverageQuizScore(Double averageQuizScore) { this.averageQuizScore = averageQuizScore; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public Double getSkillCoverage() { return skillCoverage; }
    public void setSkillCoverage(Double skillCoverage) { this.skillCoverage = skillCoverage; }

    public List<String> getRiskIndicators() { return riskIndicators; }
    public void setRiskIndicators(List<String> riskIndicators) { this.riskIndicators = riskIndicators; }

    public static class AttendanceSummary {
        private int conducted;
        private int attended;
        private double percentage;
        private String riskClassification;
        private String status;

        public AttendanceSummary() {}
        public AttendanceSummary(int conducted, int attended, double percentage, String riskClassification, String status) {
            this.conducted = conducted;
            this.attended = attended;
            this.percentage = percentage;
            this.riskClassification = riskClassification;
            this.status = status;
        }

        public int getConducted() { return conducted; }
        public void setConducted(int conducted) { this.conducted = conducted; }

        public int getAttended() { return attended; }
        public void setAttended(int attended) { this.attended = attended; }

        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }

        public String getRiskClassification() { return riskClassification; }
        public void setRiskClassification(String riskClassification) { this.riskClassification = riskClassification; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
