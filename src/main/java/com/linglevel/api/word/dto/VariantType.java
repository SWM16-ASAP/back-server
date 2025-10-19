package com.linglevel.api.word.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum VariantType {
    ORIGINAL_FORM,            // 원형
    PAST_TENSE,              // 과거형
    PAST_PARTICIPLE,         // 과거분사
    PRESENT_PARTICIPLE,      // 현재분사
    THIRD_PERSON,            // 3인칭 단수
    COMPARATIVE,             // 비교급
    SUPERLATIVE,             // 최상급
    PLURAL,                  // 복수형
    UNDEFINED;               // 정의되지 않은 변형 (소유격 등)

    @JsonCreator
    public static VariantType fromString(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim().toUpperCase();
        for (VariantType vt : VariantType.values()) {
            if (vt.name().equals(normalizedValue)) {
                return vt;
            }
        }

        // AI가 잘못된 값을 반환한 경우 null 반환 (후처리에서 필터링됨)
        return null;
    }
}

