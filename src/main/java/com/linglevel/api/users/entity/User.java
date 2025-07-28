package com.linglevel.api.users.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    private String id;

    private String provider;

    private String providerId;

    private String email;

    private String name;

    private String profileImageUrl;

    private UserRole role;

    private LocalDateTime createdAt;

}
