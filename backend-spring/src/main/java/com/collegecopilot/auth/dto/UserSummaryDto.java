package com.collegecopilot.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSummaryDto {

    private String id;
    private String name;
    private String email;
    private String role;
    private String studentId;
    private String adminId;
    private String studentCode;
    private Integer semester;
    private String department;
    private BigDecimal gpa;

    public UserSummaryDto() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final UserSummaryDto dto = new UserSummaryDto();

        public Builder id(String id) { dto.id = id; return this; }
        public Builder name(String name) { dto.name = name; return this; }
        public Builder email(String email) { dto.email = email; return this; }
        public Builder role(String role) { dto.role = role; return this; }
        public Builder studentId(String studentId) { dto.studentId = studentId; return this; }
        public Builder adminId(String adminId) { dto.adminId = adminId; return this; }
        public Builder studentCode(String studentCode) { dto.studentCode = studentCode; return this; }
        public Builder semester(Integer semester) { dto.semester = semester; return this; }
        public Builder department(String department) { dto.department = department; return this; }
        public Builder gpa(BigDecimal gpa) { dto.gpa = gpa; return this; }

        public UserSummaryDto build() { return dto; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public BigDecimal getGpa() { return gpa; }
    public void setGpa(BigDecimal gpa) { this.gpa = gpa; }
}
