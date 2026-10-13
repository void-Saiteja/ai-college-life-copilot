package com.collegecopilot.admin.dto;

import com.collegecopilot.career.dto.SkillGapAnalysisDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminStudentDetailDto {

    private ProfileDto profile;
    private AttendanceDetailDto attendance;
    private AssignmentsDetailDto assignments;
    private ExamsDetailDto exams;
    private QuizzesDetailDto quizzes;
    private CareerDetailDto career;
    private List<ResumeSummaryDto> resumes;
    private InterviewDetailDto interview;

    public AdminStudentDetailDto() {
    }

    public AdminStudentDetailDto(ProfileDto profile, AttendanceDetailDto attendance,
                                 AssignmentsDetailDto assignments, ExamsDetailDto exams,
                                 QuizzesDetailDto quizzes, CareerDetailDto career,
                                 List<ResumeSummaryDto> resumes, InterviewDetailDto interview) {
        this.profile = profile;
        this.attendance = attendance;
        this.assignments = assignments;
        this.exams = exams;
        this.quizzes = quizzes;
        this.career = career;
        this.resumes = resumes;
        this.interview = interview;
    }

    public ProfileDto getProfile() { return profile; }
    public void setProfile(ProfileDto profile) { this.profile = profile; }

    public AttendanceDetailDto getAttendance() { return attendance; }
    public void setAttendance(AttendanceDetailDto attendance) { this.attendance = attendance; }

    public AssignmentsDetailDto getAssignments() { return assignments; }
    public void setAssignments(AssignmentsDetailDto assignments) { this.assignments = assignments; }

    public ExamsDetailDto getExams() { return exams; }
    public void setExams(ExamsDetailDto exams) { this.exams = exams; }

    public QuizzesDetailDto getQuizzes() { return quizzes; }
    public void setQuizzes(QuizzesDetailDto quizzes) { this.quizzes = quizzes; }

    public CareerDetailDto getCareer() { return career; }
    public void setCareer(CareerDetailDto career) { this.career = career; }

    public List<ResumeSummaryDto> getResumes() { return resumes; }
    public void setResumes(List<ResumeSummaryDto> resumes) { this.resumes = resumes; }

    public InterviewDetailDto getInterview() { return interview; }
    public void setInterview(InterviewDetailDto interview) { this.interview = interview; }

    // Nested DTOs
    public static class ProfileDto {
        private String id;
        private String userId;
        private String studentCode;
        private String name;
        private String email;
        private Integer semester;
        private String department;
        private BigDecimal gpa;
        private String createdAt;

        public ProfileDto() {}
        public ProfileDto(String id, String userId, String studentCode, String name, String email,
                          Integer semester, String department, BigDecimal gpa, String createdAt) {
            this.id = id;
            this.userId = userId;
            this.studentCode = studentCode;
            this.name = name;
            this.email = email;
            this.semester = semester;
            this.department = department;
            this.gpa = gpa;
            this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getStudentCode() { return studentCode; }
        public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Integer getSemester() { return semester; }
        public void setSemester(Integer semester) { this.semester = semester; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public BigDecimal getGpa() { return gpa; }
        public void setGpa(BigDecimal gpa) { this.gpa = gpa; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public static class AttendanceOverallDto {
        private int conducted;
        private int attended;
        private double percentage;
        private int targetPercentage;
        private String riskClassification;
        private String status;
        private int maxCanMiss;
        private int minRequiredToAttend;

        public AttendanceOverallDto() {}
        public AttendanceOverallDto(int conducted, int attended, double percentage, int targetPercentage,
                                    String riskClassification, String status, int maxCanMiss, int minRequiredToAttend) {
            this.conducted = conducted;
            this.attended = attended;
            this.percentage = percentage;
            this.targetPercentage = targetPercentage;
            this.riskClassification = riskClassification;
            this.status = status;
            this.maxCanMiss = maxCanMiss;
            this.minRequiredToAttend = minRequiredToAttend;
        }

        public int getConducted() { return conducted; }
        public void setConducted(int conducted) { this.conducted = conducted; }
        public int getAttended() { return attended; }
        public void setAttended(int attended) { this.attended = attended; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
        public int getTargetPercentage() { return targetPercentage; }
        public void setTargetPercentage(int targetPercentage) { this.targetPercentage = targetPercentage; }
        public String getRiskClassification() { return riskClassification; }
        public void setRiskClassification(String riskClassification) { this.riskClassification = riskClassification; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getMaxCanMiss() { return maxCanMiss; }
        public void setMaxCanMiss(int maxCanMiss) { this.maxCanMiss = maxCanMiss; }
        public int getMinRequiredToAttend() { return minRequiredToAttend; }
        public void setMinRequiredToAttend(int minRequiredToAttend) { this.minRequiredToAttend = minRequiredToAttend; }
    }

    public static class SubjectAttendanceDto {
        private String subjectId;
        private String subjectCode;
        private String subjectName;
        private int conducted;
        private int attended;
        private double percentage;
        private String riskClassification;
        private String status;
        private int maxCanMiss;
        private int minRequiredToAttend;

        public SubjectAttendanceDto() {}
        public SubjectAttendanceDto(String subjectId, String subjectCode, String subjectName,
                                    int conducted, int attended, double percentage,
                                    String riskClassification, String status,
                                    int maxCanMiss, int minRequiredToAttend) {
            this.subjectId = subjectId;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.conducted = conducted;
            this.attended = attended;
            this.percentage = percentage;
            this.riskClassification = riskClassification;
            this.status = status;
            this.maxCanMiss = maxCanMiss;
            this.minRequiredToAttend = minRequiredToAttend;
        }

        public String getSubjectId() { return subjectId; }
        public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
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
        public int getMaxCanMiss() { return maxCanMiss; }
        public void setMaxCanMiss(int maxCanMiss) { this.maxCanMiss = maxCanMiss; }
        public int getMinRequiredToAttend() { return minRequiredToAttend; }
        public void setMinRequiredToAttend(int minRequiredToAttend) { this.minRequiredToAttend = minRequiredToAttend; }
    }

    public static class AttendanceDetailDto {
        private AttendanceOverallDto overall;
        private List<SubjectAttendanceDto> subjects;

        public AttendanceDetailDto() {}
        public AttendanceDetailDto(AttendanceOverallDto overall, List<SubjectAttendanceDto> subjects) {
            this.overall = overall;
            this.subjects = subjects;
        }
        public AttendanceOverallDto getOverall() { return overall; }
        public void setOverall(AttendanceOverallDto overall) { this.overall = overall; }
        public List<SubjectAttendanceDto> getSubjects() { return subjects; }
        public void setSubjects(List<SubjectAttendanceDto> subjects) { this.subjects = subjects; }
    }

    public static class AssignmentItemDto {
        private String id;
        private String title;
        private String subject;
        private String dueDate;
        private String priority;
        private String status;
        private long daysRemaining;
        private String deadlineStatus;
        private String urgency;

        public AssignmentItemDto() {}
        public AssignmentItemDto(String id, String title, String subject, String dueDate,
                                 String priority, String status, long daysRemaining,
                                 String deadlineStatus, String urgency) {
            this.id = id;
            this.title = title;
            this.subject = subject;
            this.dueDate = dueDate;
            this.priority = priority;
            this.status = status;
            this.daysRemaining = daysRemaining;
            this.deadlineStatus = deadlineStatus;
            this.urgency = urgency;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getDaysRemaining() { return daysRemaining; }
        public void setDaysRemaining(long daysRemaining) { this.daysRemaining = daysRemaining; }
        public String getDeadlineStatus() { return deadlineStatus; }
        public void setDeadlineStatus(String deadlineStatus) { this.deadlineStatus = deadlineStatus; }
        public String getUrgency() { return urgency; }
        public void setUrgency(String urgency) { this.urgency = urgency; }
    }

    public static class AssignmentsDetailDto {
        private int total;
        private int pending;
        private int overdue;
        private List<AssignmentItemDto> list;

        public AssignmentsDetailDto() {}
        public AssignmentsDetailDto(int total, int pending, int overdue, List<AssignmentItemDto> list) {
            this.total = total;
            this.pending = pending;
            this.overdue = overdue;
            this.list = list;
        }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getPending() { return pending; }
        public void setPending(int pending) { this.pending = pending; }
        public int getOverdue() { return overdue; }
        public void setOverdue(int overdue) { this.overdue = overdue; }
        public List<AssignmentItemDto> getList() { return list; }
        public void setList(List<AssignmentItemDto> list) { this.list = list; }
    }

    public static class ExamItemDto {
        private String id;
        private String title;
        private String subject;
        private String examDate;
        private String startTime;
        private String location;
        private String syllabus;
        private long daysRemaining;
        private String examStatus;
        private String urgency;

        public ExamItemDto() {}
        public ExamItemDto(String id, String title, String subject, String examDate,
                           String startTime, String location, String syllabus,
                           long daysRemaining, String examStatus, String urgency) {
            this.id = id;
            this.title = title;
            this.subject = subject;
            this.examDate = examDate;
            this.startTime = startTime;
            this.location = location;
            this.syllabus = syllabus;
            this.daysRemaining = daysRemaining;
            this.examStatus = examStatus;
            this.urgency = urgency;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getExamDate() { return examDate; }
        public void setExamDate(String examDate) { this.examDate = examDate; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getSyllabus() { return syllabus; }
        public void setSyllabus(String syllabus) { this.syllabus = syllabus; }
        public long getDaysRemaining() { return daysRemaining; }
        public void setDaysRemaining(long daysRemaining) { this.daysRemaining = daysRemaining; }
        public String getExamStatus() { return examStatus; }
        public void setExamStatus(String examStatus) { this.examStatus = examStatus; }
        public String getUrgency() { return urgency; }
        public void setUrgency(String urgency) { this.urgency = urgency; }
    }

    public static class ExamsDetailDto {
        private int total;
        private int upcoming;
        private int today;
        private List<ExamItemDto> list;

        public ExamsDetailDto() {}
        public ExamsDetailDto(int total, int upcoming, int today, List<ExamItemDto> list) {
            this.total = total;
            this.upcoming = upcoming;
            this.today = today;
            this.list = list;
        }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getUpcoming() { return upcoming; }
        public void setUpcoming(int upcoming) { this.upcoming = upcoming; }
        public int getToday() { return today; }
        public void setToday(int today) { this.today = today; }
        public List<ExamItemDto> getList() { return list; }
        public void setList(List<ExamItemDto> list) { this.list = list; }
    }

    public static class QuizAttemptItemDto {
        private String id;
        private String subjectId;
        private String topic;
        private Integer score;
        private Integer totalQuestions;
        private BigDecimal percentage;
        private String createdAt;

        public QuizAttemptItemDto() {}
        public QuizAttemptItemDto(String id, String subjectId, String topic, Integer score,
                                  Integer totalQuestions, BigDecimal percentage, String createdAt) {
            this.id = id;
            this.subjectId = subjectId;
            this.topic = topic;
            this.score = score;
            this.totalQuestions = totalQuestions;
            this.percentage = percentage;
            this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSubjectId() { return subjectId; }
        public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public Integer getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }
        public BigDecimal getPercentage() { return percentage; }
        public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public static class QuizzesDetailDto {
        private int totalAttempts;
        private double averageScore;
        private List<QuizAttemptItemDto> attempts;

        public QuizzesDetailDto() {}
        public QuizzesDetailDto(int totalAttempts, double averageScore, List<QuizAttemptItemDto> attempts) {
            this.totalAttempts = totalAttempts;
            this.averageScore = averageScore;
            this.attempts = attempts;
        }

        public int getTotalAttempts() { return totalAttempts; }
        public void setTotalAttempts(int totalAttempts) { this.totalAttempts = totalAttempts; }
        public double getAverageScore() { return averageScore; }
        public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
        public List<QuizAttemptItemDto> getAttempts() { return attempts; }
        public void setAttempts(List<QuizAttemptItemDto> attempts) { this.attempts = attempts; }
    }

    public static class CareerDetailDto {
        private String careerGoal;
        private String targetRole;
        private String domain;
        private List<?> skills;
        private SkillGapAnalysisDto skillGapAnalysis;

        public CareerDetailDto() {}
        public CareerDetailDto(String careerGoal, String targetRole, String domain,
                               List<?> skills, SkillGapAnalysisDto skillGapAnalysis) {
            this.careerGoal = careerGoal;
            this.targetRole = targetRole;
            this.domain = domain;
            this.skills = skills;
            this.skillGapAnalysis = skillGapAnalysis;
        }

        public String getCareerGoal() { return careerGoal; }
        public void setCareerGoal(String careerGoal) { this.careerGoal = careerGoal; }
        public String getTargetRole() { return targetRole; }
        public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public List<?> getSkills() { return skills; }
        public void setSkills(List<?> skills) { this.skills = skills; }
        public SkillGapAnalysisDto getSkillGapAnalysis() { return skillGapAnalysis; }
        public void setSkillGapAnalysis(SkillGapAnalysisDto skillGapAnalysis) { this.skillGapAnalysis = skillGapAnalysis; }
    }

    public static class ResumeSummaryDto {
        private String id;
        private String title;
        @JsonProperty("target_role")
        private String targetRole;
        @JsonProperty("updated_at")
        private String updatedAt;
        private Double coveragePercentage;

        public ResumeSummaryDto() {}
        public ResumeSummaryDto(String id, String title, String targetRole, String updatedAt, Double coveragePercentage) {
            this.id = id;
            this.title = title;
            this.targetRole = targetRole;
            this.updatedAt = updatedAt;
            this.coveragePercentage = coveragePercentage;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getTargetRole() { return targetRole; }
        public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        public Double getCoveragePercentage() { return coveragePercentage; }
        public void setCoveragePercentage(Double coveragePercentage) { this.coveragePercentage = coveragePercentage; }
    }

    public static class InterviewDetailDto {
        private int totalAttempts;
        private double averageScore;
        private List<?> weakAreas;
        private List<?> history;

        public InterviewDetailDto() {}
        public InterviewDetailDto(int totalAttempts, double averageScore, List<?> weakAreas, List<?> history) {
            this.totalAttempts = totalAttempts;
            this.averageScore = averageScore;
            this.weakAreas = weakAreas;
            this.history = history;
        }

        public int getTotalAttempts() { return totalAttempts; }
        public void setTotalAttempts(int totalAttempts) { this.totalAttempts = totalAttempts; }
        public double getAverageScore() { return averageScore; }
        public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
        public List<?> getWeakAreas() { return weakAreas; }
        public void setWeakAreas(List<?> weakAreas) { this.weakAreas = weakAreas; }
        public List<?> getHistory() { return history; }
        public void setHistory(List<?> history) { this.history = history; }
    }
}
