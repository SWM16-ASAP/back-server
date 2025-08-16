package com.linglevel.api.content.common;

import lombok.Getter;

@Getter
public enum DifficultyLevel {
    A0("A0", "Pre-Beginner", "Complete beginner level"),
    A1("A1", "Beginner", "Basic user level"),
    A2("A2", "Elementary", "Basic user level"),
    B1("B1", "Intermediate", "Independent user level"),
    B2("B2", "Upper-Intermediate", "Independent user level"),
    C1("C1", "Advanced", "Proficient user level"),
    C2("C2", "Proficiency", "Proficient user level");

    private final String code;
    private final String name;
    private final String description;

    DifficultyLevel(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
