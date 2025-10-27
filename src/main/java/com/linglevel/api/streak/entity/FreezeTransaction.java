package com.linglevel.api.streak.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "freezeTransactions")
@CompoundIndex(name = "idx_userId_createdAt", def = "{'userId': 1, 'createdAt': -1}")
public class FreezeTransaction {
    @Id
    private String id;

    @Indexed
    private String userId;
    private Integer amount;
    private String description;
    private Instant createdAt;
}
