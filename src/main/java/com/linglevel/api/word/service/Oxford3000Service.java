package com.linglevel.api.word.service;

import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.EssentialWordsStatsResponse;
import com.linglevel.api.word.dto.Oxford3000InitResponse;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class Oxford3000Service {

    private final WordRepository wordRepository;
    private final WordService wordService;
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
        log.info("============================================================");
        log.info("Starting Oxford 3000 initialization");
        log.info("Target Language: {}, Overwrite: {}, Limit: {}", targetLanguage, overwrite, limit);
        log.info("============================================================");

        // 1. CSV 파일에서 단어 읽기
        List<String> csvWords = readOxford3000Words();
        log.info("Step 1: Loaded {} words from Oxford 3000 CSV", csvWords.size());

        // 2. 이미 등록된 essential 단어 목록 조회 (대소문자 무시)
        List<String> existingEssentialWords = getEssentialWordsList(targetLanguage);
        log.info("Step 2: Found {} already registered essential words", existingEssentialWords.size());

        // 3. 차집합 계산 (등록이 필요한 단어만 필터링)
        List<String> wordsToProcess = csvWords.stream()
                .filter(word -> !existingEssentialWords.contains(word.toLowerCase()))
                .collect(Collectors.toList());

        int skippedCount = csvWords.size() - wordsToProcess.size();
        log.info("Step 3: {} words already registered, {} words to process",
                 skippedCount, wordsToProcess.size());

        // limit이 지정되면 해당 개수만 처리
        if (limit != null && limit > 0 && wordsToProcess.size() > limit) {
            wordsToProcess = wordsToProcess.stream().limit(limit).collect(Collectors.toList());
            log.info("Step 4: Limited to {} words for testing", wordsToProcess.size());
        }

        log.info("============================================================");
        log.info("Processing {} words...", wordsToProcess.size());
        log.info("============================================================");

        // 4. 통계 정보
        int successCount = 0;
        int failureCount = 0;
        int newlyCreatedCount = 0;
        List<String> failedWords = new ArrayList<>();

        // 5. 각 단어 처리 (최대 3번 재시도)
        for (int i = 0; i < wordsToProcess.size(); i++) {
            String word = wordsToProcess.get(i);
            boolean processed = false;
            Exception lastException = null;

            for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
                try {
                    boolean existed = processWord(word, targetLanguage, overwrite);
                    successCount++;

                    if (!existed) {
                        newlyCreatedCount++;
                    }

                    processed = true;

                    // 진행 상황 로깅 (10개마다, 또는 100개마다)
                    if ((wordsToProcess.size() <= 50 && (i + 1) % 10 == 0) ||
                        (wordsToProcess.size() > 50 && (i + 1) % 100 == 0)) {
                        log.info("Progress: {}/{} words processed ({} succeeded, {} failed)",
                                 i + 1, wordsToProcess.size(), successCount, failureCount);
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
        long durationSeconds = java.time.Duration.between(startedAt, completedAt).getSeconds();

        log.info("============================================================");
        log.info("Oxford 3000 initialization completed!");
        log.info("Duration: {} seconds ({} minutes)", durationSeconds, durationSeconds / 60);
        log.info("Total CSV words: {}", csvWords.size());
        log.info("Already registered (skipped): {}", skippedCount);
        log.info("Attempted to process: {}", wordsToProcess.size());
        log.info("Successfully processed: {}", successCount);
        log.info("Newly created: {}", newlyCreatedCount);
        log.info("Failed: {}", failureCount);
        if (!failedWords.isEmpty()) {
            log.error("Failed words: {}", String.join(", ", failedWords));
        }
        log.info("============================================================");

        return Oxford3000InitResponse.builder()
                .startedAt(startedAt)
                .completedAt(completedAt)
                .totalWords(wordsToProcess.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .alreadyExistCount(skippedCount)
                .newlyCreatedCount(newlyCreatedCount)
                .failedWords(failedWords)
                .message(String.format("Processed %d words (%d skipped, %d succeeded, %d failed)",
                         wordsToProcess.size(), skippedCount, successCount, failureCount))
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

        // WordService의 forceReanalyzeWord 사용 (overwrite 로직 포함)
        com.linglevel.api.word.dto.WordSearchResponse response =
                wordService.forceReanalyzeWord(validatedWord, targetLanguage, overwrite);

        // 생성된 모든 Word에 isEssential=true 설정
        for (com.linglevel.api.word.dto.WordResponse wordResponse : response.getResults()) {
            wordRepository.findById(wordResponse.getId())
                .ifPresent(w -> {
                    if (!Boolean.TRUE.equals(w.getIsEssential())) {
                        w.setIsEssential(true);
                        wordRepository.save(w);
                        log.info("Set isEssential=true for Oxford 3000 word: {}", w.getWord());
                    }
                });
        }

        return !response.getResults().isEmpty();
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
     * 등록된 필수 단어 목록 조회 (word 문자열만)
     *
     * @param targetLanguage 번역 대상 언어 (null이면 전체)
     * @return 필수 단어 문자열 목록 (중복 제거)
     */
    public List<String> getEssentialWordsList(LanguageCode targetLanguage) {
        List<Word> essentialWords;

        if (targetLanguage != null) {
            essentialWords = wordRepository.findAllByIsEssentialAndTargetLanguageCode(true, targetLanguage);
            log.info("Found {} essential words for target language: {}", essentialWords.size(), targetLanguage);
        } else {
            essentialWords = wordRepository.findAllByIsEssential(true);
            log.info("Found {} total essential words (all languages)", essentialWords.size());
        }

        // word 문자열만 추출하고 중복 제거 (대소문자 구분 없이)
        List<String> wordList = essentialWords.stream()
                .map(Word::getWord)
                .map(String::toLowerCase)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        log.info("Returning {} unique essential word strings", wordList.size());
        return wordList;
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
