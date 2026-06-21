package com.linglevel.api.word.service;

import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.entity.WordVariant;
import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import com.linglevel.api.word.repository.WordVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * WordVariant 전용 서비스 (언어 중립적)
 *
 * 북마크, 원형 조회 등 번역이 필요없는 작업에 사용
 * - WordVariant는 "변형 → 원형" 매핑만 저장 (언어 정보 없음)
 * - 예: "ran" → "run", "prettiest" → "pretty"
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WordVariantService {

    private final WordVariantRepository wordVariantRepository;
    private final WordAiService wordAiService;

    /**
     * 단어의 원형 후보들을 반환 (언어 중립적)
     *
     * @param word 검색할 단어 (변형 형태 가능)
     * @return 원형 단어 후보 목록
     */
    @Transactional
    public List<String> getOriginalForms(String word) {
        return getOrCreateWordVariants(word).stream()
                .map(WordVariant::getOriginalForm)
                .distinct()
                .toList();
    }

    /**
     * WordVariant 조회 또는 생성
     * DB에 없으면 AI로 분석하여 가능한 원형들을 찾기
     *
     * @param word 검색할 단어
     * @return WordVariant 목록
     */
    @Transactional
    public List<WordVariant> getOrCreateWordVariants(String word) {
        // 1. WordVariant에서 검색
        List<WordVariant> existingVariants = wordVariantRepository.findAllByWord(word);
        if (!existingVariants.isEmpty()) {
            return existingVariants;
        }

        // 2. DB에 없으면 AI 호출 (기본 언어 KO 사용 - 언어는 중요하지 않음, 원형 후보만 필요)
        log.info("WordVariant '{}' not found. Calling AI to find original form...", word);
        List<WordAnalysisResult> analysisResults = wordAiService.analyzeWord(word, LanguageCode.KO.getCode());

        if (analysisResults.isEmpty()) {
            log.warn("AI could not find original form for word '{}'", word);
            throw new WordsException(WordsErrorCode.WORD_IS_MEANINGLESS);
        }
        // 3. 분석 결과의 원형 후보들을 모두 저장한다. 같은 원형이 중복되면 첫 결과를 유지한다.
        List<WordVariant> newVariants = new ArrayList<>();
        for (WordAnalysisResult result : analysisResults) {
            boolean alreadyAdded = newVariants.stream()
                    .anyMatch(variant -> variant.getOriginalForm().equals(result.getOriginalForm()));
            if (alreadyAdded) {
                continue;
            }

            newVariants.add(WordVariant.builder()
                    .word(word)
                    .originalForm(result.getOriginalForm())
                    .variantTypes(result.getVariantTypes())
                    .build());
        }

        List<WordVariant> savedVariants = wordVariantRepository.saveAll(newVariants);
        savedVariants.forEach(variant ->
                log.info("Saved new WordVariant: {} -> {} ({})",
                        word, variant.getOriginalForm(), variant.getVariantTypes())
        );

        return savedVariants;
    }

}
