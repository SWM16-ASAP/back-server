package com.linglevel.api.word.service;

import com.linglevel.api.word.dto.WordAnalysisResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WordAiService {

    private final ChatModel chatModel;
    private final Validator validator;

    public WordAiService(ChatModel chatModel) {
        this.chatModel = chatModel;
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    private static final String PROMPT_TEMPLATE = """
            Analyze the word: {word}
            Target translation language: {targetLanguage}

            IMPORTANT: Check if this word is a HOMOGRAPH (same spelling but multiple completely different origins/meanings).
            Examples of homographs:
            - "saw": 1) past tense of "see" (보다) AND 2) noun "saw" (톱, 도구)
            - "rose": 1) past tense of "rise" (오르다) AND 2) noun "rose" (장미)
            - "left": 1) past tense of "leave" (떠나다) AND 2) adjective "left" (왼쪽의)

            If this is a HOMOGRAPH with multiple distinct origins:
            - Return a JSON ARRAY with separate analysis for EACH original form
            - Each analysis should be independent with its own originalForm, meanings, relatedForms, etc.

            For EACH distinct word origin, provide:
            1. sourceLanguageCode: The language of the input word (e.g., "EN", "KO", "JA")
            2. targetLanguageCode: The target translation language code (e.g., "KO", "EN", "JA")
            3. originalForm: The base/dictionary form following these rules:
               * For verbs: base form/infinitive (e.g., "run" for "ran")
               * For adjectives: positive form (e.g., "pretty" for "prettiest")
               * For nouns: singular form (e.g., "saw" for "saws")
               * If input is already the base form, return the same word
            4. variantType: ORIGINAL_FORM, PAST_TENSE, PAST_PARTICIPLE, PRESENT_PARTICIPLE, THIRD_PERSON, COMPARATIVE, SUPERLATIVE, PLURAL, or UNDEFINED
            5. summary: List of 3 most frequently used meanings in target language (e.g., ["보다", "알다", "이해하다"])
            6. meanings: Array of meaning objects, each containing:
               * partOfSpeech: "verb", "noun", "adjective", "adverb", etc.
               * meaning: Detailed explanation in target language
               * example: English sentence using the originalForm wrapped in angle brackets <originalForm>
               * exampleTranslation: Translation of the example in target language
            7. conjugations: (only for verbs) present, past, pastParticiple, presentParticiple, thirdPerson
            8. comparatives: (only for adjectives) positive, comparative, superlative
            9. plural: (only for nouns) singular, plural

            Example for homograph "saw":
            [
              {{
                "originalForm": "see",
                "variantType": "PAST_TENSE",
                "sourceLanguageCode": "EN",
                "targetLanguageCode": "KO",
                "summary": ["보다", "알다", "이해하다"],
                "meanings": [
                  {{
                    "partOfSpeech": "verb",
                    "meaning": "보다, 시각적으로 인지하다",
                    "example": "I <see> him every day.",
                    "exampleTranslation": "나는 매일 그를 봅니다."
                  }}
                ],
                "conjugations": {{"present": "see", "past": "saw", "pastParticiple": "seen", "presentParticiple": "seeing", "thirdPerson": "sees"}},
                "comparatives": null,
                "plural": null
              }},
              {{
                "originalForm": "saw",
                "variantType": "ORIGINAL_FORM",
                "sourceLanguageCode": "EN",
                "targetLanguageCode": "KO",
                "summary": ["톱", "톱질하다"],
                "meanings": [
                  {{
                    "partOfSpeech": "noun",
                    "meaning": "톱 (나무나 금속을 자르는 도구)",
                    "example": "I need a <saw>.",
                    "exampleTranslation": "나는 톱이 필요합니다."
                  }}
                ],
                "conjugations": null,
                "comparatives": null,
                "plural": {{"singular": "saw", "plural": "saws"}}
              }}
            ]

            If NOT a homograph, return a single-element array.

            {format}
            """;

    public List<WordAnalysisResult> analyzeWord(String word, String targetLanguage) {
        try {
            BeanOutputConverter<WordAnalysisResult[]> outputConverter =
                new BeanOutputConverter<>(WordAnalysisResult[].class);

            String format = outputConverter.getFormat();

            PromptTemplate promptTemplate = new PromptTemplate(PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                "word", word,
                "targetLanguage", targetLanguage,
                "format", format
            ));

            String response = ChatClient.create(chatModel)
                    .prompt(prompt)
                    .call()
                    .content();

            log.info("AI Response for word '{}' (target: {}): {}", word, targetLanguage, response);

            WordAnalysisResult[] results = outputConverter.convert(response);

            if (results == null || results.length == 0) {
                throw new IllegalStateException("AI returned empty result for word: " + word);
            }

            // Validation 수행
            for (WordAnalysisResult result : results) {
                validateResult(result, word);
            }

            return Arrays.asList(results);
        } catch (Exception e) {
            log.error("Failed to analyze word '{}' with AI (target: {})", word, targetLanguage, e);
            throw new RuntimeException("AI word analysis failed for word: " + word, e);
        }
    }

    /**
     * AI 응답 결과의 유효성을 검증
     */
    private void validateResult(WordAnalysisResult result, String word) {
        Set<ConstraintViolation<WordAnalysisResult>> violations = validator.validate(result);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            log.error("AI response validation failed for word '{}': {}", word, errors);
            throw new IllegalArgumentException("Invalid AI response for word '" + word + "': " + errors);
        }

        log.debug("AI response validation passed for word '{}'", word);
    }
}
