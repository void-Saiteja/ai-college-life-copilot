package com.collegecopilot.copilot;

import java.util.ArrayList;
import java.util.List;

public class StudentCopilotContext {

    private StudentProfile profile;
    private List<AttendanceItem> attendance = new ArrayList<>();
    private List<AssignmentItem> upcomingAssignments = new ArrayList<>();
    private List<ExamItem> upcomingExams = new ArrayList<>();
    private ActiveStudyPlan studyPlan;
    private QuizHistorySummary quizHistory;

    public StudentCopilotContext() {
    }

    public StudentProfile getProfile() {
        return profile;
    }

    public void setProfile(StudentProfile profile) {
        this.profile = profile;
    }

    public List<AttendanceItem> getAttendance() {
        return attendance;
    }

    public void setAttendance(List<AttendanceItem> attendance) {
        this.attendance = attendance != null ? attendance : new ArrayList<>();
    }

    public List<AssignmentItem> getUpcomingAssignments() {
        return upcomingAssignments;
    }

    public void setUpcomingAssignments(List<AssignmentItem> upcomingAssignments) {
        this.upcomingAssignments = upcomingAssignments != null ? upcomingAssignments : new ArrayList<>();
    }

    public List<ExamItem> getUpcomingExams() {
        return upcomingExams;
    }

    public void setUpcomingExams(List<ExamItem> upcomingExams) {
        this.upcomingExams = upcomingExams != null ? upcomingExams : new ArrayList<>();
    }

    public ActiveStudyPlan getStudyPlan() {
        return studyPlan;
    }

    public void setStudyPlan(ActiveStudyPlan studyPlan) {
        this.studyPlan = studyPlan;
    }

    public QuizHistorySummary getQuizHistory() {
        return quizHistory;
    }

    public void setQuizHistory(QuizHistorySummary quizHistory) {
        this.quizHistory = quizHistory;
    }

    public static class StudentProfile {
        private String id;
        private String name;
        private String email;
        private int semester;
        private String department;
        private double gpa;

        public StudentProfile() {}

        public StudentProfile(String id, String name, String email, int semester, String department, double gpa) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.semester = semester;
            this.department = department;
            this.gpa = gpa;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public int getSemester() { return semester; }
        public String getDepartment() { return department; }
        public double getGpa() { return gpa; }
    }

    public static class AttendanceItem {
        private String subject;
        private String code;
        private int conducted;
        private int attended;
        private int percentage;
        private String status;

        public AttendanceItem() {}

        public AttendanceItem(String subject, String code, int conducted, int attended, int percentage, String status) {
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

    public static class AssignmentItem {
        private String id;
        private String title;
        private String subject;
        private String dueDate;
        private String priority;
        private long daysRemaining;
        private String deadlineStatus;

        public AssignmentItem() {}

        public AssignmentItem(String id, String title, String subject, String dueDate, String priority, long daysRemaining, String deadlineStatus) {
            this.id = id;
            this.title = title;
            this.subject = subject;
            this.dueDate = dueDate;
            this.priority = priority;
            this.daysRemaining = daysRemaining;
            this.deadlineStatus = deadlineStatus;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getSubject() { return subject; }
        public String getDueDate() { return dueDate; }
        public String getPriority() { return priority; }
        public long getDaysRemaining() { return daysRemaining; }
        public String getDeadlineStatus() { return deadlineStatus; }
    }

    public static class ExamItem {
        private String id;
        private String title;
        private String subject;
        private String subjectCode;
        private String examDate;
        private String startTime;
        private String location;
        private long daysRemaining;
        private String urgency;

        public ExamItem() {}

        public ExamItem(String id, String title, String subject, String subjectCode, String examDate, String startTime, String location, long daysRemaining, String urgency) {
            this.id = id;
            this.title = title;
            this.subject = subject;
            this.subjectCode = subjectCode;
            this.examDate = examDate;
            this.startTime = startTime;
            this.location = location;
            this.daysRemaining = daysRemaining;
            this.urgency = urgency;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getSubject() { return subject; }
        public String getSubjectCode() { return subjectCode; }
        public String getExamDate() { return examDate; }
        public String getStartTime() { return startTime; }
        public String getLocation() { return location; }
        public long getDaysRemaining() { return daysRemaining; }
        public String getUrgency() { return urgency; }
    }

    public static class ActiveStudyPlan {
        private String title;
        private String goal;

        public ActiveStudyPlan() {}

        public ActiveStudyPlan(String title, String goal) {
            this.title = title;
            this.goal = goal;
        }

        public String getTitle() { return title; }
        public String getGoal() { return goal; }
    }

    public static class QuizHistorySummary {
        private int totalAttempts;
        private String recentScore;
        private List<String> weakTopics = new ArrayList<>();

        public QuizHistorySummary() {}

        public QuizHistorySummary(int totalAttempts, String recentScore, List<String> weakTopics) {
            this.totalAttempts = totalAttempts;
            this.recentScore = recentScore;
            this.weakTopics = weakTopics != null ? weakTopics : new ArrayList<>();
        }

        public int getTotalAttempts() { return totalAttempts; }
        public String getRecentScore() { return recentScore; }
        public List<String> getWeakTopics() { return weakTopics; }
    }
}
