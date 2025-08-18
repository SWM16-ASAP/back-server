package com.linglevel.api.content.common.service;

import com.linglevel.api.content.common.DifficultyLevel;
import org.springframework.stereotype.Service;

@Service
public class ReadingTimeService {

    private final int AVERAGE_READING_SPEED_PER_MINUTE = 500;

    public int calculateReadingTimeFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return calculateReadingTimeFromCharacters(text.length());
    }

    public int calculateReadingTimeFromCharacters(int characterCount) {
        return (int) Math.ceil((double) characterCount / AVERAGE_READING_SPEED_PER_MINUTE);
    }
}