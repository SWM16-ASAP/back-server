package com.linglevel.api.users.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    private LocalDateTime createdAt;

}
