package com.linglevel.api.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "메시지 응답")
public class MessageResponse {

    @Schema(description = "응답 메시지", example = "User account deleted successfully.")
    private String message;
}
