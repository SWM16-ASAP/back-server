package com.linglevel.api.version.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "appVersion")
public class AppVersion {
    @Id
    private String id;
    
    private String latestVersion;
    
    private String minimumVersion;
    
    private LocalDateTime updatedAt;
}