package com.linglevel.api.streak.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "보상 정보")
public class RewardInfo {

    @Schema(description = "획득한 티켓 개수", example = "1")
    private Integer tickets;

    @Schema(description = "획득한 프리즈 개수", example = "1")
    private Integer freezes;
}
