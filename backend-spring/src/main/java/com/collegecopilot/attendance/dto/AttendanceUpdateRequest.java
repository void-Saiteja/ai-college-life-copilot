package com.collegecopilot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AttendanceUpdateRequest {

    @NotNull(message = "Classes conducted is required")
    @Min(value = 0, message = "Classes conducted must be non-negative")
    @JsonProperty("classes_conducted")
    @JsonAlias({"classesConducted", "classes_conducted"})
    private Integer classesConducted;

    @NotNull(message = "Classes attended is required")
    @Min(value = 0, message = "Classes attended must be non-negative")
    @JsonProperty("classes_attended")
    @JsonAlias({"classesAttended", "classes_attended"})
    private Integer classesAttended;

    public AttendanceUpdateRequest() {
    }

    public AttendanceUpdateRequest(Integer classesConducted, Integer classesAttended) {
        this.classesConducted = classesConducted;
        this.classesAttended = classesAttended;
    }

    public Integer getClassesConducted() { return classesConducted; }
    public void setClassesConducted(Integer classesConducted) { this.classesConducted = classesConducted; }

    public Integer getClassesAttended() { return classesAttended; }
    public void setClassesAttended(Integer classesAttended) { this.classesAttended = classesAttended; }
}
