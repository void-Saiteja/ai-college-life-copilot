package com.collegecopilot.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExamSummaryDto {

    private int total;
    private int upcoming;
    private int today;
    private int verySoon;
    private int within7Days;
    private int completed;

    public ExamSummaryDto() {
    }

    public ExamSummaryDto(int total, int upcoming, int today, int verySoon, int within7Days, int completed) {
        this.total = total;
        this.upcoming = upcoming;
        this.today = today;
        this.verySoon = verySoon;
        this.within7Days = within7Days;
        this.completed = completed;
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getUpcoming() { return upcoming; }
    public void setUpcoming(int upcoming) { this.upcoming = upcoming; }

    public int getToday() { return today; }
    public void setToday(int today) { this.today = today; }

    public int getVerySoon() { return verySoon; }
    public void setVerySoon(int verySoon) { this.verySoon = verySoon; }

    public int getWithin7Days() { return within7Days; }
    public void setWithin7Days(int within7Days) { this.within7Days = within7Days; }

    public int getCompleted() { return completed; }
    public void setCompleted(int completed) { this.completed = completed; }
}
