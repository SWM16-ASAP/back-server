package com.linglevel.api.streak.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FreezeTransactionResponse {
    private String id;
    private Integer amount;
    private String description;
    private Instant createdAt;
}
