package com.linglevel.api.users.entity;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user")
public class User {
    
    @Id
    private String id;

    @NotNull
    @Indexed(unique = true)
    private String username;

    private String password;

    private String email;

    private String displayName;

    private String provider;

    private String profileImageUrl;

    private UserRole role;

    private Boolean deleted;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

}
