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
            Word: {word} | Target: {targetLanguage}

            **CRITICAL: If '{word}' is nonsensical/gibberish/typo, return []**

            HOMOGRAPH CHECK: Same spelling, multiple distinct origins?
            (e.g., "saw"=past of see + noun톱, "left"=past of leave + adj왼쪽, "rose"=past of rise + noun장미)
            → YES: Return array with separate entry per origin | NO: Single-element array

            STRUCTURE:
            1. sourceLanguageCode/targetLanguageCode: "EN", "KO", etc.
            2. originalForm: Base form (verbs→infinitive, adj→positive, nouns→singular)
            3. variantType: ORIGINAL_FORM|PAST_TENSE|PAST_PARTICIPLE|PRESENT_PARTICIPLE|THIRD_PERSON|COMPARATIVE|SUPERLATIVE|PLURAL|UNDEFINED
            4. summary: Max 3 common translations (e.g., ["달리다","운영하다"])
            5. meanings: Max 15 objects (common→rare, omit obscure ones)
               - partOfSpeech: verb, noun, adjective, adverb, etc.
               - meaning: Detailed explanation in target language
               - example: **CRITICAL: Wrap <originalForm> NOT input. Avoid he/she/it+verb (grammar error).**
                 CORRECT: "I <run>", "You <run>", "They <run>" | WRONG: "She <run>"✗, "I <ran>"✗
               - exampleTranslation: Translation in target language
            6. conjugations: (verbs only) present, past, pastParticiple, presentParticiple, thirdPerson
            7. comparatives: (adj only) positive, comparative, superlative
            8. plural: (nouns only) singular, plural

            EXAMPLE - "saw" homograph:
            [
              {{
                "originalForm": "see",
                "variantType": "PAST_TENSE",
                "sourceLanguageCode": "EN",
                "targetLanguageCode": "KO",
                "summary": ["보다", "알다"],
                "meanings": [
                  {{
                    "partOfSpeech": "verb",
                    "meaning": "시각적으로 인지하다",
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
                "summary": ["톱"],
                "meanings": [
                  {{
                    "partOfSpeech": "noun",
                    "meaning": "톱 (자르는 도구)",
                    "example": "I need a <saw>.",
                    "exampleTranslation": "톱이 필요합니다."
                  }}
                ],
                "conjugations": null,
                "comparatives": null,
                "plural": {{"singular": "saw", "plural": "saws"}}
              }}
            ]

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
