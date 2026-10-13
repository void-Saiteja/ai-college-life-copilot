package com.collegecopilot.career;

import com.collegecopilot.career.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InterviewService {

    private final InterviewQuestionRepository questionRepository;
    private final InterviewPracticeRepository practiceRepository;
    private final CareerProfileRepository careerProfileRepository;
    private final CareerService careerService;
    private final GeminiInterviewService geminiInterviewService;

    @Autowired
    public InterviewService(InterviewQuestionRepository questionRepository,
                            InterviewPracticeRepository practiceRepository,
                            CareerProfileRepository careerProfileRepository,
                            CareerService careerService,
                            GeminiInterviewService geminiInterviewService) {
        this.questionRepository = questionRepository;
        this.practiceRepository = practiceRepository;
        this.careerProfileRepository = careerProfileRepository;
        this.careerService = careerService;
        this.geminiInterviewService = geminiInterviewService != null ? geminiInterviewService : new GeminiInterviewService();
    }

    @Transactional
    public InterviewQuestionResponse generateQuestion(InterviewQuestionGenerateRequest request, UserPrincipal principal) {
        String studentId = careerService.resolveStudentId(principal);

        String category = request != null ? request.category() : null;
        if (category != null && !category.isBlank()
                && !GeminiInterviewService.VALID_CATEGORIES.contains(category.trim().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid interview category '" + category + "'. Valid categories: "
                            + String.join(", ", GeminiInterviewService.VALID_CATEGORIES));
        }

        String difficulty = request != null ? request.difficulty() : null;
        if (difficulty != null && !difficulty.isBlank()
                && !GeminiInterviewService.VALID_DIFFICULTIES.contains(difficulty.trim().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid difficulty '" + difficulty + "'. Valid difficulties: "
                            + String.join(", ", GeminiInterviewService.VALID_DIFFICULTIES));
        }

        String targetRole = request != null ? request.targetRole() : null;
        if (targetRole != null && !targetRole.isBlank()
                && !CareerService.ROLE_SKILL_CATALOG.containsKey(targetRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid target role '" + targetRole + "'. Supported roles: "
                            + String.join(", ", CareerService.ROLE_SKILL_CATALOG.keySet()));
        }

        Optional<CareerProfile> profileOpt = careerProfileRepository.findByStudentId(studentId);
        String assignedRole = (targetRole != null && !targetRole.isBlank())
                ? targetRole
                : profileOpt.map(CareerProfile::getTargetRole).orElse("Full Stack Developer");

        SkillGapAnalysisDto skillGapAnalysis = careerService.calculateSkillGaps(
                profileOpt.map(CareerProfile::getSkills).orElse(Collections.emptyList()),
                assignedRole
        );

        String skill = request != null ? request.skill() : null;

        GeminiInterviewService.GeneratedQuestionResult generated = geminiInterviewService.generateInterviewQuestion(
                assignedRole,
                category,
                difficulty,
                skill,
                profileOpt.orElse(null),
                skillGapAnalysis
        );

        if (generated == null || generated.question() == null || generated.question().trim().length() < 5) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Malformed interview question generated");
        }

        String id = "iq-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);

        InterviewQuestion question = new InterviewQuestion(
                id,
                studentId,
                generated.question().trim(),
                generated.category(),
                generated.difficulty(),
                generated.targetSkill(),
                assignedRole,
                generated.preparationTips(),
                generated.isAiGenerated()
        );

        question = questionRepository.save(question);
        return new InterviewQuestionResponse(true, question.toDto());
    }

    @Transactional
    public InterviewPracticeResponse submitAnswer(String questionId, InterviewAnswerSubmitRequest request, UserPrincipal principal) {
        String studentId = careerService.resolveStudentId(principal);

        InterviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview question not found"));

        if (!question.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: Access denied to interview question");
        }

        if (request == null || request.answer() == null || request.answer().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview answer is required");
        }

        String answerTrimmed = request.answer().trim();
        if (answerTrimmed.length() < 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview answer must be at least 5 characters long");
        }

        if (request.answer().length() > 10000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview answer exceeds maximum length of 10,000 characters");
        }

        GeminiInterviewService.EvaluationResult evaluation = geminiInterviewService.evaluateInterviewAnswer(
                question.getQuestion(),
                answerTrimmed,
                question.getCategory(),
                question.getDifficulty(),
                question.getTargetSkill(),
                question.getTargetRole()
        );

        String practiceId = "ip-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);

        InterviewPractice practice = new InterviewPractice(
                practiceId,
                studentId,
                question.getId(),
                question.getQuestion(),
                question.getCategory(),
                question.getDifficulty(),
                question.getTargetSkill(),
                question.getTargetRole(),
                answerTrimmed,
                evaluation.scores(),
                evaluation.feedback()
        );

        practice = practiceRepository.save(practice);
        return new InterviewPracticeResponse(true, practice.toDto());
    }

    @Transactional(readOnly = true)
    public InterviewHistoryResponse getHistory(UserPrincipal principal) {
        String studentId = careerService.resolveStudentId(principal);
        List<InterviewPractice> practices = practiceRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        InterviewHistoryDataDto historyData = geminiInterviewService.analyzeInterviewWeakAreas(practices);
        return new InterviewHistoryResponse(true, historyData);
    }

    @Transactional(readOnly = true)
    public LegacyInterviewPracticeResponse practiceInterviewLegacy(LegacyInterviewPracticeRequest request, UserPrincipal principal) {
        careerService.resolveStudentId(principal);

        if (request == null || request.answer() == null || request.answer().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview answer is required");
        }

        String targetRole = (request.targetRole() != null && !request.targetRole().isBlank())
                ? request.targetRole()
                : "Full Stack Developer";

        String question = (request.question() != null && !request.question().isBlank())
                ? request.question()
                : "Explain your technical approach.";

        return new LegacyInterviewPracticeResponse(
                true,
                new LegacyInterviewPracticeResponse.LegacyInterviewPracticeDataDto(
                        targetRole,
                        question,
                        request.answer(),
                        85,
                        "✅ Great technical response!",
                        "How would your solution scale if load increased 10x?"
                )
        );
    }
}
