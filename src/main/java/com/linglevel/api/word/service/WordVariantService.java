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

import java.util.List;
import java.util.Optional;

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
     * 단어의 원형을 반환 (언어 중립적)
     *
     * @param word 검색할 단어 (변형 형태 가능)
     * @return 원형 단어
     */
    @Transactional
    public String getOriginalForm(String word) {
        WordVariant variant = getOrCreateWordVariant(word);
        return variant.getOriginalForm();
    }

    /**
     * WordVariant 조회 또는 생성
     * DB에 없으면 AI로 분석하여 원형 찾기
     *
     * @param word 검색할 단어
     * @return WordVariant
     */
    @Transactional
    public WordVariant getOrCreateWordVariant(String word) {
        // 1. WordVariant에서 검색
        Optional<WordVariant> variantOpt = wordVariantRepository.findByWord(word);
        if (variantOpt.isPresent()) {
            return variantOpt.get();
        }

        // 2. DB에 없으면 AI 호출 (기본 언어 KO 사용 - 언어는 중요하지 않음, 원형만 필요)
        log.info("WordVariant '{}' not found. Calling AI to find original form...", word);
        List<WordAnalysisResult> analysisResults = wordAiService.analyzeWord(word, LanguageCode.KO.getCode());

        if (analysisResults.isEmpty()) {
            log.warn("AI could not find original form for word '{}'", word);
            throw new WordsException(WordsErrorCode.WORD_IS_MEANINGLESS);
        }
        // 3. 첫 번째 결과로 WordVariant 생성 및 저장
        WordAnalysisResult result = analysisResults.get(0);
        WordVariant newVariant = WordVariant.builder()
                .word(word)
                .originalForm(result.getOriginalForm())
                .variantTypes(result.getVariantTypes())
                .build();

        WordVariant savedVariant = wordVariantRepository.save(newVariant);
        log.info("Saved new WordVariant: {} -> {} ({})", word, result.getOriginalForm(), result.getVariantTypes());

        return savedVariant;
    }

    /**
     * 단어가 WordVariant에 존재하는지 확인
     */
    public boolean exists(String word) {
        return wordVariantRepository.findByWord(word).isPresent();
    }
}
