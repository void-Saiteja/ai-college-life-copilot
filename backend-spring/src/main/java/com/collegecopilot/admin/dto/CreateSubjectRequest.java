package com.collegecopilot.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateSubjectRequest {

    @NotBlank(message = "Course code and name are required")
    private String code;

    @NotBlank(message = "Course code and name are required")
    private String name;

    private String instructor;
    private Integer credits = 3;

    public CreateSubjectRequest() {
    }

    public CreateSubjectRequest(String code, String name, String instructor, Integer credits) {
        this.code = code;
        this.name = name;
        this.instructor = instructor;
        this.credits = credits != null ? credits : 3;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public Integer getCredits() {
        return credits != null ? credits : 3;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }
}
