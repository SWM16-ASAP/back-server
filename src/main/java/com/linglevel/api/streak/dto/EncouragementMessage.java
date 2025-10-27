package com.linglevel.api.streak.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "격려 메시지")
public class EncouragementMessage {

    @Schema(description = "메시지 제목", example = "25일 연속!")
    private String title;

    @Schema(description = "메시지 본문 (영문)", example = "You're in the top 5% of all users!")
    private String body;

    @Schema(description = "메시지 번역 (한글)", example = "전체 사용자 중 상위 5%입니다!")
    private String translation;
}
