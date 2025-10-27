package com.linglevel.api.streak.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreakService {

    public boolean hasCompletedStreakToday(String userId) {
        // TODO: Implement logic to check if the user has already completed a streak today
        return false;
    }
}