package com.linglevel.api.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.linglevel.api.auth.dto.LoginResponse;
import com.linglevel.api.auth.exception.AuthException;
import com.linglevel.api.auth.exception.AuthErrorCode;
import com.linglevel.api.auth.jwt.JwtTokenProvider;
import com.linglevel.api.users.entity.User;
import com.linglevel.api.users.entity.UserRole;
import com.linglevel.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse authenticateWithFirebase(String authCode) {
        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(authCode);

            String email = decodedToken.getEmail();
            String provider = getProviderFromToken(decodedToken);
            String username = provider + "_" + decodedToken.getUid();

            User user = findOrCreateUser(username, email, provider);

            String accessToken = jwtTokenProvider.createToken(user.getUsername());
            String refreshToken = ""; // todo: add refresh token
            
            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
                    
        } catch (FirebaseAuthException e) {
            log.error("Firebase authentication failed: {}", e.getMessage());
            throw new AuthException(AuthErrorCode.INVALID_FIREBASE_TOKEN);
        }
    }

    private User findOrCreateUser(String username, String email, String provider) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setEmail(email);
            return userRepository.save(user);
        } else {
            User newUser = User.builder()
                    .username(username)
                    .provider(provider)
                    .email(email)
                    .displayName(email)
                    .role(UserRole.USER)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            log.info("Creating new user: {}", email);
            return userRepository.save(newUser);
        }
    }

    private String getProviderFromToken(FirebaseToken token) {
        String issuer = token.getIssuer();
        if (issuer != null && issuer.contains("google")) {
            return "google";
        } else if (issuer != null && issuer.contains("apple")) {
            return "apple";
        }
        return "firebase"; // default value
    }
}