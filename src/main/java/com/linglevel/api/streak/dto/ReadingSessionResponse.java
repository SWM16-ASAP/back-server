package com.linglevel.api.streak.dto;

import com.linglevel.api.content.common.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "읽기 세션 정보")
public class ReadingSessionResponse {

    @Schema(description = "콘텐츠 타입", example = "BOOK")
    private ContentType contentType;

    @Schema(description = "콘텐츠 ID", example = "123")
    private String contentId;

    @Schema(description = "세션 시작 시간 (ISO-8601)", example = "2025-10-31T12:34:56Z")
    private Instant startedAt;

    @Schema(description = "현재까지 읽기 지속 시간 (초)", example = "120")
    private long durationSeconds;

    public static ReadingSessionResponse from(ReadingSession session) {
        Instant startedAt = Instant.ofEpochMilli(session.getStartedAtMillis());
        long durationSeconds = java.time.Duration.between(startedAt, Instant.now()).getSeconds();

        return ReadingSessionResponse.builder()
                .contentType(session.getContentType())
                .contentId(session.getContentId())
                .startedAt(startedAt)
                .durationSeconds(durationSeconds)
                .build();
    }
}
