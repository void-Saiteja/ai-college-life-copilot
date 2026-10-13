package com.collegecopilot.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Name, email, and password are required")
    private String name;

    @NotBlank(message = "Name, email, and password are required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Name, email, and password are required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    private String role = "STUDENT";
    private String department;
    private Integer semester;
    private String adminSecret;

    public RegisterRequest() {
    }

    public RegisterRequest(String name, String email, String password, String role, String department, Integer semester) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : "STUDENT";
        this.department = department;
        this.semester = semester;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public String getAdminSecret() {
        return adminSecret;
    }

    public void setAdminSecret(String adminSecret) {
        this.adminSecret = adminSecret;
    }
}
