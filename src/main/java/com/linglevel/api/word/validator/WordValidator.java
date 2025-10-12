package com.linglevel.api.word.validator;

import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import org.springframework.stereotype.Component;

@Component
public class WordValidator {

    private static final int MAX_WORD_LENGTH = 50;

    public String validateAndPreprocess(String word) {
        if (word == null || word.isEmpty()) {
            throw new WordsException(WordsErrorCode.INVALID_WORD_FORMAT);
        }

        // 1. 앞뒤 특수문자 제거
        String trimmedWord = trimSpecialCharacters(word);

        if (trimmedWord.isEmpty()) {
            throw new WordsException(WordsErrorCode.INVALID_WORD_FORMAT);
        }

        // 2. 대문자를 소문자로 변환
        String processedWord = trimmedWord.toLowerCase();

        // 검증 1: 단어 내부에 특수문자, 띄어쓰기, 엔터, 탭 확인
        if (containsInvalidCharacters(processedWord)) {
            throw new WordsException(WordsErrorCode.INVALID_WORD_FORMAT);
        }

        // 검증 2: 단어 길이 확인
        if (processedWord.length() > MAX_WORD_LENGTH) {
            throw new WordsException(WordsErrorCode.WORD_TOO_LONG);
        }

        return processedWord;
    }

    /**
     * 문자열 앞뒤의 특수문자를 제거합니다.
     * 유효한 문자(문자 또는 숫자)만 남기고 앞뒤를 trim합니다.
     */
    private String trimSpecialCharacters(String word) {
        int start = 0;
        int end = word.length();

        // 앞쪽 특수문자 제거
        while (start < end && !isValidCharacter(word.charAt(start))) {
            start++;
        }

        // 뒤쪽 특수문자 제거
        while (end > start && !isValidCharacter(word.charAt(end - 1))) {
            end--;
        }

        return word.substring(start, end);
    }

    /**
     * 유효한 문자인지 확인합니다.
     * 모든 언어의 문자(한글, 영어, 일본어, 중국어 등)와 숫자를 허용합니다.
     */
    private boolean isValidCharacter(char c) {
        return Character.isLetterOrDigit(c);
    }

    /**
     * 단어 내부에 허용되지 않는 문자가 있는지 확인합니다.
     * 띄어쓰기, 엔터, 탭, 특수문자가 포함되어 있으면 true를 반환합니다.
     */
    private boolean containsInvalidCharacters(String word) {
        for (char c : word.toCharArray()) {
            // 유효한 문자(문자 또는 숫자)가 아니면 invalid
            if (!isValidCharacter(c)) {
                return true;
            }
        }
        return false;
    }
}
