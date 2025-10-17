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

import java.util.*;
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
            **CRITICAL: ALL fields (summary, meaning, example, exampleTranslation) MUST have meaningful content. NEVER leave empty strings.**
            **If you cannot provide meaningful content, return [] instead.**

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

            // 같은 originalForm을 가진 결과를 병합 (AI가 잘못 분리한 경우 대비)
            List<WordAnalysisResult> mergedResults = mergeDuplicateOriginalForms(results, word);

            // 요약 정보 로깅
            if (!mergedResults.isEmpty()) {
                String summary = mergedResults.stream()
                    .map(r -> r.getOriginalForm() + " (" + String.join(", ", r.getVariantTypes().stream()
                        .map(Enum::name).toArray(String[]::new)) + ")")
                    .collect(Collectors.joining(", "));
                log.info("✅ AI analysis completed for '{}': {} result(s) - {}", word, mergedResults.size(), summary);
            } else {
                log.info("✅ AI analysis completed for '{}': No results (invalid word)", word);
            }

            return mergedResults;
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

    /**
     * 같은 originalForm을 가진 결과들을 하나로 병합
     * AI가 프롬프트를 무시하고 같은 원형에 대해 여러 항목을 반환한 경우 처리
     */
    private List<WordAnalysisResult> mergeDuplicateOriginalForms(WordAnalysisResult[] results, String word) {
        if (results == null || results.length == 0) {
            return List.of();
        }

        // originalForm 기준으로 그룹화
        Map<String, List<WordAnalysisResult>> groupedByOriginalForm = Arrays.stream(results)
                .collect(Collectors.groupingBy(WordAnalysisResult::getOriginalForm));

        List<WordAnalysisResult> mergedList = new ArrayList<>();

        for (Map.Entry<String, List<WordAnalysisResult>> entry : groupedByOriginalForm.entrySet()) {
            List<WordAnalysisResult> group = entry.getValue();

            if (group.size() == 1) {
                // 중복 없음 - 그대로 추가
                mergedList.add(group.get(0));
            } else {
                // 중복 발견 - 병합 필요
                log.warn("Merging {} duplicate entries for originalForm '{}' (input word: '{}')",
                        group.size(), entry.getKey(), word);

                WordAnalysisResult merged = mergeResults(group);
                mergedList.add(merged);
            }
        }

        return mergedList;
    }

    /**
     * 같은 originalForm을 가진 여러 결과를 하나로 병합
     */
    private WordAnalysisResult mergeResults(List<WordAnalysisResult> results) {
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Cannot merge empty results");
        }

        WordAnalysisResult first = results.get(0);

        // variantTypes 병합 (중복 제거)
        List<com.linglevel.api.word.dto.VariantType> mergedVariantTypes = results.stream()
                .flatMap(r -> r.getVariantTypes().stream())
                .distinct()
                .collect(Collectors.toList());

        // meanings 병합 (중복 제거 - partOfSpeech와 meaning이 같은 것은 제외)
        List<com.linglevel.api.word.dto.Meaning> mergedMeanings = results.stream()
                .flatMap(r -> r.getMeanings().stream())
                .collect(Collectors.toMap(
                        m -> m.getPartOfSpeech() + ":" + m.getMeaning(),
                        m -> m,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());

        // 첫 번째 결과를 기반으로 병합된 결과 생성
        return WordAnalysisResult.builder()
                .sourceLanguageCode(first.getSourceLanguageCode())
                .targetLanguageCode(first.getTargetLanguageCode())
                .originalForm(first.getOriginalForm())
                .variantTypes(mergedVariantTypes)
                .summary(first.getSummary()) // 첫 번째 것 사용
                .meanings(mergedMeanings)
                .conjugations(first.getConjugations()) // 첫 번째 것 사용
                .comparatives(first.getComparatives()) // 첫 번째 것 사용
                .plural(first.getPlural()) // 첫 번째 것 사용
                .build();
    }
}
