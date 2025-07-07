package com.linglevel.api.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공통 예외처리 응답")
public class ExceptionResponseDTO {
    @Schema(description = "에러 메시지")
    private String message;

    public ExceptionResponseDTO(Exception exception) {
        this.message = exception.getMessage();
    }
}