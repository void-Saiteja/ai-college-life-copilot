package com.collegecopilot.student.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentProfileDto {

    private String id;
    private String name;
    private String email;
    private Integer semester;
    private String department;
    private BigDecimal gpa;

    public StudentProfileDto() {
    }

    public StudentProfileDto(String id, String name, String email, Integer semester, String department, BigDecimal gpa) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.semester = semester;
        this.department = department;
        this.gpa = gpa;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
}
