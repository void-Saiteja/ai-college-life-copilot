package com.collegecopilot.budget.dto;

import java.util.ArrayList;
import java.util.List;

public class MealIdeaDto {

    private String name;
    private String cost;
    private String time;
    private List<String> ingredients = new ArrayList<>();
    private String instructions;

    public MealIdeaDto() {
    }

    public MealIdeaDto(String name, String cost, String time, List<String> ingredients, String instructions) {
        this.name = name;
        this.cost = cost;
        this.time = time;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
        this.instructions = instructions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
