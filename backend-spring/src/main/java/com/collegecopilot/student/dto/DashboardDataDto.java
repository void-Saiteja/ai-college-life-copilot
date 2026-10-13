package com.collegecopilot.student.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardDataDto {

    private StudentProfileDto student;
    private List<SubjectDto> subjects;
    private List<AttendanceDto> attendance;
    private List<AssignmentSummaryDto> upcomingAssignments;
    private List<ExamSummaryDto> upcomingExams;
    private StudyPlanDto studyPlan;
    private String recentQuizScore;
    private QuizHistorySummaryDto quizHistorySummary;
    private Object careerProfileSummary;
    private List<NotificationDto> notifications;

    public DashboardDataDto() {
    }

    public StudentProfileDto getStudent() { return student; }
    public void setStudent(StudentProfileDto student) { this.student = student; }

    public List<SubjectDto> getSubjects() { return subjects; }
    public void setSubjects(List<SubjectDto> subjects) { this.subjects = subjects; }

    public List<AttendanceDto> getAttendance() { return attendance; }
    public void setAttendance(List<AttendanceDto> attendance) { this.attendance = attendance; }

    public List<AssignmentSummaryDto> getUpcomingAssignments() { return upcomingAssignments; }
    public void setUpcomingAssignments(List<AssignmentSummaryDto> upcomingAssignments) { this.upcomingAssignments = upcomingAssignments; }

    public List<ExamSummaryDto> getUpcomingExams() { return upcomingExams; }
    public void setUpcomingExams(List<ExamSummaryDto> upcomingExams) { this.upcomingExams = upcomingExams; }

    public StudyPlanDto getStudyPlan() { return studyPlan; }
    public void setStudyPlan(StudyPlanDto studyPlan) { this.studyPlan = studyPlan; }

    public String getRecentQuizScore() { return recentQuizScore; }
    public void setRecentQuizScore(String recentQuizScore) { this.recentQuizScore = recentQuizScore; }

    public QuizHistorySummaryDto getQuizHistorySummary() { return quizHistorySummary; }
    public void setQuizHistorySummary(QuizHistorySummaryDto quizHistorySummary) { this.quizHistorySummary = quizHistorySummary; }

    public Object getCareerProfileSummary() { return careerProfileSummary; }
    public void setCareerProfileSummary(Object careerProfileSummary) { this.careerProfileSummary = careerProfileSummary; }

    public List<NotificationDto> getNotifications() { return notifications; }
    public void setNotifications(List<NotificationDto> notifications) { this.notifications = notifications; }
}
