package com.linglevel.api.content.custom.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "콘텐츠 타입")
public enum ContentType {
    TEXT("text", "텍스트", "사용자가 직접 입력한 텍스트"),
    LINK("link", "링크", "외부 링크"),
    PDF("pdf", "PDF 파일", "PDF 문서");

    private final String code;
    private final String name;
    private final String description;

    ContentType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}