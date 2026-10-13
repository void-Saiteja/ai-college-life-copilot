package com.collegecopilot.copilot;

import com.collegecopilot.auth.User;
import com.collegecopilot.auth.UserRepository;
import com.collegecopilot.copilot.dto.*;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.student.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CopilotService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CopilotService.class);

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final ExamRepository examRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final GeminiCopilotService geminiCopilotService;

    public CopilotService(ChatSessionRepository chatSessionRepository,
                          ChatMessageRepository chatMessageRepository,
                          StudentRepository studentRepository,
                          UserRepository userRepository,
                          SubjectRepository subjectRepository,
                          AttendanceRepository attendanceRepository,
                          AssignmentRepository assignmentRepository,
                          ExamRepository examRepository,
                          StudyPlanRepository studyPlanRepository,
                          QuizAttemptRepository quizAttemptRepository,
                          GeminiCopilotService geminiCopilotService) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceRepository = attendanceRepository;
        this.assignmentRepository = assignmentRepository;
        this.examRepository = examRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.geminiCopilotService = geminiCopilotService;
    }

    public Student resolveStudent(UserPrincipal principal) {
        if (principal == null) return null;
        if (principal.getStudentId() != null && !principal.getStudentId().isBlank()) {
            Optional<Student> byStudentId = studentRepository.findById(principal.getStudentId());
            if (byStudentId.isPresent()) return byStudentId.get();
        }
        return studentRepository.findByUserId(principal.getId()).orElse(null);
    }

    @Transactional(readOnly = true)
    public ChatSessionListResponse getChatSessions(UserPrincipal principal) {
        Student student = resolveStudent(principal);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Student profile required");
        }

        List<ChatSession> sessions = chatSessionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        List<ChatSessionDto> dtos = sessions.stream().map(this::mapSessionToDto).collect(Collectors.toList());
        return new ChatSessionListResponse(true, dtos);
    }

    @Transactional(readOnly = true)
    public ChatMessageListResponse getSessionMessages(String sessionId, UserPrincipal principal) {
        Student student = resolveStudent(principal);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Student profile required");
        }

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat session not found"));

        if (!student.getId().equals(session.getStudentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this chat session");
        }

        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<ChatMessageDto> dtos = messages.stream().map(this::mapMessageToDto).collect(Collectors.toList());
        return new ChatMessageListResponse(true, dtos);
    }

    @Transactional
    public ChatResponse postChatMessage(ChatRequest request, UserPrincipal principal) {
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content is required");
        }

        String rawMessage = request.getMessage().trim();
        if (rawMessage.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message exceeds maximum limit of 4000 characters");
        }

        Student student = resolveStudent(principal);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Student profile required");
        }

        String currentSessionId = request.getSessionId();
        if (currentSessionId != null && !currentSessionId.isBlank()) {
            ChatSession existing = chatSessionRepository.findById(currentSessionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat session not found"));

            if (!student.getId().equals(existing.getStudentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this chat session");
            }
        } else {
            currentSessionId = "cs-" + System.currentTimeMillis();
            String title = rawMessage.length() > 30 ? rawMessage.substring(0, 30) + "..." : rawMessage;
            ChatSession newSession = new ChatSession(currentSessionId, student.getId(), title);
            chatSessionRepository.save(newSession);
        }

        // 1. Save user message
        String userMsgId = "cm-" + System.currentTimeMillis() + "-u";
        ChatMessage userMsg = new ChatMessage(userMsgId, currentSessionId, "user", rawMessage);
        chatMessageRepository.save(userMsg);

        // Check for simple greetings without semantic vector search or Gemini call
        if (isSimpleGreeting(rawMessage)) {
            Optional<User> userOpt = userRepository.findById(student.getUserId());
            String fullName = userOpt.map(User::getName).orElse("there");
            String firstName = fullName.split(" ")[0];
            String greetingReply = String.format("""
                    👋 Hello %s! I'm your AI College Life Copilot.

                    I can help you with:
                    • **Academic Status**: Check your attendance percentages and bunk allowances
                    • **Deadlines & Exams**: Review upcoming assignments, project submissions, and midterm schedules
                    • **College Policies**: Answer questions about grading, leaves, attendance, and campus guidelines

                    How can I help you today?""", firstName);

            String assistantMsgId = "cm-" + (System.currentTimeMillis() + 1) + "-a";
            ChatMessage assistantMsg = new ChatMessage(assistantMsgId, currentSessionId, "assistant", greetingReply);
            chatMessageRepository.save(assistantMsg);

            ChatMessageDto userDto = mapMessageToDto(userMsg);
            ChatMessageDto assistantDto = mapMessageToDto(assistantMsg);
            assistantDto.setSources(Collections.emptyList());

            return new ChatResponse(true, new ChatResponse.ChatResponseData(currentSessionId, userDto, assistantDto));
        }

        // 2. Build student academic context
        StudentCopilotContext studentCtx = buildStudentContext(student);

        // 3. Fetch recent conversation history, strictly excluding failed error responses
        List<ChatMessage> historyMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(currentSessionId);
        List<ChatMessageDto> historyDtos = historyMessages.stream()
                .filter(m -> isCleanHistoryMessage(m, userMsgId))
                .map(this::mapMessageToDto)
                .collect(Collectors.toList());

        // 4. Generate AI Copilot reply via Gemini
        GeminiCopilotService.GeminiResponse geminiResponse;
        try {
            geminiResponse = geminiCopilotService.generateCopilotReply(
                    rawMessage,
                    studentCtx,
                    historyDtos
            );
        } catch (Throwable t) {
            log.warn("Gemini Copilot generation threw unexpected Throwable ({}: {}); engaging student fallback", t.getClass().getSimpleName(), t.getMessage());
            String fallback = geminiCopilotService.generateFallbackReply(rawMessage, studentCtx);
            geminiResponse = new GeminiCopilotService.GeminiResponse(
                    true,
                    fallback,
                    false,
                    t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName(),
                    true
            );
        }

        String aiReply = geminiResponse.reply();

        // 5. Save assistant response
        String assistantMsgId = "cm-" + (System.currentTimeMillis() + 1) + "-a";
        ChatMessage assistantMsg = new ChatMessage(assistantMsgId, currentSessionId, "assistant", aiReply);
        chatMessageRepository.save(assistantMsg);

        ChatMessageDto userDto = mapMessageToDto(userMsg);
        ChatMessageDto assistantDto = mapMessageToDto(assistantMsg);
        assistantDto.setSources(Collections.emptyList());

        return new ChatResponse(true, new ChatResponse.ChatResponseData(currentSessionId, userDto, assistantDto));
    }

    @Transactional
    public ChatDeleteResponse deleteChatSession(String sessionId, UserPrincipal principal) {
        Student student = resolveStudent(principal);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Student profile required");
        }

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat session not found"));

        if (!student.getId().equals(session.getStudentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this chat session");
        }

        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.delete(session);

        return new ChatDeleteResponse(true, "Chat conversation deleted");
    }

    public QuickChatResponse chatCopilotQuick(String message, UserPrincipal principal) {
        if (message == null || message.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message query is required");
        }

        if (message.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message exceeds maximum limit of 4000 characters");
        }

        if (geminiCopilotService.detectPromptInjection(message)) {
            return new QuickChatResponse(
                    true,
                    "🛡️ **Institutional Security Notice**: System prompts, operational directives, and other students' private academic records are strictly confidential and cannot be revealed or modified.",
                    Instant.now().toString()
            );
        }

        String lower = message.toLowerCase();
        String reply;

        if (lower.contains("exam") || lower.contains("study") || lower.contains("test")) {
            reply = "📚 **AI Study Tip**: For upcoming exams, practice active recall! Instead of passively reading notes, test yourself using flashcards or explain the concept out loud. Also, apply the **Pomodoro Technique**: 25 min study + 5 min break.";
        } else if (lower.contains("stress") || lower.contains("tired") || lower.contains("burnout")) {
            reply = "🧘 **Copilot Wellness Check**: Take a deep breath! College can get intense. Remember to drink water, get at least 7 hours of sleep, and break large tasks into 20-minute chunks. You've got this!";
        } else if (lower.contains("budget") || lower.contains("money") || lower.contains("save") || lower.contains("cost")) {
            reply = "💰 **Smart College Finance Advice**: Cook in batches on Sundays, look for student discounts (UNiDAYS, Spotify Student, GitHub Student Pack), and stick to your daily spending target.";
        } else if (lower.contains("schedule") || lower.contains("time") || lower.contains("planner")) {
            reply = "📅 **Time Management Strategy**: Block out specific time slots for studying, classes, and relaxation. Protect your high-energy hours (morning or evening) for hard technical assignments.";
        } else {
            reply = "🤖 **AI College Copilot**: I'm here to support your student life! I can help you organize assignments, generate quizzes from notes, analyze your budget, or build a personalized study schedule. How can I assist you right now?";
        }

        return new QuickChatResponse(true, reply, Instant.now().toString());
    }

    public StudentCopilotContext buildStudentContext(Student student) {
        StudentCopilotContext context = new StudentCopilotContext();

        Optional<User> userOpt = userRepository.findById(student.getUserId());
        String studentName = userOpt.map(User::getName).orElse("Alex Mercer");
        String studentEmail = userOpt.map(User::getEmail).orElse("alex@student.edu");

        context.setProfile(new StudentCopilotContext.StudentProfile(
                student.getId(),
                studentName,
                studentEmail,
                student.getSemester(),
                student.getDepartment(),
                student.getGpa() != null ? student.getGpa().doubleValue() : 3.85
        ));

        // Subjects map
        Map<String, Subject> subjectMap = subjectRepository.findAll().stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (s1, s2) -> s1));

        // Attendance
        List<Attendance> attendanceList = attendanceRepository.findByStudentId(student.getId());
        List<StudentCopilotContext.AttendanceItem> attItems = attendanceList.stream().map(a -> {
            Subject sub = subjectMap.get(a.getSubjectId());
            int conducted = a.getClassesConducted();
            int attended = a.getClassesAttended();
            int pct = conducted > 0 ? (int) Math.round((attended * 100.0) / conducted) : 0;
            String status = pct >= 75 ? "Good Standing" : "Low Attendance Warning";
            return new StudentCopilotContext.AttendanceItem(
                    sub != null ? sub.getName() : "Subject",
                    sub != null ? sub.getCode() : "CS",
                    conducted,
                    attended,
                    pct,
                    status
            );
        }).collect(Collectors.toList());
        context.setAttendance(attItems);

        // Assignments
        LocalDate today = LocalDate.now();
        List<Assignment> assignments = assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc(student.getId());
        List<StudentCopilotContext.AssignmentItem> assignmentItems = assignments.stream()
                .filter(a -> !"Completed".equalsIgnoreCase(a.getStatus()))
                .map(a -> {
                    Subject sub = subjectMap.get(a.getSubjectId());
                    long days = a.getDueDate() != null ? ChronoUnit.DAYS.between(today, a.getDueDate()) : 0;
                    String status = days < 0 ? "Overdue" : days <= 2 ? "Due Soon" : "Upcoming";
                    return new StudentCopilotContext.AssignmentItem(
                            a.getId(),
                            a.getTitle(),
                            sub != null ? sub.getName() : "Course",
                            a.getDueDate() != null ? a.getDueDate().toString() : today.toString(),
                            a.getPriority() != null ? a.getPriority() : "Medium",
                            days,
                            status
                    );
                }).collect(Collectors.toList());
        context.setUpcomingAssignments(assignmentItems);

        // Exams
        List<Exam> exams = examRepository.findByStudentIdOrLegacyOrderByExamDateAsc(student.getId());
        List<StudentCopilotContext.ExamItem> examItems = exams.stream()
                .filter(e -> !"Completed".equalsIgnoreCase(e.getStatus()))
                .map(e -> {
                    Subject sub = subjectMap.get(e.getSubjectId());
                    long days = e.getExamDate() != null ? ChronoUnit.DAYS.between(today, e.getExamDate()) : 0;
                    String urgency = days <= 3 ? "Critical" : days <= 7 ? "High" : "Normal";
                    return new StudentCopilotContext.ExamItem(
                            e.getId(),
                            e.getTitle(),
                            sub != null ? sub.getName() : "Course",
                            sub != null ? sub.getCode() : "CS",
                            e.getExamDate() != null ? e.getExamDate().toString() : today.toString(),
                            e.getStartTime() != null ? e.getStartTime().toString() : "10:00",
                            e.getLocation() != null ? e.getLocation() : "Campus Hall",
                            days,
                            urgency
                    );
                }).collect(Collectors.toList());
        context.setUpcomingExams(examItems);

        // Active study plan
        studyPlanRepository.findFirstByStudentIdAndStatus(student.getId(), "Active")
                .ifPresent(p -> context.setStudyPlan(new StudentCopilotContext.ActiveStudyPlan(p.getTitle(), "Midterm Preparation")));

        // Quiz history
        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        if (!attempts.isEmpty()) {
            QuizAttempt recent = attempts.get(0);
            String recentScore = recent.getPercentage() + "% in " + (recent.getTopic() != null ? recent.getTopic() : "General");

            Map<String, List<Double>> topicScores = new HashMap<>();
            for (QuizAttempt a : attempts) {
                String key = (a.getTopic() != null ? a.getTopic() : "General");
                double pct = a.getPercentage() != null ? a.getPercentage().doubleValue() : 0.0;
                topicScores.computeIfAbsent(key, k -> new ArrayList<>()).add(pct);
            }

            List<String> weakTopics = new ArrayList<>();
            topicScores.forEach((t, scores) -> {
                double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                if (avg < 60) weakTopics.add(t);
            });

            context.setQuizHistory(new StudentCopilotContext.QuizHistorySummary(attempts.size(), recentScore, weakTopics));
        }

        return context;
    }

    private ChatSessionDto mapSessionToDto(ChatSession session) {
        String created = session.getCreatedAt() != null ? session.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toString() : Instant.now().toString();
        String updated = session.getUpdatedAt() != null ? session.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toString() : Instant.now().toString();
        return new ChatSessionDto(
                session.getId(),
                session.getStudentId(),
                session.getTitle(),
                created,
                updated
        );
    }

    private ChatMessageDto mapMessageToDto(ChatMessage msg) {
        String created = msg.getCreatedAt() != null ? msg.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toString() : Instant.now().toString();
        return new ChatMessageDto(
                msg.getId(),
                msg.getSessionId(),
                msg.getSender(),
                msg.getContent(),
                Collections.emptyList(),
                created
        );
    }

    private boolean isCleanHistoryMessage(ChatMessage m, String currentUserMsgId) {
        if (m == null || m.getContent() == null) {
            return false;
        }
        if (m.getId() != null && m.getId().equals(currentUserMsgId)) {
            return false;
        }
        String content = m.getContent().trim();
        if (content.isEmpty()) {
            return false;
        }
        if ("assistant".equalsIgnoreCase(m.getSender())) {
            if (content.startsWith("⚠️") || content.startsWith("\u26A0") || content.startsWith("\uD83D\uDEE1") || content.startsWith("🛡️") || content.startsWith("??") || content.startsWith("â")) {
                return false;
            }
            if (content.contains("Gemini AI Service Error")
                    || content.contains("Configuration Notice")
                    || content.contains("Invalid Gemini API Key")
                    || content.contains("Institutional Security Notice")
                    || content.contains("Unable to connect to Google Gemini API")) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSimpleGreeting(String text) {
        if (text == null) return false;
        String cleaned = text.trim().toLowerCase().replaceAll("[!.,?]+$", "").trim();
        List<String> greetings = List.of(
                "hi", "hello", "hey", "hey there", "hi there", "hello there",
                "good morning", "good afternoon", "good evening", "greetings",
                "sup", "yo", "what's up", "whats up", "hi copilot", "hello copilot", "hey copilot"
        );
        return greetings.contains(cleaned);
    }
}
