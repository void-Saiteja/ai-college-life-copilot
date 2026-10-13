package com.collegecopilot.auth;

import com.collegecopilot.auth.dto.*;
import com.collegecopilot.security.JwtTokenProvider;
import com.collegecopilot.student.Student;
import com.collegecopilot.student.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Value("${ADMIN_SECRET:dev_admin_secret_key_2026}")
    private String configuredAdminSecret;

    public AuthService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       AdminRepository adminRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String adminHeader) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new AuthException(HttpStatus.CONFLICT, "User with this email already exists");
        }

        Role userRole = Role.STUDENT;
        if (request.getRole() != null && "ADMIN".equalsIgnoreCase(request.getRole().trim())) {
            String suppliedSecret = request.getAdminSecret() != null ? request.getAdminSecret() : adminHeader;
            if (suppliedSecret != null && suppliedSecret.equals(configuredAdminSecret)) {
                userRole = Role.ADMIN;
            } else {
                throw new AuthException(HttpStatus.FORBIDDEN,
                        "Forbidden: Admin registration requires valid administrative authorization or secret key");
            }
        }

        String userId = "u-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String passwordHash = passwordEncoder.encode(request.getPassword());

        User newUser = new User(userId, request.getName().trim(), normalizedEmail, passwordHash, userRole);
        userRepository.save(newUser);

        String studentId = null;
        String adminId = null;

        if (userRole == Role.STUDENT) {
            studentId = "std-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
            String studentCode = "CS-" + (1000 + (int)(Math.random() * 9000));
            Integer semester = request.getSemester() != null ? request.getSemester() : 1;
            String department = (request.getDepartment() != null && !request.getDepartment().isBlank())
                    ? request.getDepartment().trim()
                    : "Computer Science & Engineering";

            Student student = new Student(studentId, userId, studentCode, semester, department, new BigDecimal("3.80"));
            studentRepository.save(student);
        } else {
            adminId = "adm-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
            Admin admin = new Admin(adminId, userId, "Academic Administrator");
            adminRepository.save(admin);
        }

        String token = tokenProvider.generateToken(
                newUser.getId(),
                newUser.getName(),
                newUser.getEmail(),
                newUser.getRole().name(),
                studentId,
                adminId
        );

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id(newUser.getId())
                .name(newUser.getName())
                .email(newUser.getEmail())
                .role(newUser.getRole().name())
                .studentId(studentId)
                .adminId(adminId)
                .build();

        return new AuthResponse(true, "Registration successful", token, userSummary);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        Optional<Student> studentOpt = studentRepository.findByUserId(user.getId());
        Optional<Admin> adminOpt = adminRepository.findByUserId(user.getId());

        String studentId = studentOpt.map(Student::getId).orElse(null);
        String adminId = adminOpt.map(Admin::getId).orElse(null);

        String token = tokenProvider.generateToken(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                studentId,
                adminId
        );

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .studentId(studentId)
                .adminId(adminId)
                .semester(studentOpt.map(Student::getSemester).orElse(1))
                .department(studentOpt.map(Student::getDepartment).orElse("Computer Science"))
                .build();

        return new AuthResponse(true, "Login successful", token, userSummary);
    }

    @Transactional(readOnly = true)
    public AuthResponse getMe(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User profile not found"));

        Optional<Student> studentOpt = studentRepository.findByUserId(user.getId());
        Optional<Admin> adminOpt = adminRepository.findByUserId(user.getId());

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .studentId(studentOpt.map(Student::getId).orElse(null))
                .adminId(adminOpt.map(Admin::getId).orElse(null))
                .studentCode(studentOpt.map(Student::getStudentCode).orElse(null))
                .semester(studentOpt.map(Student::getSemester).orElse(1))
                .department(studentOpt.map(Student::getDepartment).orElse("Computer Science"))
                .gpa(studentOpt.map(Student::getGpa).orElse(new BigDecimal("3.80")))
                .build();

        return new AuthResponse(true, userSummary);
    }
}
