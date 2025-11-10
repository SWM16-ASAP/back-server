package com.linglevel.api.content.common;

import lombok.Getter;

@Getter
public enum ContentCategory {
    SPORTS("Sports"),
    SCIENCE("Science"),
    TECH("Technology"),
    BUSINESS("Business"),
    EDU("Education"),
    CULTURE("Culture");

    private final String displayName;

    ContentCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * displayName 또는 enum name으로부터 ContentCategory로 변환
     * @param value displayName (예: "Technology") 또는 enum name (예: "TECH")
     * @return 매칭되는 ContentCategory, 없으면 null
     */
    public static ContentCategory fromString(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();

        // 먼저 displayName으로 매칭 시도
        for (ContentCategory category : values()) {
            if (category.displayName.equalsIgnoreCase(trimmedValue)) {
                return category;
            }
        }

        // displayName 매칭 실패시 enum name으로 매칭 시도
        try {
            return ContentCategory.valueOf(trimmedValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
