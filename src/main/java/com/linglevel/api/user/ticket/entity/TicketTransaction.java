package com.linglevel.api.user.ticket.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ticketTransactions")
@CompoundIndex(name = "userId_createdAt", def = "{'userId': 1, 'createdAt': -1}")
public class TicketTransaction {
    
    @Id
    private String id;
    
    private String userId;
    
    private Integer amount; // 양수: 획득, 음수: 사용
    
    private String description;
    
    @CreatedDate
    private LocalDateTime createdAt;
}