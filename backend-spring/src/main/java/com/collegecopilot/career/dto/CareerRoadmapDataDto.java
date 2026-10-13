package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class CareerRoadmapDataDto {

    private String summary;
    private List<RoadmapPhaseDto> phases = new ArrayList<>();

    @JsonProperty("isFallback")
    private boolean isFallback;

    public CareerRoadmapDataDto() {
    }

    public CareerRoadmapDataDto(String summary, List<RoadmapPhaseDto> phases, boolean isFallback) {
        this.summary = summary;
        this.phases = phases != null ? phases : new ArrayList<>();
        this.isFallback = isFallback;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<RoadmapPhaseDto> getPhases() {
        return phases;
    }

    public void setPhases(List<RoadmapPhaseDto> phases) {
        this.phases = phases;
    }

    public boolean isFallback() {
        return isFallback;
    }

    public void setFallback(boolean fallback) {
        isFallback = fallback;
    }
}
