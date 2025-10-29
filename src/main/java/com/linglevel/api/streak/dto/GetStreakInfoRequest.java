package com.linglevel.api.streak.dto;

import com.linglevel.api.i18n.LanguageCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "스트릭 정보 조회 요청")
public class GetStreakInfoRequest {

    @Builder.Default
    @Schema(description = "언어 코드 (기본값: EN)", example = "EN", required = false)
    private LanguageCode languageCode = LanguageCode.EN;
}
