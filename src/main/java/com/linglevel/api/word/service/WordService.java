package com.linglevel.api.word.service;

import com.linglevel.api.bookmark.repository.WordBookmarkRepository;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.*;
import com.linglevel.api.word.entity.InvalidWord;
import com.linglevel.api.word.entity.Word;
import com.linglevel.api.word.entity.WordVariant;
import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import com.linglevel.api.word.repository.InvalidWordRepository;
import com.linglevel.api.word.repository.WordRepository;
import com.linglevel.api.word.repository.WordVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordService {

    private final WordRepository wordRepository;
    private final WordBookmarkRepository wordBookmarkRepository;
    private final WordVariantRepository wordVariantRepository;
    private final InvalidWordRepository invalidWordRepository;
    private final WordAiService wordAiService;

    public WordSearchResponse getOrCreateWords(String userId, String word, LanguageCode targetLanguage) {
        List<WordVariant> wordVariants = getOrCreateWordEntities(word, targetLanguage);

        // 각 원형에 대한 WordResponse 생성
        List<WordResponse> results = new ArrayList<>();

        for (WordVariant wordVariant : wordVariants) {
            // 원형 단어를 targetLanguage로 번역된 것 가져오기
            Word originalWord = wordRepository.findByWordAndTargetLanguageCode(
                    wordVariant.getOriginalForm(),
                    targetLanguage
            ).orElseGet(() -> {
                // 해당 언어로 번역된 Word가 없으면 AI로 새로 생성
                log.info("Word '{}' not found for targetLanguage {}, creating new one...",
                    wordVariant.getOriginalForm(), targetLanguage);

                List<WordAnalysisResult> analysisResults = wordAiService.analyzeWord(
                    wordVariant.getOriginalForm(),
                    targetLanguage.getCode()
                );

                // Word 생성 및 저장 (빈 결과는 WordAiService에서 예외 발생)
                Word newWord = convertAnalysisResultToWord(analysisResults.get(0));
                return wordRepository.save(newWord);
            });

            boolean isBookmarked = wordBookmarkRepository.existsByUserIdAndWord(userId, wordVariant.getOriginalForm());

            WordResponse response = convertToResponse(
                originalWord,
                isBookmarked,
                wordVariant.getVariantTypes(),
                wordVariant.getOriginalForm()
            );

            results.add(response);
        }

        return WordSearchResponse.builder()
                .searchedWord(word)
                .results(results)
                .build();
    }

    @Transactional
    public List<WordVariant> getOrCreateWordEntities(String word, LanguageCode targetLanguage) {
        // 1. WordVariant에서 검색 (변형 형태인지 확인)
        List<WordVariant> existingVariants = wordVariantRepository.findAllByWord(word);
        if (!existingVariants.isEmpty()) {
            log.info("Found {} existing variants for word '{}'", existingVariants.size(), word);
            return existingVariants;
        }

        // 2. InvalidWord 캐시 확인 - 3회 유예 후 차단
        Optional<InvalidWord> cachedInvalidWord = invalidWordRepository.findByWord(word);
        if (cachedInvalidWord.isPresent()) {
            InvalidWord invalidWord = cachedInvalidWord.get();
            if (invalidWord.getAttemptCount() >= 3) {
                log.info("Word '{}' permanently blocked after {} failed attempts", word, invalidWord.getAttemptCount());
                throw new WordsException(WordsErrorCode.WORD_IS_MEANINGLESS);
            }
            log.info("Word '{}' found in cache with {} attempts. Allowing retry (attempt {}/3)",
                word, invalidWord.getAttemptCount(), invalidWord.getAttemptCount() + 1);
        }

        // 3. DB에 없으면 AI 호출 (실패 시에도 InvalidWord로 캐싱)
        log.info("Word '{}' not found in database. Calling AI to analyze...", word);
        List<WordAnalysisResult> analysisResults;
        try {
            analysisResults = wordAiService.analyzeWord(word, targetLanguage.getCode());

            // AI 호출 성공 시 InvalidWord 캐시에서 제거 (일시적 오류였던 경우 복구)
            cachedInvalidWord.ifPresent(invalidWord -> {
                invalidWordRepository.delete(invalidWord);
                log.info("Removed word '{}' from invalid word cache after successful AI analysis (was attempt {}/3)",
                    word, invalidWord.getAttemptCount());
            });

        } catch (Exception e) {
            // AI 호출 실패 또는 무의미한 단어인 경우 InvalidWord로 캐싱
            log.warn("AI call failed for word '{}'. Caching as invalid word to prevent retries.", word, e);
            saveInvalidWord(word);
            throw new WordsException(WordsErrorCode.WORD_IS_MEANINGLESS);
        }

        // 4. 트랜잭션 내에서 DB 저장 처리
        List<WordVariant> savedVariants = new ArrayList<>();
        for (WordAnalysisResult analysisResult : analysisResults) {
            WordVariant savedVariant = saveWordFromAnalysis(word, analysisResult);
            savedVariants.add(savedVariant);
        }

        return savedVariants;
    }


    @Transactional
    public WordVariant saveWordFromAnalysis(String word, WordAnalysisResult analysisResult) {
        // 1. 원형 단어가 해당 언어 쌍으로 이미 존재하는지 확인
        String originalForm = analysisResult.getOriginalForm();
        LanguageCode sourceLanguageCode = analysisResult.getSourceLanguageCode();
        LanguageCode targetLanguageCode = analysisResult.getTargetLanguageCode();

        wordRepository.findByWordAndSourceLanguageCodeAndTargetLanguageCode(
                originalForm, sourceLanguageCode, targetLanguageCode
        ).orElseGet(() -> {
            // 해당 언어 쌍으로 번역된 Word가 없으면 새로 저장
            Word newWord = convertAnalysisResultToWord(analysisResult);
            Word savedWord = wordRepository.save(newWord);
            log.info("Saved new word: {} ({} -> {})", originalForm, sourceLanguageCode, targetLanguageCode);

            // 변형 형태들을 WordVariant에 저장 (언어 중립적)
            saveWordVariants(savedWord);

            return savedWord;
        });

        // 2. 입력 단어를 WordVariant에 저장 (언어 중립적, 중복 체크)
        // 중요: word와 originalForm 둘 다 체크해야 함 (homograph 대응)
        Optional<WordVariant> existingVariant = wordVariantRepository.findByWordAndOriginalForm(word, originalForm);
        if (existingVariant.isPresent()) {
            log.info("Variant already exists: {} -> {}", word, originalForm);
            return existingVariant.get();
        }

        List<VariantType> variantTypes = analysisResult.getVariantTypes() != null && !analysisResult.getVariantTypes().isEmpty()
                ? analysisResult.getVariantTypes()
                : List.of(VariantType.ORIGINAL_FORM);

        WordVariant inputVariant = createVariant(word, originalForm, variantTypes);
        wordVariantRepository.save(inputVariant);
        log.info("Saved input variant: {} -> {} ({})", word, originalForm, variantTypes);

        return inputVariant;
    }

    private Word convertAnalysisResultToWord(WordAnalysisResult result) {
        // RelatedForms 구성
        RelatedForms relatedForms = RelatedForms.builder()
                .conjugations(result.getConjugations())
                .comparatives(result.getComparatives())
                .plural(result.getPlural())
                .build();

        return Word.builder()
                .word(result.getOriginalForm())
                .sourceLanguageCode(result.getSourceLanguageCode())
                .targetLanguageCode(result.getTargetLanguageCode())
                .summary(result.getSummary())
                .meanings(result.getMeanings())  // AI의 Meaning을 그대로 저장
                .relatedForms(relatedForms)
                .build();
    }

    @Transactional
    public void saveWordVariants(Word word) {
        List<WordVariant> variants = new ArrayList<>();

        RelatedForms relatedForms = word.getRelatedForms();
        if (relatedForms == null) {
            return;
        }

        // 동사 변형 저장
        if (relatedForms.getConjugations() != null) {
            var conj = relatedForms.getConjugations();
            if (conj.getPast() != null && !conj.getPast().equals(word.getWord())) {
                variants.add(createVariant(conj.getPast(), word.getWord(), List.of(VariantType.PAST_TENSE)));
            }
            if (conj.getPastParticiple() != null && !conj.getPastParticiple().equals(word.getWord())) {
                variants.add(createVariant(conj.getPastParticiple(), word.getWord(), List.of(VariantType.PAST_PARTICIPLE)));
            }
            if (conj.getPresentParticiple() != null && !conj.getPresentParticiple().equals(word.getWord())) {
                variants.add(createVariant(conj.getPresentParticiple(), word.getWord(), List.of(VariantType.PRESENT_PARTICIPLE)));
            }
            if (conj.getThirdPerson() != null && !conj.getThirdPerson().equals(word.getWord())) {
                variants.add(createVariant(conj.getThirdPerson(), word.getWord(), List.of(VariantType.THIRD_PERSON)));
            }
        }

        // 형용사/부사 변형 저장
        if (relatedForms.getComparatives() != null) {
            var comp = relatedForms.getComparatives();
            if (comp.getComparative() != null && !comp.getComparative().equals(word.getWord())) {
                variants.add(createVariant(comp.getComparative(), word.getWord(), List.of(VariantType.COMPARATIVE)));
            }
            if (comp.getSuperlative() != null && !comp.getSuperlative().equals(word.getWord())) {
                variants.add(createVariant(comp.getSuperlative(), word.getWord(), List.of(VariantType.SUPERLATIVE)));
            }
        }

        // 명사 복수형 저장
        if (relatedForms.getPlural() != null) {
            var plural = relatedForms.getPlural();
            if (plural.getPlural() != null && !plural.getPlural().equals(word.getWord())) {
                variants.add(createVariant(plural.getPlural(), word.getWord(), List.of(VariantType.PLURAL)));
            }
        }

        // N+1 문제 해결: 한 번에 조회 후 필터링
        if (!variants.isEmpty()) {
            // 중복 제거 및 variantTypes 병합: 같은 단어가 여러 번 추가되는 경우 variantTypes를 합침
            List<WordVariant> uniqueVariants = variants.stream()
                    .collect(Collectors.toMap(
                            WordVariant::getWord,
                            variant -> variant,
                            (existing, replacement) -> {
                                // 같은 단어인 경우 variantTypes를 병합
                                List<VariantType> mergedTypes = new ArrayList<>(existing.getVariantTypes());
                                replacement.getVariantTypes().forEach(type -> {
                                    if (!mergedTypes.contains(type)) {
                                        mergedTypes.add(type);
                                    }
                                });
                                existing.setVariantTypes(mergedTypes);
                                return existing;
                            }
                    ))
                    .values()
                    .stream()
                    .collect(Collectors.toList());

            List<String> variantWords = uniqueVariants.stream()
                    .map(WordVariant::getWord)
                    .collect(Collectors.toList());

            // 이미 존재하는 variant들을 한 번의 쿼리로 조회
            List<WordVariant> existingVariants = wordVariantRepository.findByWordIn(variantWords);
            List<String> existingWords = existingVariants.stream()
                    .map(WordVariant::getWord)
                    .collect(Collectors.toList());

            // 새로운 variant만 필터링하여 배치 저장
            List<WordVariant> newVariants = uniqueVariants.stream()
                    .filter(variant -> !existingWords.contains(variant.getWord()))
                    .collect(Collectors.toList());

            if (!newVariants.isEmpty()) {
                wordVariantRepository.saveAll(newVariants);
                newVariants.forEach(variant ->
                    log.info("Saved variant: {} -> {} ({})", variant.getWord(), variant.getOriginalForm(), variant.getVariantTypes())
                );
            }
        }
    }

    private WordVariant createVariant(String variantWord, String originalForm, List<VariantType> types) {
        return WordVariant.builder()
                .word(variantWord)
                .originalForm(originalForm)
                .variantTypes(types)
                .build();
    }

    /**
     * 유효하지 않은 단어를 캐시에 저장 (AI 재호출 방지)
     */
    @Transactional
    public void saveInvalidWord(String word) {
        Optional<InvalidWord> existingInvalidWord = invalidWordRepository.findByWord(word);

        if (existingInvalidWord.isPresent()) {
            // 이미 존재하면 시도 횟수만 증가
            InvalidWord invalidWord = existingInvalidWord.get();
            invalidWord.setAttemptCount(invalidWord.getAttemptCount() + 1);
            invalidWordRepository.save(invalidWord);
            log.info("Updated invalid word '{}' attempt count: {}", word, invalidWord.getAttemptCount());
        } else {
            // 새로 저장
            LocalDateTime now = LocalDateTime.now();

            InvalidWord invalidWord = InvalidWord.builder()
                    .word(word)
                    .attemptedAt(now)
                    .attemptCount(1)
                    .build();
            invalidWordRepository.save(invalidWord);
            log.info("Cached invalid word '{}' permanently (attempt 1/3)", word);
        }
    }

    private WordResponse convertToResponse(Word word, boolean isBookmarked, List<VariantType> variantTypes, String originalForm) {
        return WordResponse.builder()
                .id(word.getId())
                .originalForm(originalForm)
                .variantTypes(variantTypes)
                .sourceLanguageCode(word.getSourceLanguageCode())
                .targetLanguageCode(word.getTargetLanguageCode())
                .summary(word.getSummary())
                .meanings(word.getMeanings())  // Meaning을 그대로 사용
                .relatedForms(word.getRelatedForms())
                .bookmarked(isBookmarked)
                .isEssential(word.getIsEssential())
                .build();
    }

    /**
     * 관리자 전용: 단어를 AI로 강제 재분석
     *
     * @param word 재분석할 단어
     * @param targetLanguage 번역 대상 언어
     * @param overwrite true: 기존 데이터 삭제 후 재생성, false: 기존 유지 + 새로운 의미 추가
     * @return WordSearchResponse
     */
    @Transactional
    public WordSearchResponse forceReanalyzeWord(String word, LanguageCode targetLanguage, boolean overwrite) {
        return forceReanalyzeWord(word, targetLanguage, overwrite, false);
    }

    /**
     * 관리자 전용: 단어를 AI로 강제 재분석 (Variant 삭제 옵션 포함)
     *
     * @param word 재분석할 단어
     * @param targetLanguage 번역 대상 언어
     * @param overwrite true: 기존 데이터 삭제 후 재생성, false: 기존 유지 + 새로운 의미 추가
     * @param deleteVariants true: Variant도 함께 삭제 (완전 초기화), false: Variant 유지 (기본값)
     * @return WordSearchResponse
     */
    @Transactional
    public WordSearchResponse forceReanalyzeWord(String word, LanguageCode targetLanguage, boolean overwrite, boolean deleteVariants) {
        log.info("Force re-analyzing word '{}' with targetLanguage={}, overwrite={}, deleteVariants={}",
                word, targetLanguage, overwrite, deleteVariants);

        if (overwrite) {
            List<WordVariant> existingVariants = wordVariantRepository.findAllByWord(word);
            if (!existingVariants.isEmpty()) {
                // 관련된 원형 단어들의 Word 엔티티 삭제
                for (WordVariant variant : existingVariants) {
                    String originalForm = variant.getOriginalForm();
                    wordRepository.findByWordAndTargetLanguageCode(
                            originalForm, targetLanguage
                    ).ifPresent(wordToDelete -> {
                        wordRepository.delete(wordToDelete);
                        log.info("Deleted Word: {} (targetLanguage={})", originalForm, targetLanguage);
                    });
                }

                if (deleteVariants) {
                    // Variant도 함께 삭제 (완전 초기화)
                    wordVariantRepository.deleteAll(existingVariants);
                    log.info("Deleted {} existing WordVariants for word '{}' (complete reset)", existingVariants.size(), word);
                } else {
                    // Variant는 유지 (AI가 기존 관계 + 새로운 homograph 추가 가능)
                    log.info("Kept {} existing WordVariants for word '{}' (only Word deleted)", existingVariants.size(), word);
                }
            }
        }

        // AI로 재분석
        log.info("Calling AI to re-analyze word '{}'...", word);
        List<WordAnalysisResult> analysisResults = wordAiService.analyzeWord(word, targetLanguage.getCode());

        // 분석 결과를 DB에 저장 (빈 결과는 WordAiService에서 예외 발생, overwrite=false면 중복 체크로 인해 새로운 것만 추가됨)
        List<WordVariant> savedVariants = new ArrayList<>();
        for (WordAnalysisResult analysisResult : analysisResults) {
            WordVariant savedVariant = saveWordFromAnalysis(word, analysisResult);
            savedVariants.add(savedVariant);
        }

        log.info("Force re-analysis completed. Saved {} variants", savedVariants.size());

        // 결과를 WordSearchResponse로 변환하여 반환
        // userId는 null로 전달 (어드민 API이므로 북마크 체크 불필요)
        List<WordResponse> results = new ArrayList<>();
        for (WordVariant wordVariant : savedVariants) {
            Word originalWord = wordRepository.findByWordAndTargetLanguageCode(
                    wordVariant.getOriginalForm(),
                    targetLanguage
            ).orElseThrow(() -> new WordsException(WordsErrorCode.WORD_NOT_FOUND));

            WordResponse response = convertToResponse(
                    originalWord,
                    false, // 어드민 API이므로 북마크 체크하지 않음
                    wordVariant.getVariantTypes(),
                    wordVariant.getOriginalForm()
            );
            results.add(response);
        }

        return WordSearchResponse.builder()
                .searchedWord(word)
                .results(results)
                .build();
    }
}