package com.linglevel.api.user.service;

import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.exception.UsersErrorCode;
import com.linglevel.api.user.exception.UsersException;
import com.linglevel.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsersService {
    
    private final UserRepository userRepository;
    
    @Transactional
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsersException(UsersErrorCode.USER_NOT_FOUND));
        
        if (user.getDeleted()) {
            throw new UsersException(UsersErrorCode.USER_NOT_FOUND);
        }

        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        String originalUsername = user.getUsername();
        user.setUsername("deleted_" + user.getDeletedAt() + "_" + originalUsername);
        
        userRepository.save(user);
        log.info("User deleted successfully: {}", originalUsername);
    }
}