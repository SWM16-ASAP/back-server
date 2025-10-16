package com.linglevel.api.word.service;

import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.EssentialWordsStatsResponse;
import com.linglevel.api.word.dto.Oxford3000InitResponse;
import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.entity.Word;
import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import com.linglevel.api.word.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class Oxford3000Service {

    private final WordRepository wordRepository;
    private final WordService wordService;
    private final WordAiService wordAiService;
    private final com.linglevel.api.word.validator.WordValidator wordValidator;

    private static final String OXFORD3000_CSV_PATH = "data/oxford3000_final_cleaned.csv";
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Oxford 3000 단어를 초기화합니다.
     *
     * @param targetLanguage 번역 대상 언어
     * @param overwrite true: 기존 데이터 삭제 후 재생성, false: 기존 데이터 유지 + isEssential 업데이트
     * @return Oxford3000InitResponse
     */
    @Transactional
    public Oxford3000InitResponse initializeOxford3000(LanguageCode targetLanguage, boolean overwrite) {
        return initializeOxford3000(targetLanguage, overwrite, null);
    }

    /**
     * Oxford 3000 단어를 초기화합니다 (제한된 개수로 테스트 가능)
     *
     * @param targetLanguage 번역 대상 언어
     * @param overwrite true: 기존 데이터 삭제 후 재생성, false: 기존 데이터 유지 + isEssential 업데이트
     * @param limit 처리할 최대 단어 수 (null이면 전체)
     * @return Oxford3000InitResponse
     */
    @Transactional
    public Oxford3000InitResponse initializeOxford3000(LanguageCode targetLanguage, boolean overwrite, Integer limit) {
        LocalDateTime startedAt = LocalDateTime.now();
        log.info("Starting Oxford 3000 initialization: targetLanguage={}, overwrite={}, limit={}",
                 targetLanguage, overwrite, limit);

        // 1. CSV 파일에서 단어 읽기
        List<String> words = readOxford3000Words();

        // limit이 지정되면 해당 개수만 처리
        if (limit != null && limit > 0) {
            words = words.stream().limit(limit).collect(Collectors.toList());
            log.info("Limited to {} words for testing", words.size());
        }

        log.info("Loaded {} words from Oxford 3000 CSV", words.size());

        // 2. 통계 정보
        int successCount = 0;
        int failureCount = 0;
        int alreadyExistCount = 0;
        int newlyCreatedCount = 0;
        List<String> failedWords = new ArrayList<>();

        // 3. 각 단어 처리 (최대 3번 재시도)
        for (String word : words) {
            boolean processed = false;
            Exception lastException = null;

            for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
                try {
                    boolean existed = processWord(word, targetLanguage, overwrite);
                    successCount++;

                    if (existed) {
                        alreadyExistCount++;
                    } else {
                        newlyCreatedCount++;
                    }

                    processed = true;

                    // 진행 상황 로깅 (100개마다)
                    if (successCount % 100 == 0) {
                        log.info("Progress: {}/{} words processed", successCount, words.size());
                    }

                    break; // 성공하면 재시도 중단

                } catch (Exception e) {
                    lastException = e;
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        log.warn("Failed to process word '{}' (attempt {}/{}): {}. Retrying...",
                                 word, attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                    } else {
                        log.error("Failed to process word '{}' after {} attempts: {}",
                                  word, MAX_RETRY_ATTEMPTS, e.getMessage(), e);
                    }
                }
            }

            // 모든 재시도 실패
            if (!processed) {
                failureCount++;
                failedWords.add(word);
            }
        }

        LocalDateTime completedAt = LocalDateTime.now();
        log.info("Oxford 3000 initialization completed: success={}, failure={}, alreadyExist={}, newlyCreated={}",
                 successCount, failureCount, alreadyExistCount, newlyCreatedCount);

        return Oxford3000InitResponse.builder()
                .startedAt(startedAt)
                .completedAt(completedAt)
                .totalWords(words.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .alreadyExistCount(alreadyExistCount)
                .newlyCreatedCount(newlyCreatedCount)
                .failedWords(failedWords)
                .message(String.format("Successfully processed %d/%d words", successCount, words.size()))
                .build();
    }

    /**
     * 개별 단어 처리
     *
     * @param word 처리할 단어
     * @param targetLanguage 번역 대상 언어
     * @param overwrite 덮어쓰기 여부
     * @return true: 이미 존재했던 단어, false: 새로 생성된 단어
     */
    @Transactional
    public boolean processWord(String word, LanguageCode targetLanguage, boolean overwrite) {
        // WordValidator를 통한 전처리 및 검증
        String validatedWord = wordValidator.validateAndPreprocess(word);
        log.debug("Word validated and preprocessed: '{}' -> '{}'", word, validatedWord);

        if (overwrite) {
            // 덮어쓰기 모드: 기존 WordVariant와 Word 삭제
            List<com.linglevel.api.word.entity.WordVariant> existingVariants =
                    wordService.getOrCreateWordEntities(validatedWord, targetLanguage);

            for (com.linglevel.api.word.entity.WordVariant variant : existingVariants) {
                String originalForm = variant.getOriginalForm();
                wordRepository.findByWordAndSourceLanguageCodeAndTargetLanguageCode(
                        originalForm, LanguageCode.EN, targetLanguage
                ).ifPresent(wordRepository::delete);
            }
        }

        // WordService의 로직 그대로 사용
        List<com.linglevel.api.word.entity.WordVariant> wordVariants =
                wordService.getOrCreateWordEntities(validatedWord, targetLanguage);

        // 생성된 모든 Word에 isEssential=true 설정
        for (com.linglevel.api.word.entity.WordVariant variant : wordVariants) {
            String originalForm = variant.getOriginalForm();
            Optional<Word> savedWord = wordRepository.findByWordAndSourceLanguageCodeAndTargetLanguageCode(
                    originalForm, LanguageCode.EN, targetLanguage
            );

            savedWord.ifPresent(w -> {
                if (!Boolean.TRUE.equals(w.getIsEssential())) {
                    w.setIsEssential(true);
                    wordRepository.save(w);
                    log.info("Set isEssential=true for Oxford 3000 word: {}", w.getWord());
                }
            });
        }

        return !wordVariants.isEmpty();
    }

    /**
     * CSV 파일에서 Oxford 3000 단어 목록 읽기
     */
    private List<String> readOxford3000Words() {
        try {
            ClassPathResource resource = new ClassPathResource(OXFORD3000_CSV_PATH);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                return reader.lines()
                        .skip(1) // 헤더 스킵 ("word")
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            log.error("Failed to read Oxford 3000 CSV file: {}", e.getMessage(), e);
            throw new WordsException(WordsErrorCode.WORD_NOT_FOUND);
        }
    }

    /**
     * 필수 단어 통계 조회
     */
    public EssentialWordsStatsResponse getEssentialWordsStats() {
        // 전체 필수 단어 수
        long totalEssential = wordRepository.countByIsEssential(true);

        // 언어별 통계 (간단한 구현 - 실제로는 aggregation 사용 권장)
        List<Word> allEssentialWords = wordRepository.findAllByIsEssential(true);

        Map<LanguageCode, Long> countByTarget = allEssentialWords.stream()
                .collect(Collectors.groupingBy(Word::getTargetLanguageCode, Collectors.counting()));

        Map<LanguageCode, Long> countBySource = allEssentialWords.stream()
                .collect(Collectors.groupingBy(Word::getSourceLanguageCode, Collectors.counting()));

        return EssentialWordsStatsResponse.builder()
                .totalEssentialWords(totalEssential)
                .countByTargetLanguage(countByTarget)
                .countBySourceLanguage(countBySource)
                .build();
    }

    /**
     * 특정 단어의 isEssential 상태 업데이트
     */
    @Transactional
    public void updateEssentialStatus(String wordId, boolean isEssential) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new WordsException(WordsErrorCode.WORD_NOT_FOUND));

        word.setIsEssential(isEssential);
        wordRepository.save(word);

        log.info("Updated word '{}' isEssential={}", word.getWord(), isEssential);
    }
}
