package com.linglevel.api.i18n;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LanguageCode {
    KO("KO", "Korean"),
    EN("EN", "English"),
    JA("JA", "Japanese");

    private final String code;
    private final String description;
}