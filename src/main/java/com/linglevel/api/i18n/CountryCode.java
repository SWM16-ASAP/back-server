package com.linglevel.api.i18n;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CountryCode {
    KR("KR", "대한민국", "South Korea"),
    US("US", "미국", "United States"),
    JP("JP", "일본", "Japan");

    private final String code;
    private final String koreanName;
    private final String englishName;
}
