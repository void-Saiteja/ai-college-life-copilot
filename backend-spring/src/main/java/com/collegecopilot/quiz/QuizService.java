package com.collegecopilot.quiz;

import com.collegecopilot.quiz.dto.*;
import com.collegecopilot.student.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final QuizAttemptRepository quizAttemptRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ExamRepository examRepository;
    private final GeminiQuizService geminiQuizService;

    // In-memory store for active generated quizzes (matching Node.js db.quizzes store)
    private final Map<String, QuizRecord> activeQuizzes = new ConcurrentHashMap<>();

    public QuizService(
            QuizAttemptRepository quizAttemptRepository,
            StudentRepository studentRepository,
            SubjectRepository subjectRepository,
            ExamRepository examRepository,
            GeminiQuizService geminiQuizService) {
        this.quizAttemptRepository = quizAttemptRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.examRepository = examRepository;
        this.geminiQuizService = geminiQuizService;
    }

    public QuizGenerateResponse generateQuiz(String userIdOrStudentId, QuizGenerateRequest request) {
        Student student = resolveStudent(userIdOrStudentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }

        String subject = (request != null && request.getSubject() != null && !request.getSubject().isBlank())
                ? request.getSubject().trim()
                : "CS 201";
        String topic = (request != null && request.getTopic() != null && !request.getTopic().isBlank())
                ? request.getTopic().trim()
                : "Data Structures & Algorithms";
        String difficulty = (request != null && request.getDifficulty() != null && !request.getDifficulty().isBlank())
                ? request.getDifficulty().trim()
                : "Medium";
        int questionCount = (request != null && request.getQuestionCount() != null)
                ? Math.max(1, Math.min(20, request.getQuestionCount()))
                : 5;

        // Gather upcoming exams
        List<Exam> exams = examRepository.findByStudentIdOrLegacyOrderByExamDateAsc(student.getId());
        List<String> examStrings = exams.stream()
                .filter(e -> !"Completed".equalsIgnoreCase(e.getStatus()))
                .map(e -> String.format("- %s (%s)", e.getTitle(), e.getExamDate() != null ? e.getExamDate().toString() : "Upcoming"))
                .collect(Collectors.toList());

        GeminiQuizService.QuizGenerationResult result = geminiQuizService.generateQuizQuestions(
                subject, topic, difficulty, questionCount, examStrings
        );

        if (result.questions() == null || result.questions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Failed to generate valid quiz questions");
        }

        String quizId = "qz-" + System.currentTimeMillis();
        String createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        QuizRecord record = new QuizRecord(
                quizId,
                student.getId(),
                subject,
                topic,
                difficulty,
                result.questions().size(),
                result.questions(),
                createdAt
        );
        activeQuizzes.put(quizId, record);

        // Sanitize questions by removing correct answers and explanations for public payload
        List<QuizQuestionDto> publicQuestions = result.questions().stream()
                .map(q -> new QuizQuestionDto(q.getId(), q.getQuestion(), q.getOptions()))
                .collect(Collectors.toList());

        QuizGeneratedDataDto data = new QuizGeneratedDataDto(
                quizId,
                subject,
                topic,
                difficulty,
                publicQuestions.size(),
                publicQuestions,
                result.isFallback()
        );

        return new QuizGenerateResponse(true, data);
    }

    @Transactional
    public QuizSubmitResponse submitQuiz(String userIdOrStudentId, String id, QuizSubmitRequest request) {
        Student student = resolveStudent(userIdOrStudentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }

        String resolvedQuizId = (id != null && !id.isBlank() && !"submit".equalsIgnoreCase(id))
                ? id
                : (request != null ? request.getQuizId() : null);

        QuizRecord targetQuiz = (resolvedQuizId != null) ? activeQuizzes.get(resolvedQuizId) : null;
        List<QuizQuestionDto> questionsToScore;

        if (targetQuiz != null) {
            // Ownership check: Student can only submit their own quiz
            if (!student.getId().equals(targetQuiz.getStudentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this quiz");
            }
            // Duplicate submission check
            if ("Completed".equalsIgnoreCase(targetQuiz.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This quiz has already been submitted and completed");
            }
            questionsToScore = targetQuiz.getQuestions();
        } else if (request != null && request.getRawQuestions() != null && !request.getRawQuestions().isEmpty()) {
            for (QuizQuestionDto rq : request.getRawQuestions()) {
                if (!geminiQuizService.validateQuizQuestion(rq)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid question structure in submission");
                }
            }
            questionsToScore = request.getRawQuestions();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz record not found");
        }

        Map<String, Object> answers = (request != null && request.getAnswers() != null)
                ? request.getAnswers()
                : Collections.emptyMap();

        // Evaluate answers deterministically
        int correct = 0;
        int incorrect = 0;
        int unanswered = 0;
        List<QuizQuestionResultDto> results = new ArrayList<>();

        for (QuizQuestionDto q : questionsToScore) {
            Object rawSelected = answers.get(String.valueOf(q.getId()));
            if (rawSelected == null) {
                rawSelected = answers.get(q.getId());
            }

            Integer selectedIdx = parseAnswerIndex(rawSelected);
            if (selectedIdx != null && (selectedIdx < 0 || (q.getOptions() != null && selectedIdx >= q.getOptions().size()))) {
                selectedIdx = null;
            }

            boolean hasAnswered = selectedIdx != null;
            boolean isCorrect = hasAnswered && q.getCorrectAnswer() != null && selectedIdx.equals(q.getCorrectAnswer());

            if (!hasAnswered) {
                unanswered++;
            } else if (isCorrect) {
                correct++;
            } else {
                incorrect++;
            }

            results.add(new QuizQuestionResultDto(
                    q.getId(),
                    q.getQuestion(),
                    q.getOptions(),
                    selectedIdx,
                    q.getCorrectAnswer(),
                    isCorrect,
                    q.getExplanation()
            ));
        }

        int totalQuestions = questionsToScore.size();
        int score = correct;
        double percentage = totalQuestions > 0
                ? Math.round(((double) correct / totalQuestions) * 10000.0) / 100.0
                : 0.0;

        String masteryLevel;
        if (percentage < 60.0) {
            masteryLevel = "WEAK";
        } else if (percentage < 80.0) {
            masteryLevel = "MODERATE";
        } else {
            masteryLevel = "STRONG";
        }

        String quizTopic = targetQuiz != null ? targetQuiz.getTopic() : "Topic Review";
        String quizSubject = targetQuiz != null ? targetQuiz.getSubject() : "Subject";

        List<String> weakTopics = percentage < 60.0
                ? List.of(quizTopic)
                : Collections.emptyList();

        List<String> recommendedTopics = percentage < 80.0
                ? List.of(quizTopic + " Concept Review", "Practice Problems")
                : List.of("Advanced Problem Solving");

        String aiFeedback = percentage >= 80.0
                ? "🌟 Excellent performance! You demonstrate strong mastery of this topic."
                : percentage >= 60.0
                ? "👍 Good effort! Moderate understanding. Review the detailed explanations below."
                : "📚 Weak topic identified (<60%). Focused revision recommended before exams.";

        // Persist attempt to database
        String attemptId = "qa-" + System.currentTimeMillis();
        String subjectId = resolveSubjectId(quizSubject);
        BigDecimal pct = BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP);

        QuizAttempt attempt = new QuizAttempt(
                attemptId,
                student.getId(),
                subjectId,
                quizTopic,
                score,
                totalQuestions,
                pct
        );
        quizAttemptRepository.save(attempt);

        if (targetQuiz != null) {
            targetQuiz.setStatus("Completed");
            targetQuiz.setCompletedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            targetQuiz.setScore(score);
            targetQuiz.setPercentage(percentage);
        }

        QuizSubmissionResultDto data = new QuizSubmissionResultDto(
                attemptId,
                targetQuiz != null ? targetQuiz.getId() : null,
                quizSubject,
                quizTopic,
                score,
                totalQuestions,
                correct,
                incorrect,
                unanswered,
                percentage,
                masteryLevel,
                results,
                weakTopics,
                recommendedTopics,
                aiFeedback
        );

        return new QuizSubmitResponse(true, data);
    }

    @Transactional(readOnly = true)
    public QuizHistoryResponse getQuizzesAndHistory(String userIdOrStudentId) {
        Student student = resolveStudent(userIdOrStudentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }

        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        List<QuizAttemptDto> attemptDtos = attempts.stream().map(a -> new QuizAttemptDto(
                a.getId(),
                null,
                a.getStudentId(),
                resolveSubjectName(a.getSubjectId()),
                a.getTopic(),
                a.getScore(),
                a.getTotalQuestions(),
                a.getPercentage(),
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : null
        )).collect(Collectors.toList());

        List<QuizSummaryDto> quizSummaries = activeQuizzes.values().stream()
                .filter(q -> student.getId().equals(q.getStudentId()))
                .map(q -> new QuizSummaryDto(
                        q.getId(),
                        q.getSubject(),
                        q.getTopic(),
                        q.getDifficulty(),
                        q.getQuestionCount(),
                        q.getStatus(),
                        q.getScore(),
                        q.getPercentage(),
                        q.getCreatedAt()
                ))
                .sorted(Comparator.comparing(QuizSummaryDto::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        return new QuizHistoryResponse(true, new QuizHistoryDataDto(attemptDtos, quizSummaries));
    }

    public QuizDetailResponse getQuizById(String userIdOrStudentId, String id) {
        Student student = resolveStudent(userIdOrStudentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }

        QuizRecord quiz = activeQuizzes.get(id);
        if (quiz == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
        }

        // Authorization check: Student can only access their own quiz
        if (!student.getId().equals(quiz.getStudentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this quiz");
        }

        boolean isCompleted = "Completed".equalsIgnoreCase(quiz.getStatus());
        List<QuizQuestionDto> questionsPayload = isCompleted
                ? quiz.getQuestions()
                : quiz.getQuestions().stream()
                .map(q -> new QuizQuestionDto(q.getId(), q.getQuestion(), q.getOptions()))
                .collect(Collectors.toList());

        QuizRecordDto recordDto = new QuizRecordDto(
                quiz.getId(),
                quiz.getStudentId(),
                quiz.getSubject(),
                quiz.getTopic(),
                quiz.getDifficulty(),
                quiz.getQuestionCount(),
                questionsPayload,
                quiz.getStatus(),
                quiz.getScore(),
                quiz.getPercentage(),
                quiz.getCreatedAt(),
                quiz.getCompletedAt()
        );

        return new QuizDetailResponse(true, recordDto);
    }

    @Transactional
    public QuizDeleteResponse deleteQuiz(String userIdOrStudentId, String id) {
        Student student = resolveStudent(userIdOrStudentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }

        QuizRecord quiz = activeQuizzes.get(id);
        Optional<QuizAttempt> attemptOpt = quizAttemptRepository.findById(id);

        if (quiz == null && attemptOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz or attempt not found");
        }

        if (quiz != null && !student.getId().equals(quiz.getStudentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this quiz");
        }

        if (attemptOpt.isPresent() && !student.getId().equals(attemptOpt.get().getStudentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this quiz attempt");
        }

        if (quiz != null) {
            activeQuizzes.remove(id);
        }

        attemptOpt.ifPresent(quizAttemptRepository::delete);

        return new QuizDeleteResponse(true, "Quiz deleted successfully");
    }

    @Transactional(readOnly = true)
    public QuizAdvisorResponse runQuizAdvisor(String userIdOrStudentId) {
        Student student = resolveStudent(userIdOrStudentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }

        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());

        List<Exam> exams = examRepository.findByStudentIdOrLegacyOrderByExamDateAsc(student.getId());
        List<String> examStrings = exams.stream()
                .filter(e -> !"Completed".equalsIgnoreCase(e.getStatus()))
                .map(e -> String.format("- %s (%s)", e.getTitle(), e.getExamDate() != null ? e.getExamDate().toString() : "Upcoming"))
                .collect(Collectors.toList());

        List<GeminiQuizService.AdvisorAttempt> advisorAttempts = attempts.stream().map(a -> new GeminiQuizService.AdvisorAttempt(
                resolveSubjectName(a.getSubjectId()),
                a.getTopic(),
                a.getPercentage() != null ? a.getPercentage().doubleValue() : 0.0,
                a.getScore() != null ? a.getScore() : 0,
                a.getTotalQuestions() != null ? a.getTotalQuestions() : 5
        )).collect(Collectors.toList());

        QuizAdvisorDataDto advice = geminiQuizService.generateAdvisorAdvice(advisorAttempts, examStrings);
        return new QuizAdvisorResponse(true, advice);
    }

    private Student resolveStudent(String userIdOrStudentId) {
        if (userIdOrStudentId == null || userIdOrStudentId.isBlank()) {
            return null;
        }
        Optional<Student> direct = studentRepository.findById(userIdOrStudentId);
        if (direct.isPresent()) {
            return direct.get();
        }
        return studentRepository.findByUserId(userIdOrStudentId).orElse(null);
    }

    private String resolveSubjectId(String subjectNameOrCode) {
        if (subjectNameOrCode == null || subjectNameOrCode.isBlank()) {
            return "sub-1";
        }
        String q = subjectNameOrCode.toLowerCase().trim();
        List<Subject> subjects = subjectRepository.findAll();
        for (Subject s : subjects) {
            if (s.getName() != null && (s.getName().toLowerCase().contains(q) || q.contains(s.getName().toLowerCase()))) {
                return s.getId();
            }
            if (s.getCode() != null && (s.getCode().equalsIgnoreCase(q) || q.contains(s.getCode().toLowerCase()))) {
                return s.getId();
            }
        }
        return !subjects.isEmpty() ? subjects.get(0).getId() : "sub-1";
    }

    private String resolveSubjectName(String subjectId) {
        if (subjectId == null) return "Subject";
        return subjectRepository.findById(subjectId)
                .map(Subject::getName)
                .orElse("Subject");
    }

    private Integer parseAnswerIndex(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number num) {
            return num.intValue();
        }
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Helper for testing to inject/inspect active quizzes
    public void putActiveQuizForTest(String id, QuizRecord record) {
        activeQuizzes.put(id, record);
    }

    public QuizRecord getActiveQuizForTest(String id) {
        return activeQuizzes.get(id);
    }
}
