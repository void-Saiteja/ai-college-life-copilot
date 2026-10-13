package com.collegecopilot.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminDashboardDataDto {

    private StudentsStats students;
    private AttendanceStats attendance;
    private AssignmentsStats assignments;
    private ExamsStats exams;
    private QuizzesStats quizzes;
    private CareerStats career;
    private DocumentsStats documents;

    public AdminDashboardDataDto() {
    }

    public AdminDashboardDataDto(StudentsStats students, AttendanceStats attendance,
                                 AssignmentsStats assignments, ExamsStats exams,
                                 QuizzesStats quizzes, CareerStats career,
                                 DocumentsStats documents) {
        this.students = students;
        this.attendance = attendance;
        this.assignments = assignments;
        this.exams = exams;
        this.quizzes = quizzes;
        this.career = career;
        this.documents = documents;
    }

    public StudentsStats getStudents() {
        return students;
    }

    public void setStudents(StudentsStats students) {
        this.students = students;
    }

    public AttendanceStats getAttendance() {
        return attendance;
    }

    public void setAttendance(AttendanceStats attendance) {
        this.attendance = attendance;
    }

    public AssignmentsStats getAssignments() {
        return assignments;
    }

    public void setAssignments(AssignmentsStats assignments) {
        this.assignments = assignments;
    }

    public ExamsStats getExams() {
        return exams;
    }

    public void setExams(ExamsStats exams) {
        this.exams = exams;
    }

    public QuizzesStats getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(QuizzesStats quizzes) {
        this.quizzes = quizzes;
    }

    public CareerStats getCareer() {
        return career;
    }

    public void setCareer(CareerStats career) {
        this.career = career;
    }

    public DocumentsStats getDocuments() {
        return documents;
    }

    public void setDocuments(DocumentsStats documents) {
        this.documents = documents;
    }

    public static class StudentsStats {
        private int totalStudents;
        private int activeStudents;

        public StudentsStats() {}
        public StudentsStats(int totalStudents, int activeStudents) {
            this.totalStudents = totalStudents;
            this.activeStudents = activeStudents;
        }
        public int getTotalStudents() { return totalStudents; }
        public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }
        public int getActiveStudents() { return activeStudents; }
        public void setActiveStudents(int activeStudents) { this.activeStudents = activeStudents; }
    }

    public static class AttendanceStats {
        private int totalStudentsWithAttendance;
        private double averageAttendance;
        private int studentsBelowAttendanceTarget;
        private int studentsAtRisk;

        public AttendanceStats() {}
        public AttendanceStats(int totalStudentsWithAttendance, double averageAttendance,
                               int studentsBelowAttendanceTarget, int studentsAtRisk) {
            this.totalStudentsWithAttendance = totalStudentsWithAttendance;
            this.averageAttendance = averageAttendance;
            this.studentsBelowAttendanceTarget = studentsBelowAttendanceTarget;
            this.studentsAtRisk = studentsAtRisk;
        }
        public int getTotalStudentsWithAttendance() { return totalStudentsWithAttendance; }
        public void setTotalStudentsWithAttendance(int totalStudentsWithAttendance) { this.totalStudentsWithAttendance = totalStudentsWithAttendance; }
        public double getAverageAttendance() { return averageAttendance; }
        public void setAverageAttendance(double averageAttendance) { this.averageAttendance = averageAttendance; }
        public int getStudentsBelowAttendanceTarget() { return studentsBelowAttendanceTarget; }
        public void setStudentsBelowAttendanceTarget(int studentsBelowAttendanceTarget) { this.studentsBelowAttendanceTarget = studentsBelowAttendanceTarget; }
        public int getStudentsAtRisk() { return studentsAtRisk; }
        public void setStudentsAtRisk(int studentsAtRisk) { this.studentsAtRisk = studentsAtRisk; }
    }

    public static class AssignmentsStats {
        private int totalAssignments;
        private int pendingAssignments;
        private int completedAssignments;
        private int overdueAssignments;
        private int dueSoonAssignments;

        public AssignmentsStats() {}
        public AssignmentsStats(int totalAssignments, int pendingAssignments, int completedAssignments,
                                int overdueAssignments, int dueSoonAssignments) {
            this.totalAssignments = totalAssignments;
            this.pendingAssignments = pendingAssignments;
            this.completedAssignments = completedAssignments;
            this.overdueAssignments = overdueAssignments;
            this.dueSoonAssignments = dueSoonAssignments;
        }
        public int getTotalAssignments() { return totalAssignments; }
        public void setTotalAssignments(int totalAssignments) { this.totalAssignments = totalAssignments; }
        public int getPendingAssignments() { return pendingAssignments; }
        public void setPendingAssignments(int pendingAssignments) { this.pendingAssignments = pendingAssignments; }
        public int getCompletedAssignments() { return completedAssignments; }
        public void setCompletedAssignments(int completedAssignments) { this.completedAssignments = completedAssignments; }
        public int getOverdueAssignments() { return overdueAssignments; }
        public void setOverdueAssignments(int overdueAssignments) { this.overdueAssignments = overdueAssignments; }
        public int getDueSoonAssignments() { return dueSoonAssignments; }
        public void setDueSoonAssignments(int dueSoonAssignments) { this.dueSoonAssignments = dueSoonAssignments; }
    }

    public static class ExamsStats {
        private int totalExams;
        private int upcomingExams;
        private int examsToday;
        private int completedExams;

        public ExamsStats() {}
        public ExamsStats(int totalExams, int upcomingExams, int examsToday, int completedExams) {
            this.totalExams = totalExams;
            this.upcomingExams = upcomingExams;
            this.examsToday = examsToday;
            this.completedExams = completedExams;
        }
        public int getTotalExams() { return totalExams; }
        public void setTotalExams(int totalExams) { this.totalExams = totalExams; }
        public int getUpcomingExams() { return upcomingExams; }
        public void setUpcomingExams(int upcomingExams) { this.upcomingExams = upcomingExams; }
        public int getExamsToday() { return examsToday; }
        public void setExamsToday(int examsToday) { this.examsToday = examsToday; }
        public int getCompletedExams() { return completedExams; }
        public void setCompletedExams(int completedExams) { this.completedExams = completedExams; }
    }

    public static class QuizzesStats {
        private int totalQuizAttempts;
        private double averageQuizScore;
        private int weakQuizAttempts;
        private int weakStudents;

        public QuizzesStats() {}
        public QuizzesStats(int totalQuizAttempts, double averageQuizScore, int weakQuizAttempts, int weakStudents) {
            this.totalQuizAttempts = totalQuizAttempts;
            this.averageQuizScore = averageQuizScore;
            this.weakQuizAttempts = weakQuizAttempts;
            this.weakStudents = weakStudents;
        }
        public int getTotalQuizAttempts() { return totalQuizAttempts; }
        public void setTotalQuizAttempts(int totalQuizAttempts) { this.totalQuizAttempts = totalQuizAttempts; }
        public double getAverageQuizScore() { return averageQuizScore; }
        public void setAverageQuizScore(double averageQuizScore) { this.averageQuizScore = averageQuizScore; }
        public int getWeakQuizAttempts() { return weakQuizAttempts; }
        public void setWeakQuizAttempts(int weakQuizAttempts) { this.weakQuizAttempts = weakQuizAttempts; }
        public int getWeakStudents() { return weakStudents; }
        public void setWeakStudents(int weakStudents) { this.weakStudents = weakStudents; }
    }

    public static class SkillGapCount {
        private String skill;
        private int count;

        public SkillGapCount() {}
        public SkillGapCount(String skill, int count) {
            this.skill = skill;
            this.count = count;
        }
        public String getSkill() { return skill; }
        public void setSkill(String skill) { this.skill = skill; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }

    public static class CareerStats {
        private int studentsWithCareerProfiles;
        private int studentsWithSkillGaps;
        private List<SkillGapCount> commonSkillGaps;

        public CareerStats() {}
        public CareerStats(int studentsWithCareerProfiles, int studentsWithSkillGaps, List<SkillGapCount> commonSkillGaps) {
            this.studentsWithCareerProfiles = studentsWithCareerProfiles;
            this.studentsWithSkillGaps = studentsWithSkillGaps;
            this.commonSkillGaps = commonSkillGaps;
        }
        public int getStudentsWithCareerProfiles() { return studentsWithCareerProfiles; }
        public void setStudentsWithCareerProfiles(int studentsWithCareerProfiles) { this.studentsWithCareerProfiles = studentsWithCareerProfiles; }
        public int getStudentsWithSkillGaps() { return studentsWithSkillGaps; }
        public void setStudentsWithSkillGaps(int studentsWithSkillGaps) { this.studentsWithSkillGaps = studentsWithSkillGaps; }
        public List<SkillGapCount> getCommonSkillGaps() { return commonSkillGaps; }
        public void setCommonSkillGaps(List<SkillGapCount> commonSkillGaps) { this.commonSkillGaps = commonSkillGaps; }
    }

    public static class DocumentsStats {
        private int totalDocuments;
        private int totalIndexedChunks;
        private String embeddingProvider;
        private String indexingStatus;

        public DocumentsStats() {}
        public DocumentsStats(int totalDocuments, int totalIndexedChunks, String embeddingProvider, String indexingStatus) {
            this.totalDocuments = totalDocuments;
            this.totalIndexedChunks = totalIndexedChunks;
            this.embeddingProvider = embeddingProvider;
            this.indexingStatus = indexingStatus;
        }
        public int getTotalDocuments() { return totalDocuments; }
        public void setTotalDocuments(int totalDocuments) { this.totalDocuments = totalDocuments; }
        public int getTotalIndexedChunks() { return totalIndexedChunks; }
        public void setTotalIndexedChunks(int totalIndexedChunks) { this.totalIndexedChunks = totalIndexedChunks; }
        public String getEmbeddingProvider() { return embeddingProvider; }
        public void setEmbeddingProvider(String embeddingProvider) { this.embeddingProvider = embeddingProvider; }
        public String getIndexingStatus() { return indexingStatus; }
        public void setIndexingStatus(String indexingStatus) { this.indexingStatus = indexingStatus; }
    }
}
