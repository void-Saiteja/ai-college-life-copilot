package com.collegecopilot.career.dto;

import java.util.ArrayList;
import java.util.List;

public class SkillGapAnalysisDto {

    private String targetRole;
    private int totalRequired;
    private int strongCount;
    private int partialCount;
    private int gapCount;
    private double coveragePercentage;
    private List<String> strongList = new ArrayList<>();
    private List<String> partialList = new ArrayList<>();
    private List<String> missingList = new ArrayList<>();
    private List<SkillGapItemDto> skillAnalysis = new ArrayList<>();

    public SkillGapAnalysisDto() {
    }

    public SkillGapAnalysisDto(String targetRole, int totalRequired, int strongCount, int partialCount,
                               int gapCount, double coveragePercentage, List<String> strongList,
                               List<String> partialList, List<String> missingList,
                               List<SkillGapItemDto> skillAnalysis) {
        this.targetRole = targetRole;
        this.totalRequired = totalRequired;
        this.strongCount = strongCount;
        this.partialCount = partialCount;
        this.gapCount = gapCount;
        this.coveragePercentage = coveragePercentage;
        this.strongList = strongList != null ? strongList : new ArrayList<>();
        this.partialList = partialList != null ? partialList : new ArrayList<>();
        this.missingList = missingList != null ? missingList : new ArrayList<>();
        this.skillAnalysis = skillAnalysis != null ? skillAnalysis : new ArrayList<>();
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public int getTotalRequired() {
        return totalRequired;
    }

    public void setTotalRequired(int totalRequired) {
        this.totalRequired = totalRequired;
    }

    public int getStrongCount() {
        return strongCount;
    }

    public void setStrongCount(int strongCount) {
        this.strongCount = strongCount;
    }

    public int getPartialCount() {
        return partialCount;
    }

    public void setPartialCount(int partialCount) {
        this.partialCount = partialCount;
    }

    public int getGapCount() {
        return gapCount;
    }

    public void setGapCount(int gapCount) {
        this.gapCount = gapCount;
    }

    public double getCoveragePercentage() {
        return coveragePercentage;
    }

    public void setCoveragePercentage(double coveragePercentage) {
        this.coveragePercentage = coveragePercentage;
    }

    public List<String> getStrongList() {
        return strongList;
    }

    public void setStrongList(List<String> strongList) {
        this.strongList = strongList;
    }

    public List<String> getPartialList() {
        return partialList;
    }

    public void setPartialList(List<String> partialList) {
        this.partialList = partialList;
    }

    public List<String> getMissingList() {
        return missingList;
    }

    public void setMissingList(List<String> missingList) {
        this.missingList = missingList;
    }

    public List<SkillGapItemDto> getSkillAnalysis() {
        return skillAnalysis;
    }

    public void setSkillAnalysis(List<SkillGapItemDto> skillAnalysis) {
        this.skillAnalysis = skillAnalysis;
    }
}
