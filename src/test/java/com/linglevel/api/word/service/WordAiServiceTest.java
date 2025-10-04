package com.linglevel.api.word.service;

import com.linglevel.api.word.dto.WordAnalysisResult;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WordAiService 통합 테스트
 * 실제 AI 호출을 테스트합니다.
 * 
 * 주의: 이 테스트는 실제 AWS Bedrock API를 호출하므로 비용이 발생할 수 있습니다.
 * application-test.properties에 AWS 자격증명이 설정되어 있어야 합니다.
 */
@SpringBootTest
@ActiveProfiles("local")
class WordAiServiceTest {

    private static final Logger log = LoggerFactory.getLogger(WordAiServiceTest.class);

    @Autowired
    private WordAiService wordAiService;

    @Test
    void 단어_분석_테스트_동사() {
        // given
        String word = "buying";

        // when
        WordAnalysisResult result = wordAiService.analyzeWord(word);

        // then
        log.info("분석 결과: {}", result);

        assertThat(result).isNotNull();
        assertThat(result.getOriginalForm()).isEqualTo("buy");
        assertThat(result.getVariantType()).isNotNull();
        assertThat(result.getPartOfSpeech()).isNotNull().isNotEmpty();
        assertThat(result.getMeaningsKo()).isNotEmpty();
        assertThat(result.getMeaningsJa()).isNotEmpty();
        assertThat(result.getExamples()).isNotEmpty();

        // 동사이므로 conjugations가 있어야 함
        assertThat(result.getConjugations()).isNotNull();
        assertThat(result.getConjugations().getPast()).isNotNull();

        log.info("원형: {}", result.getOriginalForm());
        log.info("변형 타입: {}", result.getVariantType());
        log.info("품사: {}", result.getPartOfSpeech());
        log.info("과거형: {}", result.getConjugations().getPast());
        log.info("한국어 뜻: {}", result.getMeaningsKo());
        log.info("일본어 뜻: {}", result.getMeaningsJa());
        log.info("예문: {}", result.getExamples());
    }

    //@Test
    void 단어_분석_테스트_형용사() {
        // given
        String word = "beautiful";

        // when
        WordAnalysisResult result = wordAiService.analyzeWord(word);

        // then
        log.info("분석 결과: {}", result);

        assertThat(result).isNotNull();
        assertThat(result.getOriginalForm()).isEqualTo(word);
        assertThat(result.getVariantType()).isNotNull();
        assertThat(result.getPartOfSpeech()).isNotNull().isNotEmpty();
        assertThat(result.getMeaningsKo()).isNotEmpty();

        // 형용사이므로 comparatives가 있어야 함
        assertThat(result.getComparatives()).isNotNull();

        log.info("원형: {}", result.getOriginalForm());
        log.info("변형 타입: {}", result.getVariantType());
        log.info("품사: {}", result.getPartOfSpeech());
        if (result.getComparatives().getComparative() != null) {
            log.info("비교급: {}", result.getComparatives().getComparative());
            log.info("최상급: {}", result.getComparatives().getSuperlative());
        }
        log.info("한국어 뜻: {}", result.getMeaningsKo());
    }

    //@Test
    void 단어_분석_테스트_명사_소유격() {
        // given
        String word = "bank's";

        // when
        WordAnalysisResult result = wordAiService.analyzeWord(word);

        // then
        log.info("분석 결과: {}", result);

        assertThat(result).isNotNull();
        assertThat(result.getOriginalForm()).isEqualTo("bank");
        assertThat(result.getVariantType()).isNotNull();
        assertThat(result.getPartOfSpeech()).isNotNull().isNotEmpty();
        assertThat(result.getMeaningsKo()).isNotEmpty();

        // 명사이므로 plural이 있어야 함
        assertThat(result.getPlural()).isNotNull();
        assertThat(result.getPlural().getPlural()).isNotNull();

        log.info("원형: {}", result.getOriginalForm());
        log.info("변형 타입: {}", result.getVariantType());
        log.info("품사: {}", result.getPartOfSpeech());
        log.info("복수형: {}", result.getPlural().getPlural());
        log.info("한국어 뜻: {}", result.getMeaningsKo());
    }

    //@Test
    void 단어_분석_테스트_다중품사() {
        // given
        String word = "mean";

        // when
        WordAnalysisResult result = wordAiService.analyzeWord(word);

        // then
        log.info("분석 결과: {}", result);

        assertThat(result).isNotNull();
        assertThat(result.getOriginalForm()).isEqualTo(word);
        assertThat(result.getVariantType()).isNotNull();
        assertThat(result.getPartOfSpeech()).isNotNull();
        assertThat(result.getPartOfSpeech()).hasSizeGreaterThan(1);
        assertThat(result.getMeaningsKo()).isNotEmpty();
        assertThat(result.getExamples()).isNotEmpty();

        log.info("원형: {}", result.getOriginalForm());
        log.info("변형 타입: {}", result.getVariantType());
        log.info("품사: {}", result.getPartOfSpeech());
        log.info("동사 활용: {}", result.getConjugations());
        log.info("비교급: {}", result.getComparatives());
        log.info("한국어 뜻: {}", result.getMeaningsKo());
        log.info("예문: {}", result.getExamples());
    }
}

