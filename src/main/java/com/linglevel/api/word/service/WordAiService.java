package com.linglevel.api.word.service;

import com.linglevel.api.word.dto.WordAnalysisResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
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

            HOMOGRAPH CHECK: Same spelling, multiple DISTINCT ORIGINS or DIFFERENT ORIGINAL FORMS?
            (e.g., "saw"=past of "see" + noun "saw"톱, "left"=past of "leave" + adj "left"왼쪽, "rose"=past of "rise" + noun "rose"장미)
            **CRITICAL**: If input word IS the original form (e.g., "run", "book"), return SINGLE entry with variantTypes=[ORIGINAL_FORM]
            - "run" → Single entry: originalForm="run", variantTypes=[ORIGINAL_FORM] (DO NOT split into ORIGINAL_FORM and PAST_PARTICIPLE)
            - "books" → Single entry: originalForm="book", variantTypes=[PLURAL, THIRD_PERSON]
            → YES (different origins): Return array with separate entries | NO: Single-element array

            **CRITICAL MERGING RULE:**
            - If input word has SAME originalForm but DIFFERENT variantTypes, MERGE into ONE ENTRY with multiple variantTypes
            - Example "books":
              Single entry: originalForm="book", variantTypes=[PLURAL, THIRD_PERSON], meanings include BOTH noun meanings AND verb meanings
            - Each meaning should specify its partOfSpeech clearly

            STRUCTURE:
            **CRITICAL: Only variantTypes describes the INPUT word. Everything else (summary, meanings, examples) describes the ORIGINAL FORM.**

            1. sourceLanguageCode/targetLanguageCode: "EN", "KO", etc.
            2. originalForm: Base form (verbs→infinitive, adj→positive, nouns→singular)
            3. variantTypes: **ARRAY** of relationships between INPUT word and originalForm
               - Values: ORIGINAL_FORM|PAST_TENSE|PAST_PARTICIPLE|PRESENT_PARTICIPLE|THIRD_PERSON|COMPARATIVE|SUPERLATIVE|PLURAL|UNDEFINED
               - If input="ran" and originalForm="run", then variantTypes=[PAST_TENSE]
               - If input="books" and originalForm="book", then variantTypes=[PLURAL, THIRD_PERSON] (both noun plural AND verb 3rd person)
            4. summary: Max 3 common translations of the ORIGINAL FORM
               - Input "ran" → summary of "run": ["달리다","운영하다"]
               - Input "prettiest" → summary of "pretty": ["예쁜","아름다운"]
            5. meanings: All meanings describe the ORIGINAL FORM (not the input word)
               - Max 15 objects (common→rare, omit obscure ones)
               - partOfSpeech: verb, noun, adjective, adverb, etc.
               - meaning: Detailed explanation in target language
               - example: **CRITICAL RULES:**
                 1. ALWAYS use the ORIGINAL FORM in the example (NOT the input word!)
                    - If originalForm="book" (input was "books"), use "book" in example
                    - If originalForm="run" (input was "ran"), use "run" in example
                 2. **PART OF SPEECH MUST MATCH**: The word in the example MUST be used as the specified partOfSpeech
                    - If partOfSpeech="noun", the word must function as a noun in the example
                    - If partOfSpeech="verb", the word must function as a verb in the example
                    - WRONG: partOfSpeech="noun" but example has "I need to book a flight" (book is verb here)
                    - CORRECT: partOfSpeech="noun" and example has "I love reading a book" (book is noun here)
                 3. Grammar: Ensure grammatically correct sentences (e.g., "I/You/We/They run" ✓, "She runs" ✓)
                 4. QUALITY: Natural, practical sentences used in real-life contexts
                 5. CLARITY: Sentence must clearly demonstrate the word's meaning
                 6. LENGTH: 5-12 words (not too short, not too long)
                 7. AVOID: Generic phrases like "I need...", "This is...", "It is..." - be creative!
                 GOOD: "I love reading a good book." (book as noun, matches partOfSpeech)
                 GOOD: "We run a small bakery in downtown." (run as verb, matches partOfSpeech)
                 BAD: "I need to book a flight." (if partOfSpeech is noun - book is verb here!)
               - exampleTranslation: Translation in target language
            6. conjugations: (verbs only) present, past, pastParticiple, presentParticiple, thirdPerson
            7. comparatives: (adj only) positive, comparative, superlative
            8. plural: (nouns only) singular, plural

            EXAMPLE - "saw" homograph:
            [
              {{
                "originalForm": "see",
                "variantTypes": ["PAST_TENSE"],
                "sourceLanguageCode": "EN",
                "targetLanguageCode": "KO",
                "summary": ["보다", "알다"],
                "meanings": [
                  {{
                    "partOfSpeech": "verb",
                    "meaning": "시각적으로 인지하다",
                    "example": "We see the mountains clearly from our window.",
                    "exampleTranslation": "우리는 창문에서 산이 선명하게 보입니다."
                  }}
                ],
                "conjugations": {{"present": "see", "past": "saw", "pastParticiple": "seen", "presentParticiple": "seeing", "thirdPerson": "sees"}},
                "comparatives": null,
                "plural": null
              }},
              {{
                "originalForm": "saw",
                "variantTypes": ["ORIGINAL_FORM"],
                "sourceLanguageCode": "EN",
                "targetLanguageCode": "KO",
                "summary": ["톱"],
                "meanings": [
                  {{
                    "partOfSpeech": "noun",
                    "meaning": "톱 (자르는 도구)",
                    "example": "The carpenter used a saw to cut the wood.",
                    "exampleTranslation": "목수는 톱을 사용하여 나무를 잘랐습니다."
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

            ChatResponse chatResponse = ChatClient.create(chatModel)
                    .prompt(prompt)
                    .call()
                    .chatResponse();

            String response = chatResponse.getResult().getOutput().getText();

            // 토큰 사용량 및 비용 로깅
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                var usage = chatResponse.getMetadata().getUsage();
                long inputTokens = usage.getPromptTokens();
                long outputTokens = usage.getGenerationTokens();
                long totalTokens = usage.getTotalTokens();

                double inputCostUsd = (inputTokens / 1000.0) * 0.00017;
                double outputCostUsd = (outputTokens / 1000.0) * 0.000085;
                double totalCostUsd = inputCostUsd + outputCostUsd;

                // 환율: 1 USD = 1430 KRW
                double totalCostKrw = totalCostUsd * 1430;

                log.info("📊 Token Usage for word '{}': Input={}, Output={}, Total={}",
                    word, inputTokens, outputTokens, totalTokens);
                log.info("💰 Cost: ${} (₩{}) = Input: ${} + Output: ${}",
                    String.format("%.6f", totalCostUsd),
                    String.format("%.2f", totalCostKrw),
                    String.format("%.6f", inputCostUsd),
                    String.format("%.6f", outputCostUsd));
            }

            // 전체 응답은 debug 레벨로만 출력 (응답이 길어서 info 레벨에서는 제외)
            log.debug("AI Response for word '{}' (target: {}): {}", word, targetLanguage, response);

            WordAnalysisResult[] results = outputConverter.convert(response);

            // Validation 수행
            for (WordAnalysisResult result : results) {
                validateResult(result, word);
            }

            // 요약 정보 로깅
            if (results != null && results.length > 0) {
                String summary = Arrays.stream(results)
                    .map(r -> r.getOriginalForm() + " (" + String.join(", ", r.getVariantTypes().stream()
                        .map(Enum::name).toArray(String[]::new)) + ")")
                    .collect(Collectors.joining(", "));
                log.info("✅ AI analysis completed for '{}': {} result(s) - {}", word, results.length, summary);
            } else {
                log.info("✅ AI analysis completed for '{}': No results (invalid word)", word);
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
