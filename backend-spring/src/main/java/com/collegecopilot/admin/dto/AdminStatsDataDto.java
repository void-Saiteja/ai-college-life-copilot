package com.collegecopilot.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminStatsDataDto {

    private int totalStudents;
    private int totalDocuments;
    private int totalSubjects;
    private int totalAssignments;
    private int totalExams;
    private int totalChatSessions;
    private List<AnnouncementDto> recentAnnouncements;
    private List<FaqDto> faqs;

    public AdminStatsDataDto() {
    }

    public AdminStatsDataDto(int totalStudents, int totalDocuments, int totalSubjects,
                             int totalAssignments, int totalExams, int totalChatSessions,
                             List<AnnouncementDto> recentAnnouncements, List<FaqDto> faqs) {
        this.totalStudents = totalStudents;
        this.totalDocuments = totalDocuments;
        this.totalSubjects = totalSubjects;
        this.totalAssignments = totalAssignments;
        this.totalExams = totalExams;
        this.totalChatSessions = totalChatSessions;
        this.recentAnnouncements = recentAnnouncements;
        this.faqs = faqs;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }

    public int getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(int totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public int getTotalSubjects() {
        return totalSubjects;
    }

    public void setTotalSubjects(int totalSubjects) {
        this.totalSubjects = totalSubjects;
    }

    public int getTotalAssignments() {
        return totalAssignments;
    }

    public void setTotalAssignments(int totalAssignments) {
        this.totalAssignments = totalAssignments;
    }

    public int getTotalExams() {
        return totalExams;
    }

    public void setTotalExams(int totalExams) {
        this.totalExams = totalExams;
    }

    public int getTotalChatSessions() {
        return totalChatSessions;
    }

    public void setTotalChatSessions(int totalChatSessions) {
        this.totalChatSessions = totalChatSessions;
    }

    public List<AnnouncementDto> getRecentAnnouncements() {
        return recentAnnouncements;
    }

    public void setRecentAnnouncements(List<AnnouncementDto> recentAnnouncements) {
        this.recentAnnouncements = recentAnnouncements;
    }

    public List<FaqDto> getFaqs() {
        return faqs;
    }

    public void setFaqs(List<FaqDto> faqs) {
        this.faqs = faqs;
    }
}
