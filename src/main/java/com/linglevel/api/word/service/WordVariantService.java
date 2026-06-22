package com.linglevel.api.word.service;

import com.linglevel.api.word.entity.WordVariant;
import com.linglevel.api.word.repository.WordVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    /**
     * 단어의 원형 후보들을 반환 (언어 중립적)
     *
     * @param word 검색할 단어 (변형 형태 가능)
     * @return 원형 단어 후보 목록
     */
    public List<String> getOriginalForms(String word) {
        return wordVariantRepository.findAllByWord(word).stream()
                .map(WordVariant::getOriginalForm)
                .distinct()
                .toList();
    }
}
