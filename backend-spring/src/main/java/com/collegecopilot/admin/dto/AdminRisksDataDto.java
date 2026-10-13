package com.collegecopilot.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminRisksDataDto {

    private List<AttendanceRiskItem> attendanceRisk;
    private List<AssignmentRiskItem> assignmentRisk;
    private List<ExamRiskItem> examRisk;
    private List<QuizRiskItem> quizRisk;
    private List<CareerGapItem> careerGap;

    public AdminRisksDataDto() {
    }

    public AdminRisksDataDto(List<AttendanceRiskItem> attendanceRisk,
                             List<AssignmentRiskItem> assignmentRisk,
                             List<ExamRiskItem> examRisk,
                             List<QuizRiskItem> quizRisk,
                             List<CareerGapItem> careerGap) {
        this.attendanceRisk = attendanceRisk;
        this.assignmentRisk = assignmentRisk;
        this.examRisk = examRisk;
        this.quizRisk = quizRisk;
        this.careerGap = careerGap;
    }

    public List<AttendanceRiskItem> getAttendanceRisk() { return attendanceRisk; }
    public void setAttendanceRisk(List<AttendanceRiskItem> attendanceRisk) { this.attendanceRisk = attendanceRisk; }

    public List<AssignmentRiskItem> getAssignmentRisk() { return assignmentRisk; }
    public void setAssignmentRisk(List<AssignmentRiskItem> assignmentRisk) { this.assignmentRisk = assignmentRisk; }

    public List<ExamRiskItem> getExamRisk() { return examRisk; }
    public void setExamRisk(List<ExamRiskItem> examRisk) { this.examRisk = examRisk; }

    public List<QuizRiskItem> getQuizRisk() { return quizRisk; }
    public void setQuizRisk(List<QuizRiskItem> quizRisk) { this.quizRisk = quizRisk; }

    public List<CareerGapItem> getCareerGap() { return careerGap; }
    public void setCareerGap(List<CareerGapItem> careerGap) { this.careerGap = careerGap; }

    public static class AttendanceRiskItem {
        private String studentId;
        private String name;
        private String department;
        private Integer semester;
        private double percentage;
        private int conducted;
        private int attended;
        private String riskClassification;

        public AttendanceRiskItem() {}
        public AttendanceRiskItem(String studentId, String name, String department, Integer semester,
                                  double percentage, int conducted, int attended, String riskClassification) {
            this.studentId = studentId;
            this.name = name;
            this.department = department;
            this.semester = semester;
            this.percentage = percentage;
            this.conducted = conducted;
            this.attended = attended;
            this.riskClassification = riskClassification;
        }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public Integer getSemester() { return semester; }
        public void setSemester(Integer semester) { this.semester = semester; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
        public int getConducted() { return conducted; }
        public void setConducted(int conducted) { this.conducted = conducted; }
        public int getAttended() { return attended; }
        public void setAttended(int attended) { this.attended = attended; }
        public String getRiskClassification() { return riskClassification; }
        public void setRiskClassification(String riskClassification) { this.riskClassification = riskClassification; }
    }

    public static class AssignmentRiskItem {
        private String studentId;
        private String name;
        private String department;
        private int overdueCount;
        private int pendingCount;

        public AssignmentRiskItem() {}
        public AssignmentRiskItem(String studentId, String name, String department, int overdueCount, int pendingCount) {
            this.studentId = studentId;
            this.name = name;
            this.department = department;
            this.overdueCount = overdueCount;
            this.pendingCount = pendingCount;
        }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public int getOverdueCount() { return overdueCount; }
        public void setOverdueCount(int overdueCount) { this.overdueCount = overdueCount; }
        public int getPendingCount() { return pendingCount; }
        public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
    }

    public static class ExamRiskItem {
        private String studentId;
        private String name;
        private String department;
        private int upcomingCount;
        private boolean isExamToday;

        public ExamRiskItem() {}
        public ExamRiskItem(String studentId, String name, String department, int upcomingCount, boolean isExamToday) {
            this.studentId = studentId;
            this.name = name;
            this.department = department;
            this.upcomingCount = upcomingCount;
            this.isExamToday = isExamToday;
        }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public int getUpcomingCount() { return upcomingCount; }
        public void setUpcomingCount(int upcomingCount) { this.upcomingCount = upcomingCount; }
        public boolean isExamToday() { return isExamToday; }
        public void setExamToday(boolean examToday) { isExamToday = examToday; }
    }

    public static class QuizRiskItem {
        private String studentId;
        private String name;
        private String department;
        private double averageScore;
        private int attemptsCount;

        public QuizRiskItem() {}
        public QuizRiskItem(String studentId, String name, String department, double averageScore, int attemptsCount) {
            this.studentId = studentId;
            this.name = name;
            this.department = department;
            this.averageScore = averageScore;
            this.attemptsCount = attemptsCount;
        }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public double getAverageScore() { return averageScore; }
        public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
        public int getAttemptsCount() { return attemptsCount; }
        public void setAttemptsCount(int attemptsCount) { this.attemptsCount = attemptsCount; }
    }

    public static class CareerGapItem {
        private String studentId;
        private String name;
        private String targetRole;
        private Double skillCoverage;

        public CareerGapItem() {}
        public CareerGapItem(String studentId, String name, String targetRole, Double skillCoverage) {
            this.studentId = studentId;
            this.name = name;
            this.targetRole = targetRole;
            this.skillCoverage = skillCoverage;
        }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTargetRole() { return targetRole; }
        public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
        public Double getSkillCoverage() { return skillCoverage; }
        public void setSkillCoverage(Double skillCoverage) { this.skillCoverage = skillCoverage; }
    }
}
