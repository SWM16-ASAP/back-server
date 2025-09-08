package com.linglevel.api.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
public class FirebaseConfig {
    
    @Value("${firebase.config}")
    private String firebaseConfig;
    
    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {
        byte[] decodedConfig = Base64.getDecoder().decode(firebaseConfig);
        
        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new ByteArrayInputStream(decodedConfig)
        );

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        FirebaseApp.initializeApp(options);
        return FirebaseAuth.getInstance(FirebaseApp.getInstance());
    }
    
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseAuth firebaseAuth) {
        return FirebaseMessaging.getInstance(FirebaseApp.getInstance());
    }
}