package com.linglevel.api.i18n;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CountryCode {
    KR("KR", "South Korea"),
    US("US", "United States"),
    JP("JP", "Japan");

    private final String code;
    private final String description;
}
