package com.collegecopilot.career.dto;

public class CareerProfileDataDto {

    private CareerProfileDto profile;
    private SkillGapAnalysisDto skillGapAnalysis;

    public CareerProfileDataDto() {
    }

    public CareerProfileDataDto(CareerProfileDto profile, SkillGapAnalysisDto skillGapAnalysis) {
        this.profile = profile;
        this.skillGapAnalysis = skillGapAnalysis;
    }

    public CareerProfileDto getProfile() {
        return profile;
    }

    public void setProfile(CareerProfileDto profile) {
        this.profile = profile;
    }

    public SkillGapAnalysisDto getSkillGapAnalysis() {
        return skillGapAnalysis;
    }

    public void setSkillGapAnalysis(SkillGapAnalysisDto skillGapAnalysis) {
        this.skillGapAnalysis = skillGapAnalysis;
    }
}
