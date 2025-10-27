package com.linglevel.api.streak.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "freezeTransactions")
@Getter
@Setter
@Builder
public class FreezeTransaction {
    @Id
    private String id;

    @Indexed
    private String userId;

    private Integer amount;

    private String description;

    private Instant createdAt;
}