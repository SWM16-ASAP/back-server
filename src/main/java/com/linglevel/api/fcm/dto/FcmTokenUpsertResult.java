package com.linglevel.api.fcm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FcmTokenUpsertResult {
    private String tokenId;
    private boolean created;
}