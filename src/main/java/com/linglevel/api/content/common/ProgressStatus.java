package com.linglevel.api.content.common;

import lombok.Getter;

@Getter
public enum ProgressStatus {
    NOT_STARTED("not_started", "Content has not been started yet"),
    IN_PROGRESS("in_progress", "Content is currently being read"),
    COMPLETED("completed", "Content has been fully completed");

    private final String code;
    private final String description;

    ProgressStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ProgressStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ProgressStatus status : ProgressStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid progress status code: " + code);
    }

    public static ProgressStatus fromProgressData(double progressPercentage, boolean isCompleted) {
        if (isCompleted) {
            return COMPLETED;
        } else if (progressPercentage > 0.0) {
            return IN_PROGRESS;
        } else {
            return NOT_STARTED;
        }
    }
}