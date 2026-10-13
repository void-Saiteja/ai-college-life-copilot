package com.collegecopilot.student.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubjectDto {

    private String id;
    private String code;
    private String name;
    private String instructor;
    private Integer credits;
    private String department;

    public SubjectDto() {
    }

    public SubjectDto(String id, String code, String name, String instructor, Integer credits, String department) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.instructor = instructor;
        this.credits = credits;
        this.department = department;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }

    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
