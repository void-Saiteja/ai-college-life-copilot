package com.collegecopilot.career;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.career.dto.*;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.student.QuizAttempt;
import com.collegecopilot.student.QuizAttemptRepository;
import com.collegecopilot.student.Student;
import com.collegecopilot.student.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CareerService {

    public static final Map<String, List<String>> ROLE_SKILL_CATALOG = new LinkedHashMap<>();

    static {
        ROLE_SKILL_CATALOG.put("Full Stack Developer", List.of("JavaScript", "React", "Node.js", "REST APIs", "SQL", "Git", "Docker"));
        ROLE_SKILL_CATALOG.put("Frontend Developer", List.of("JavaScript", "React", "HTML", "CSS", "TypeScript", "Tailwind", "Git"));
        ROLE_SKILL_CATALOG.put("Backend Developer", List.of("Node.js", "Express", "SQL", "PostgreSQL", "REST APIs", "Docker", "Git"));
        ROLE_SKILL_CATALOG.put("Data Analyst", List.of("Python", "SQL", "Statistics", "Pandas", "Data Visualization", "Excel"));
        ROLE_SKILL_CATALOG.put("Data Scientist", List.of("Python", "SQL", "Statistics", "Pandas", "Machine Learning", "Data Visualization"));
        ROLE_SKILL_CATALOG.put("Machine Learning Engineer", List.of("Python", "Machine Learning", "Deep Learning", "PyTorch", "SQL", "Docker"));
        ROLE_SKILL_CATALOG.put("AI Engineer", List.of("Python", "Machine Learning", "Deep Learning", "NLP", "APIs", "Docker"));
    }

    public static final List<String> VALID_PROFICIENCIES = List.of("BEGINNER", "INTERMEDIATE", "ADVANCED");

    private final CareerProfileRepository careerProfileRepository;
    private final StudentRepository studentRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final GeminiCareerAdvisorService advisorService;

    public CareerService(CareerProfileRepository careerProfileRepository, StudentRepository studentRepository) {
        this(careerProfileRepository, studentRepository, null, new GeminiCareerAdvisorService());
    }

    @Autowired
    public CareerService(CareerProfileRepository careerProfileRepository,
                         StudentRepository studentRepository,
                         @Autowired(required = false) QuizAttemptRepository quizAttemptRepository,
                         GeminiCareerAdvisorService advisorService) {
        this.careerProfileRepository = careerProfileRepository;
        this.studentRepository = studentRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.advisorService = advisorService != null ? advisorService : new GeminiCareerAdvisorService();
    }

    public static boolean validateProficiency(String prof) {
        if (prof == null || prof.isBlank()) {
            return false;
        }
        return VALID_PROFICIENCIES.contains(prof.trim().toUpperCase());
    }

    public String resolveStudentId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        String studentKey = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        if (studentKey == null || studentKey.isBlank()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }

        Optional<Student> direct = studentRepository.findById(studentKey);
        if (direct.isPresent()) {
            return direct.get().getId();
        }

        Optional<Student> byUser = studentRepository.findByUserId(studentKey);
        return byUser.map(Student::getId).orElse(studentKey);
    }

    public SkillGapAnalysisDto calculateSkillGaps(List<SkillItem> studentSkills, String targetRole) {
        String role = (targetRole != null && ROLE_SKILL_CATALOG.containsKey(targetRole))
                ? targetRole
                : "Full Stack Developer";

        List<String> requiredSkills = ROLE_SKILL_CATALOG.getOrDefault(role, ROLE_SKILL_CATALOG.get("Full Stack Developer"));

        Map<String, String> studentSkillMap = new HashMap<>();
        if (studentSkills != null) {
            for (SkillItem s : studentSkills) {
                if (s != null && s.getSkill() != null) {
                    studentSkillMap.put(s.getSkill().toLowerCase().trim(),
                            (s.getProficiency() != null ? s.getProficiency() : "BEGINNER").toUpperCase().trim());
                }
            }
        }

        List<SkillGapItemDto> skillAnalysis = new ArrayList<>();
        int strongCount = 0;
        int partialCount = 0;
        int gapCount = 0;

        List<String> strongList = new ArrayList<>();
        List<String> partialList = new ArrayList<>();
        List<String> missingList = new ArrayList<>();

        for (String reqSkill : requiredSkills) {
            String sLower = reqSkill.toLowerCase().trim();
            String studentProf = studentSkillMap.get(sLower);

            String classification;

            if (studentProf == null) {
                classification = "GAP";
                gapCount++;
                missingList.add(reqSkill);
            } else if ("ADVANCED".equalsIgnoreCase(studentProf) || "INTERMEDIATE".equalsIgnoreCase(studentProf)) {
                classification = "STRONG";
                strongCount++;
                strongList.add(reqSkill);
            } else if ("BEGINNER".equalsIgnoreCase(studentProf)) {
                classification = "PARTIAL";
                partialCount++;
                partialList.add(reqSkill);
            } else {
                classification = "GAP";
                gapCount++;
                missingList.add(reqSkill);
            }

            skillAnalysis.add(new SkillGapItemDto(
                    reqSkill,
                    studentProf != null ? studentProf : "NONE",
                    classification
            ));
        }

        int totalRequired = requiredSkills.size();
        double coveragePercentage = totalRequired > 0
                ? Math.round((((double) (strongCount + partialCount) / totalRequired) * 100.0) * 100.0) / 100.0
                : 0.0;

        return new SkillGapAnalysisDto(
                role,
                totalRequired,
                strongCount,
                partialCount,
                gapCount,
                coveragePercentage,
                strongList,
                partialList,
                missingList,
                skillAnalysis
        );
    }

    @Transactional(readOnly = true)
    public CareerProfileResponse getCareerProfile(UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        Optional<CareerProfile> opt = careerProfileRepository.findByStudentId(studentId);
        if (opt.isEmpty()) {
            return new CareerProfileResponse(true, null, "No career profile found for student. Please create a profile.");
        }

        CareerProfile profile = opt.get();
        SkillGapAnalysisDto gapAnalysis = calculateSkillGaps(profile.getSkills(), profile.getTargetRole());

        return new CareerProfileResponse(true, new CareerProfileDataDto(toDto(profile), gapAnalysis));
    }

    @Transactional
    public CareerProfileResponse saveCareerProfile(CareerProfileSaveRequest request, UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        if (request != null && request.getTargetRole() != null && !request.getTargetRole().isBlank()) {
            String role = request.getTargetRole().trim();
            if (!ROLE_SKILL_CATALOG.containsKey(role)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid target role '" + role + "'. Supported roles: " + String.join(", ", ROLE_SKILL_CATALOG.keySet()));
            }
        }

        List<SkillItem> validatedSkills = null;
        if (request != null && request.getSkills() != null) {
            validatedSkills = new ArrayList<>();
            for (SkillItemDto s : request.getSkills()) {
                if (s == null || s.getSkill() == null || s.getSkill().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skill name is required for all skills");
                }
                if (s.getProficiency() != null && !validateProficiency(s.getProficiency())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid proficiency level '" + s.getProficiency() + "'. Valid levels: " + String.join(", ", VALID_PROFICIENCIES));
                }
                validatedSkills.add(new SkillItem(s.getSkill().trim(),
                        (s.getProficiency() != null && !s.getProficiency().isBlank()) ? s.getProficiency().toUpperCase().trim() : "BEGINNER"));
            }
        }

        CareerProfile profile = careerProfileRepository.findByStudentId(studentId)
                .orElse(null);

        if (profile == null) {
            String id = "cp-" + System.currentTimeMillis();
            String goal = (request != null && request.getCareerGoal() != null) ? request.getCareerGoal() : "Become a Full Stack Software Engineer";
            String role = (request != null && request.getTargetRole() != null && !request.getTargetRole().isBlank()) ? request.getTargetRole().trim() : "Full Stack Developer";
            String domain = (request != null && request.getDomain() != null) ? request.getDomain() : "Software Development";
            List<SkillItem> sk = validatedSkills != null ? validatedSkills : new ArrayList<>();
            List<String> interests = (request != null && request.getInterests() != null) ? request.getInterests() : new ArrayList<>();
            List<Object> projects = (request != null && request.getExperienceProjects() != null) ? request.getExperienceProjects() : new ArrayList<>();

            profile = new CareerProfile(id, studentId, goal, role, domain, sk, interests, projects);
        } else {
            if (request != null) {
                if (request.getCareerGoal() != null) profile.setCareerGoal(request.getCareerGoal());
                if (request.getTargetRole() != null && !request.getTargetRole().isBlank()) profile.setTargetRole(request.getTargetRole().trim());
                if (request.getDomain() != null) profile.setDomain(request.getDomain());
                if (validatedSkills != null) profile.setSkills(validatedSkills);
                if (request.getInterests() != null) profile.setInterests(request.getInterests());
                if (request.getExperienceProjects() != null) profile.setExperienceProjects(request.getExperienceProjects());
            }
        }

        CareerProfile saved = careerProfileRepository.save(profile);
        SkillGapAnalysisDto gapAnalysis = calculateSkillGaps(saved.getSkills(), saved.getTargetRole());

        return new CareerProfileResponse(true, new CareerProfileDataDto(toDto(saved), gapAnalysis));
    }

    @Transactional
    public CareerProfileResponse addSkill(SkillAddRequest request, UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        if (request == null || request.getSkill() == null || request.getSkill().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skill name is required");
        }

        String proficiency = (request.getProficiency() != null && !request.getProficiency().isBlank())
                ? request.getProficiency().trim().toUpperCase()
                : "BEGINNER";

        if (!validateProficiency(proficiency)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid proficiency level '" + proficiency + "'. Valid levels: " + String.join(", ", VALID_PROFICIENCIES));
        }

        CareerProfile profile = careerProfileRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    String id = "cp-" + System.currentTimeMillis();
                    return new CareerProfile(id, studentId, "Become a Software Engineer", "Full Stack Developer", "Software Development",
                            new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                });

        String skillTrimmed = request.getSkill().trim();
        boolean exists = profile.getSkills().stream()
                .anyMatch(s -> s.getSkill() != null && s.getSkill().equalsIgnoreCase(skillTrimmed));

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Skill '" + skillTrimmed + "' already exists in your career profile");
        }

        profile.getSkills().add(new SkillItem(skillTrimmed, proficiency));
        CareerProfile saved = careerProfileRepository.save(profile);

        SkillGapAnalysisDto gapAnalysis = calculateSkillGaps(saved.getSkills(), saved.getTargetRole());
        return new CareerProfileResponse(true, new CareerProfileDataDto(toDto(saved), gapAnalysis));
    }

    @Transactional
    public CareerProfileResponse updateSkill(String skillName, SkillUpdateRequest request, UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        if (request == null || !validateProficiency(request.getProficiency())) {
            String p = request != null ? request.getProficiency() : "null";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid proficiency level '" + p + "'. Valid levels: " + String.join(", ", VALID_PROFICIENCIES));
        }

        CareerProfile profile = careerProfileRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Career profile not found"));

        if (skillName == null || skillName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill name cannot be empty");
        }

        String targetName = skillName.trim();
        SkillItem targetSkill = profile.getSkills().stream()
                .filter(s -> s.getSkill() != null && s.getSkill().equalsIgnoreCase(targetName))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill '" + targetName + "' not found in profile"));

        targetSkill.setProficiency(request.getProficiency().trim().toUpperCase());
        CareerProfile saved = careerProfileRepository.save(profile);

        SkillGapAnalysisDto gapAnalysis = calculateSkillGaps(saved.getSkills(), saved.getTargetRole());
        return new CareerProfileResponse(true, new CareerProfileDataDto(toDto(saved), gapAnalysis));
    }

    @Transactional
    public CareerProfileResponse deleteSkill(String skillName, UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        CareerProfile profile = careerProfileRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Career profile not found"));

        if (skillName == null || skillName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill name cannot be empty");
        }

        String targetName = skillName.trim();
        int initialCount = profile.getSkills().size();
        profile.getSkills().removeIf(s -> s.getSkill() != null && s.getSkill().equalsIgnoreCase(targetName));

        if (profile.getSkills().size() == initialCount) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill '" + targetName + "' not found in profile");
        }

        CareerProfile saved = careerProfileRepository.save(profile);
        SkillGapAnalysisDto gapAnalysis = calculateSkillGaps(saved.getSkills(), saved.getTargetRole());

        return new CareerProfileResponse(true, new CareerProfileDataDto(toDto(saved), gapAnalysis));
    }

    @Transactional(readOnly = true)
    public CareerAdvisorResponse runCareerAdvisor(UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        CareerProfile profile = careerProfileRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Career profile required. Please setup your target role and skills first."));

        String targetRole = profile.getTargetRole() != null ? profile.getTargetRole() : "Full Stack Developer";
        SkillGapAnalysisDto skillGapAnalysis = calculateSkillGaps(profile.getSkills(), targetRole);

        List<String> weakTopics = new ArrayList<>();
        if (quizAttemptRepository != null) {
            List<QuizAttempt> attempts = quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
            Map<String, List<BigDecimal>> topicScores = new HashMap<>();
            for (QuizAttempt qa : attempts) {
                String topicKey = (qa.getTopic() != null && !qa.getTopic().isBlank()) ? qa.getTopic() : "General";
                if (qa.getPercentage() != null) {
                    topicScores.computeIfAbsent(topicKey, k -> new ArrayList<>()).add(qa.getPercentage());
                }
            }
            topicScores.forEach((topic, scores) -> {
                double avg = scores.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
                if (avg < 60.0) {
                    weakTopics.add(topic);
                }
            });
        }

        CareerAdvisorDataDto advisorData = advisorService.generateCareerAdvice(profile, skillGapAnalysis, weakTopics);
        return new CareerAdvisorResponse(true, advisorData);
    }

    @Transactional(readOnly = true)
    public CareerRoadmapResponse runLearningRoadmap(UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        CareerProfile profile = careerProfileRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Career profile required. Please setup your target role and skills first."));

        String targetRole = profile.getTargetRole() != null ? profile.getTargetRole() : "Full Stack Developer";
        SkillGapAnalysisDto skillGapAnalysis = calculateSkillGaps(profile.getSkills(), targetRole);

        CareerRoadmapDataDto roadmapData = advisorService.generateLearningRoadmap(profile, skillGapAnalysis);
        return new CareerRoadmapResponse(true, roadmapData);
    }

    public CareerProfileDto toDto(CareerProfile profile) {
        if (profile == null) return null;

        List<SkillItemDto> skills = profile.getSkills() != null
                ? profile.getSkills().stream().map(s -> new SkillItemDto(s.getSkill(), s.getProficiency())).toList()
                : List.of();

        String createdAtStr = profile.getCreatedAt() != null ? profile.getCreatedAt().toString() : null;
        String updatedAtStr = profile.getUpdatedAt() != null ? profile.getUpdatedAt().toString() : null;

        return new CareerProfileDto(
                profile.getId(),
                profile.getStudentId(),
                profile.getCareerGoal(),
                profile.getTargetRole(),
                profile.getDomain(),
                skills,
                profile.getInterests() != null ? profile.getInterests() : List.of(),
                profile.getExperienceProjects() != null ? profile.getExperienceProjects() : List.of(),
                createdAtStr,
                updatedAtStr
        );
    }
}
