package com.linglevel.api.word.entity;

import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.Meaning;
import com.linglevel.api.word.dto.RelatedForms;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * 단어 엔티티 (원형 단어만 저장)
 * 변형 형태는 WordVariant에 별도 저장
 *
 * 같은 단어도 언어 쌍별로 여러 개 저장 가능
 * 예: "run" EN->KO, "run" EN->JA는 별도 문서
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "words")
@CompoundIndex(
    name = "word_language_pair_idx",
    def = "{'word': 1, 'sourceLanguageCode': 1, 'targetLanguageCode': 1}",
    unique = true
)
public class Word {
    @Id
    private String id;

    /**
     * 원형 단어 (예: "pretty", "see", "run")
     * 복합 unique index의 일부 (word + sourceLanguageCode + targetLanguageCode)
     */
    private String word;

    /**
     * 원본 언어 코드
     */
    private LanguageCode sourceLanguageCode;

    /**
     * 번역 대상 언어 코드
     */
    private LanguageCode targetLanguageCode;

    /**
     * 자주 쓰이는 뜻 3개 요약 (대상 언어)
     */
    private List<String> summary;

    /**
     * 품사별 의미 목록
     * AI로부터 받은 Meaning을 그대로 저장
     */
    private List<Meaning> meanings;

    /**
     * 관련 변형 형태들 (동사 활용형, 비교급, 복수형 등)
     */
    private RelatedForms relatedForms;

    @Builder.Default
    private Boolean isEssential = false;
}