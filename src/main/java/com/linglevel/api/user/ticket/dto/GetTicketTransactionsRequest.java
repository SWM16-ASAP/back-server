package com.linglevel.api.user.ticket.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class GetTicketTransactionsRequest {
    
    @Parameter(description = "페이지 번호", example = "1")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private Integer page = 1;
    
    @Parameter(description = "페이지 당 항목 수", example = "10")
    @Min(value = 1, message = "페이지 당 항목 수는 1 이상이어야 합니다.")
    @Max(value = 200, message = "페이지 당 항목 수는 200 이하여야 합니다.")
    private Integer limit = 10;
}