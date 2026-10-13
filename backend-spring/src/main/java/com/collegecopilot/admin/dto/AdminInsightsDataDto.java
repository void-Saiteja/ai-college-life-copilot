package com.collegecopilot.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminInsightsDataDto {

    private List<String> keyObservations;
    private List<String> areasRequiringAttention;
    private List<String> notableAcademicPatterns;
    private List<String> suggestedAdministrativeActions;
    private String caveatsAndLimitations;
    private Boolean isAiGenerated;

    public AdminInsightsDataDto() {
    }

    public AdminInsightsDataDto(List<String> keyObservations, List<String> areasRequiringAttention,
                                List<String> notableAcademicPatterns, List<String> suggestedAdministrativeActions,
                                String caveatsAndLimitations, Boolean isAiGenerated) {
        this.keyObservations = keyObservations;
        this.areasRequiringAttention = areasRequiringAttention;
        this.notableAcademicPatterns = notableAcademicPatterns;
        this.suggestedAdministrativeActions = suggestedAdministrativeActions;
        this.caveatsAndLimitations = caveatsAndLimitations;
        this.isAiGenerated = isAiGenerated;
    }

    public List<String> getKeyObservations() { return keyObservations; }
    public void setKeyObservations(List<String> keyObservations) { this.keyObservations = keyObservations; }

    public List<String> getAreasRequiringAttention() { return areasRequiringAttention; }
    public void setAreasRequiringAttention(List<String> areasRequiringAttention) { this.areasRequiringAttention = areasRequiringAttention; }

    public List<String> getNotableAcademicPatterns() { return notableAcademicPatterns; }
    public void setNotableAcademicPatterns(List<String> notableAcademicPatterns) { this.notableAcademicPatterns = notableAcademicPatterns; }

    public List<String> getSuggestedAdministrativeActions() { return suggestedAdministrativeActions; }
    public void setSuggestedAdministrativeActions(List<String> suggestedAdministrativeActions) { this.suggestedAdministrativeActions = suggestedAdministrativeActions; }

    public String getCaveatsAndLimitations() { return caveatsAndLimitations; }
    public void setCaveatsAndLimitations(String caveatsAndLimitations) { this.caveatsAndLimitations = caveatsAndLimitations; }

    public Boolean getIsAiGenerated() { return isAiGenerated; }
    public void setIsAiGenerated(Boolean isAiGenerated) { this.isAiGenerated = isAiGenerated; }
}
