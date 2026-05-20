package com.linglevel.api.word.service;

import com.linglevel.api.word.exception.WordsErrorCode;
import lombok.Getter;

@Getter
public class WordSingleFlightLeaderFailureException extends RuntimeException {

    private final WordsErrorCode leaderErrorCode;

    public WordSingleFlightLeaderFailureException(String message) {
        this(message, null);
    }

    public WordSingleFlightLeaderFailureException(String message, WordsErrorCode leaderErrorCode) {
        super(message);
        this.leaderErrorCode = leaderErrorCode;
    }
}
