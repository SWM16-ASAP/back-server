package com.linglevel.api.word.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PartOfSpeech {
    NOUN("noun", "명사"),
    VERB("verb", "동사"),
    ADJECTIVE("adjective", "형용사"),
    ADVERB("adverb", "부사"),
    PRONOUN("pronoun", "대명사"),
    PREPOSITION("preposition", "전치사"),
    CONJUNCTION("conjunction", "접속사"),
    INTERJECTION("interjection", "감탄사"),
    DETERMINER("determiner", "한정사"),
    ARTICLE("article", "관사"),
    NUMERAL("numeral", "수사");

    private final String value;
    private final String korean;

    PartOfSpeech(String value, String korean) {
        this.value = value;
        this.korean = korean;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getKorean() {
        return korean;
    }

    @JsonCreator
    public static PartOfSpeech fromString(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim().toLowerCase();
        for (PartOfSpeech pos : PartOfSpeech.values()) {
            if (pos.value.equals(normalizedValue)) {
                return pos;
            }
        }

        throw new IllegalArgumentException("Unknown part of speech: " + value +
            ". Valid values are: noun, verb, adjective, adverb, pronoun, preposition, conjunction, interjection, determiner, article, numeral");
    }

    @Override
    public String toString() {
        return value;
    }
}