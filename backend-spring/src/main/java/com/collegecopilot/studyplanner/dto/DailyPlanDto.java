package com.collegecopilot.studyplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyPlanDto {

    private String date;
    private List<DailyTaskDto> tasks = new ArrayList<>();

    public DailyPlanDto() {
    }

    public DailyPlanDto(String date, List<DailyTaskDto> tasks) {
        this.date = date;
        this.tasks = tasks != null ? tasks : new ArrayList<>();
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public List<DailyTaskDto> getTasks() { return tasks; }
    public void setTasks(List<DailyTaskDto> tasks) { this.tasks = tasks; }
}
