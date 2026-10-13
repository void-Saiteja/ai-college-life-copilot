package com.collegecopilot.auth;

import com.collegecopilot.auth.dto.*;
import com.collegecopilot.security.JwtTokenProvider;
import com.collegecopilot.student.Student;
import com.collegecopilot.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "configuredAdminSecret", "dev_admin_secret_key_2026");
    }

    @Test
    @DisplayName("Register student successfully")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "password123", "STUDENT", "Computer Science", 1);

        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(tokenProvider.generateToken(anyString(), eq("Jane Doe"), eq("jane@example.com"), eq("STUDENT"), anyString(), isNull()))
                .thenReturn("mocked.jwt.token");

        AuthResponse response = authService.register(request, null);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Registration successful");
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("jane@example.com");
        assertThat(response.getUser().getRole()).isEqualTo("STUDENT");

        verify(userRepository).save(any(User.class));
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    @DisplayName("Register fails when email is duplicate")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "password123", "STUDENT", null, null);
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, null))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException authEx = (AuthException) ex;
                    assertThat(authEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(authEx.getMessage()).isEqualTo("User with this email already exists");
                });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register admin fails without valid admin secret")
    void testRegisterAdminUnauthorized() {
        RegisterRequest request = new RegisterRequest("Admin User", "admin@example.com", "password123", "ADMIN", null, null);
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request, "wrong-secret"))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException authEx = (AuthException) ex;
                    assertThat(authEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(authEx.getMessage()).contains("Forbidden: Admin registration requires valid administrative authorization");
                });
    }

    @Test
    @DisplayName("Login successfully with valid credentials")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("alex@student.edu", "secret123");
        User user = new User("u-1", "Alex Mercer", "alex@student.edu", "hashed_pass", Role.STUDENT);
        Student student = new Student("std-1", "u-1", "CS-1001", 3, "CS", new BigDecimal("3.90"));

        when(userRepository.findByEmailIgnoreCase("alex@student.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed_pass")).thenReturn(true);
        when(studentRepository.findByUserId("u-1")).thenReturn(Optional.of(student));
        when(adminRepository.findByUserId("u-1")).thenReturn(Optional.empty());
        when(tokenProvider.generateToken("u-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null))
                .thenReturn("valid.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Login successful");
        assertThat(response.getToken()).isEqualTo("valid.jwt.token");
        assertThat(response.getUser().getId()).isEqualTo("u-1");
        assertThat(response.getUser().getStudentId()).isEqualTo("std-1");
    }

    @Test
    @DisplayName("Login fails when user not found")
    void testLoginUserNotFound() {
        LoginRequest request = new LoginRequest("notfound@student.edu", "secret123");
        when(userRepository.findByEmailIgnoreCase("notfound@student.edu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException authEx = (AuthException) ex;
                    assertThat(authEx.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(authEx.getMessage()).isEqualTo("Invalid email or password");
                });
    }

    @Test
    @DisplayName("Login fails when password does not match")
    void testLoginPasswordMismatch() {
        LoginRequest request = new LoginRequest("alex@student.edu", "wrongpass");
        User user = new User("u-1", "Alex Mercer", "alex@student.edu", "hashed_pass", Role.STUDENT);

        when(userRepository.findByEmailIgnoreCase("alex@student.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed_pass")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException authEx = (AuthException) ex;
                    assertThat(authEx.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(authEx.getMessage()).isEqualTo("Invalid email or password");
                });
    }

    @Test
    @DisplayName("Get me returns student profile without password hash")
    void testGetMeSuccess() {
        User user = new User("u-1", "Alex Mercer", "alex@student.edu", "hashed_pass", Role.STUDENT);
        Student student = new Student("std-1", "u-1", "CS-1001", 4, "Computer Science", new BigDecimal("3.85"));

        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(studentRepository.findByUserId("u-1")).thenReturn(Optional.of(student));
        when(adminRepository.findByUserId("u-1")).thenReturn(Optional.empty());

        AuthResponse response = authService.getMe("u-1");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getUser().getId()).isEqualTo("u-1");
        assertThat(response.getUser().getName()).isEqualTo("Alex Mercer");
        assertThat(response.getUser().getEmail()).isEqualTo("alex@student.edu");
        assertThat(response.getUser().getStudentId()).isEqualTo("std-1");
        assertThat(response.getUser().getStudentCode()).isEqualTo("CS-1001");
        assertThat(response.getUser().getGpa()).isEqualTo(new BigDecimal("3.85"));
    }

    @Test
    @DisplayName("Get me throws 404 when user is not found")
    void testGetMeUserNotFound() {
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe("non-existent"))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException authEx = (AuthException) ex;
                    assertThat(authEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(authEx.getMessage()).isEqualTo("User profile not found");
                });
    }
}
