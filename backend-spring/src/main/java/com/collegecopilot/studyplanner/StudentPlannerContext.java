package com.collegecopilot.studyplanner;

import java.util.ArrayList;
import java.util.List;

public class StudentPlannerContext {

    private String studentName;
    private String department;
    private int semester = 4;
    private double gpa = 3.85;

    private List<String> subjectNames = new ArrayList<>();
    private List<AttendanceSummary> attendance = new ArrayList<>();
    private List<AssignmentSummary> assignments = new ArrayList<>();
    private List<ExamSummary> exams = new ArrayList<>();
    private String activePlanTitle;

    public StudentPlannerContext() {
    }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public double getGpa() { return gpa; }
    public void setGpa(double gpa) { this.gpa = gpa; }

    public List<String> getSubjectNames() { return subjectNames; }
    public void setSubjectNames(List<String> subjectNames) { this.subjectNames = subjectNames; }

    public List<AttendanceSummary> getAttendance() { return attendance; }
    public void setAttendance(List<AttendanceSummary> attendance) { this.attendance = attendance; }

    public List<AssignmentSummary> getAssignments() { return assignments; }
    public void setAssignments(List<AssignmentSummary> assignments) { this.assignments = assignments; }

    public List<ExamSummary> getExams() { return exams; }
    public void setExams(List<ExamSummary> exams) { this.exams = exams; }

    public String getActivePlanTitle() { return activePlanTitle; }
    public void setActivePlanTitle(String activePlanTitle) { this.activePlanTitle = activePlanTitle; }

    public static class AttendanceSummary {
        private String subject;
        private String code;
        private int conducted;
        private int attended;
        private int percentage;
        private String status;

        public AttendanceSummary() {}

        public AttendanceSummary(String subject, String code, int conducted, int attended, int percentage, String status) {
            this.subject = subject;
            this.code = code;
            this.conducted = conducted;
            this.attended = attended;
            this.percentage = percentage;
            this.status = status;
        }

        public String getSubject() { return subject; }
        public String getCode() { return code; }
        public int getConducted() { return conducted; }
        public int getAttended() { return attended; }
        public int getPercentage() { return percentage; }
        public String getStatus() { return status; }
    }

    public static class AssignmentSummary {
        private String title;
        private String subject;
        private String dueDate;
        private String priority;

        public AssignmentSummary() {}

        public AssignmentSummary(String title, String subject, String dueDate, String priority) {
            this.title = title;
            this.subject = subject;
            this.dueDate = dueDate;
            this.priority = priority;
        }

        public String getTitle() { return title; }
        public String getSubject() { return subject; }
        public String getDueDate() { return dueDate; }
        public String getPriority() { return priority; }
    }

    public static class ExamSummary {
        private String title;
        private String subject;
        private String examDate;
        private String startTime;
        private String location;

        public ExamSummary() {}

        public ExamSummary(String title, String subject, String examDate, String startTime, String location) {
            this.title = title;
            this.subject = subject;
            this.examDate = examDate;
            this.startTime = startTime;
            this.location = location;
        }

        public String getTitle() { return title; }
        public String getSubject() { return subject; }
        public String getExamDate() { return examDate; }
        public String getStartTime() { return startTime; }
        public String getLocation() { return location; }
    }
}
