package com.linglevel.api.auth.jwt;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String tokenId;
    
    @Indexed
    private String userId;
    
    @Indexed(name = "ttl_expires_at", expireAfter = "0s")
    private LocalDateTime expiresAt;
    
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}