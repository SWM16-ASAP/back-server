package com.linglevel.api.content.custom.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "콘텐츠 요청 처리 상태")
public enum ContentRequestStatus {
    PENDING("pending", "대기 중", "AI 처리 대기 중인 상태"),
    PROCESSING("processing", "처리 중", "AI가 콘텐츠를 처리하고 있는 상태"),
    COMPLETED("completed", "완료", "AI 처리가 성공적으로 완료된 상태"),
    FAILED("failed", "실패", "AI 처리가 실패한 상태"),
    DELETED("deleted", "삭제됨", "사용자가 삭제한 상태");

    private final String code;
    private final String name;
    private final String description;

    ContentRequestStatus(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}