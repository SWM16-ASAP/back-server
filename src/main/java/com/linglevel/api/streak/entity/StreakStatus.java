package com.linglevel.api.streak.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StreakStatus {

    COMPLETED("COMPLETED", "완료"),
    FREEZE_USED("FREEZE_USED", "프리즈 사용"),
    MISSED("MISSED", "놓침");

    private final String code;
    private final String name;
}
