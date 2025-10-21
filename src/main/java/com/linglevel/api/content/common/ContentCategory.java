package com.linglevel.api.content.common;

import lombok.Getter;

@Getter
public enum ContentCategory {
    SPORTS("Sports"),
    SCIENCE("Science"),
    TECH("Technology"),
    BUSINESS("Business"),
    CULTURE("Culture");

    private final String displayName;

    ContentCategory(String displayName) {
        this.displayName = displayName;
    }

    public static ContentCategory fromString(String tag) {
        if (tag == null) {
            return null;
        }

        for (ContentCategory category : values()) {
            if (category.displayName.equalsIgnoreCase(tag.trim())) {
                return category;
            }
        }
        return null;
    }
}
