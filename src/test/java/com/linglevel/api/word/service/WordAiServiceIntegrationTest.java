package com.linglevel.api.word.service;

import com.linglevel.api.word.dto.VariantType;
import com.linglevel.api.word.dto.WordAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WordAiService 통합 테스트 - 실제 AI 모델을 호출하여 프롬프트 엔지니어링 결과를 검증
 *
 * 주의:
 * - 이 테스트는 실제 AWS Bedrock API를 호출하므로 비용이 발생합니다
 * - 기본적으로 @Disabled로 비활성화되어 있습니다
 * - 프롬프트를 수정하거나 결과를 확인할 때만 주석을 제거하여 실행하세요
 *
 * 실행 방법:
 * 1. @Disabled 주석 제거
 * 2. ./gradlew test --tests WordAiServiceIntegrationTest
 * 3. 또는 IDE에서 개별 테스트 실행
 */
@SpringBootTest
@ActiveProfiles("local")
@Slf4j
@Disabled("실제 AI API를 호출하므로 필요할 때만 실행 (비용 발생)")
class WordAiServiceIntegrationTest {

    @Autowired
    private WordAiService wordAiService;

    @Test
    @DisplayName("일반 동사 - 과거형 입력 시 원형으로 변환되어야 함")
    void testVerbPastTense_ran() {
        // given
        String word = "ran";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();

        // 로그로 결과 출력 (프롬프트 엔지니어링 검증용)
        logResults(word, results);

        // 기본 검증
        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("run");
        assertThat(result.getVariantType()).isEqualTo(VariantType.PAST_TENSE);
        assertThat(result.getSourceLanguageCode()).isNotNull();
        assertThat(result.getTargetLanguageCode()).isNotNull();
        assertThat(result.getSummary()).isNotEmpty();
        assertThat(result.getSummary().size()).isLessThanOrEqualTo(3);
        assertThat(result.getMeanings()).isNotEmpty();
        assertThat(result.getConjugations()).isNotNull();
        assertThat(result.getConjugations().getPast()).isEqualTo("ran");
    }

    @Test
    @DisplayName("일반 형용사 - 최상급 입력 시 원형으로 변환되어야 함")
    void testAdjectiveSuperlative_prettiest() {
        // given
        String word = "prettiest";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();

        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("pretty");
        assertThat(result.getVariantType()).isEqualTo(VariantType.SUPERLATIVE);
        assertThat(result.getComparatives()).isNotNull();
        assertThat(result.getComparatives().getSuperlative()).isEqualTo("prettiest");
    }

    @Test
    @DisplayName("일반 명사 - 복수형 입력 시 단수형으로 변환되어야 함")
    void testNounPlural_books() {
        // given
        String word = "books";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();

        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("book");
        assertThat(result.getVariantType()).isEqualTo(VariantType.PLURAL);
        assertThat(result.getPlural()).isNotNull();
        assertThat(result.getPlural().getPlural()).isEqualTo("books");
    }

    @Test
    @DisplayName("Homograph - 'saw' (see의 과거형 + 톱)")
    void testHomograph_saw() {
        // given
        String word = "saw";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).hasSize(2); // 두 가지 의미를 가진 결과가 있어야 함

        logResults(word, results);

        // 첫 번째 결과: see의 과거형
        WordAnalysisResult seeResult = results.stream()
                .filter(r -> r.getOriginalForm().equals("see"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("'see' 결과를 찾을 수 없습니다"));

        assertThat(seeResult.getVariantType()).isEqualTo(VariantType.PAST_TENSE);
        assertThat(seeResult.getConjugations()).isNotNull();
        assertThat(seeResult.getConjugations().getPast()).isEqualTo("saw");
        assertThat(seeResult.getSummary()).contains("보다");

        // 두 번째 결과: 톱 (명사)
        WordAnalysisResult sawNounResult = results.stream()
                .filter(r -> r.getOriginalForm().equals("saw"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("'saw' (명사) 결과를 찾을 수 없습니다"));

        assertThat(sawNounResult.getVariantType()).isEqualTo(VariantType.ORIGINAL_FORM);
        assertThat(sawNounResult.getPlural()).isNotNull();
        assertThat(sawNounResult.getSummary()).contains("톱");
    }

    @Test
    @DisplayName("Homograph - 'rose' (rise의 과거형 + 장미)")
    void testHomograph_rose() {
        // given
        String word = "rose";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).hasSize(2);

        logResults(word, results);

        // rise의 과거형
        WordAnalysisResult riseResult = results.stream()
                .filter(r -> r.getOriginalForm().equals("rise"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("'rise' 결과를 찾을 수 없습니다"));

        assertThat(riseResult.getVariantType()).isEqualTo(VariantType.PAST_TENSE);

        // 장미 (명사)
        WordAnalysisResult roseNounResult = results.stream()
                .filter(r -> r.getOriginalForm().equals("rose"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("'rose' (명사) 결과를 찾을 수 없습니다"));

        assertThat(roseNounResult.getVariantType()).isEqualTo(VariantType.ORIGINAL_FORM);
        assertThat(roseNounResult.getSummary()).contains("장미");
    }

    @Test
    @DisplayName("Homograph - 'left' (leave의 과거형 + 왼쪽)")
    void testHomograph_left() {
        // given
        String word = "left";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).hasSize(2);

        logResults(word, results);

        // 1. 'leave'의 과거형 검증
        WordAnalysisResult leaveResult = results.stream()
                .filter(r -> r.getOriginalForm().equals("leave"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("'leave' 결과를 찾을 수 없습니다"));

        assertThat(leaveResult.getVariantType()).isEqualTo(VariantType.PAST_TENSE);
        assertThat(leaveResult.getSummary()).anyMatch(s -> s.contains("떠나다") || s.contains("남기다"));

        // 2. '왼쪽'이라는 의미의 'left' 검증
        WordAnalysisResult leftResult = results.stream()
                .filter(r -> r.getOriginalForm().equals("left"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("'left' (형용사/명사) 결과를 찾을 수 없습니다"));

        assertThat(leftResult.getVariantType()).isEqualTo(VariantType.ORIGINAL_FORM);
        assertThat(leftResult.getSummary()).contains("왼쪽");
    }

    @Test
    @DisplayName("원형 단어 - 'run' (동사)")
    void testOriginalForm_run() {
        // given
        String word = "run";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).hasSize(1);

        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("run");
        assertThat(result.getVariantType()).isEqualTo(VariantType.ORIGINAL_FORM);
        assertThat(result.getSummary()).containsAnyOf("달리다", "운영하다", "작동하다");
        assertThat(result.getConjugations()).isNotNull();
        assertThat(result.getConjugations().getPast()).isEqualTo("ran");
        assertThat(result.getConjugations().getPresentParticiple()).isEqualTo("running");
    }

    @Test
    @DisplayName("불규칙 복수형 - 'children'")
    void testIrregularPlural_children() {
        // given
        String word = "children";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();

        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("child");
        assertThat(result.getVariantType()).isEqualTo(VariantType.PLURAL);
        assertThat(result.getPlural()).isNotNull();
        assertThat(result.getPlural().getSingular()).isEqualTo("child");
        assertThat(result.getPlural().getPlural()).isEqualTo("children");
    }

    @Test
    @DisplayName("불규칙 동사 - 'went' (go의 과거형)")
    void testIrregularVerb_went() {
        // given
        String word = "went";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();

        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("go");
        assertThat(result.getVariantType()).isEqualTo(VariantType.PAST_TENSE);
        assertThat(result.getConjugations()).isNotNull();
        assertThat(result.getConjugations().getPast()).isEqualTo("went");
        assertThat(result.getConjugations().getPastParticiple()).isEqualTo("gone");
    }

    @Test
    @DisplayName("현재분사 - 'running'")
    void testPresentParticiple_running() {
        // given
        String word = "running";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();

        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("run");
        assertThat(result.getVariantType()).isEqualTo(VariantType.PRESENT_PARTICIPLE);
        assertThat(result.getConjugations()).isNotNull();
        assertThat(result.getConjugations().getPresentParticiple()).isEqualTo("running");
    }

    @Test
    @DisplayName("3인칭 단수 - 'goes'")
    void testThirdPerson_goes() {
        // given
        String word = "goes";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();

        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("go");
        assertThat(result.getVariantType()).isEqualTo(VariantType.THIRD_PERSON);
        assertThat(result.getConjugations()).isNotNull();
        assertThat(result.getConjugations().getThirdPerson()).isEqualTo("goes");
    }

    @Test
    @DisplayName("말이 안되는 단어 입력 시, 빈 리스트를 반환해야 함")
    void testNonsensicalWord_shouldReturnEmptyList() {
        // given
        String word = "asdfqwer";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        // AI가 의미 없는 단어에 대해 결과를 생성하지 않아야 함 (빈 리스트)
        assertThat(results).isNotNull();
        assertThat(results).isEmpty();
    }

    /**
     * 테스트 결과를 보기 좋게 로그로 출력
     */
    private void logResults(String word, List<WordAnalysisResult> results) {
        log.info("\n" + "=".repeat(80));
        log.info("검색어: {}", word);
        log.info("결과 개수: {}", results.size());
        log.info("=".repeat(80));

        for (int i = 0; i < results.size(); i++) {
            WordAnalysisResult result = results.get(i);
            log.info("\n[결과 #{}]", i + 1);
            log.info("  원형: {}", result.getOriginalForm());
            log.info("  변형 타입: {}", result.getVariantType());
            log.info("  언어: {} -> {}", result.getSourceLanguageCode(), result.getTargetLanguageCode());
            log.info("  요약: {}", result.getSummary());

            log.info("  의미:");
            result.getMeanings().forEach(meaning -> {
                log.info("    - [{}] {}", meaning.getPartOfSpeech(), meaning.getMeaning());
                log.info("      예문: {}", meaning.getExample());
                log.info("      번역: {}", meaning.getExampleTranslation());
            });

            if (result.getConjugations() != null) {
                log.info("  동사 활용:");
                log.info("    - 현재: {}", result.getConjugations().getPresent());
                log.info("    - 과거: {}", result.getConjugations().getPast());
                log.info("    - 과거분사: {}", result.getConjugations().getPastParticiple());
                log.info("    - 현재분사: {}", result.getConjugations().getPresentParticiple());
                log.info("    - 3인칭: {}", result.getConjugations().getThirdPerson());
            }

            if (result.getComparatives() != null) {
                log.info("  형용사 비교:");
                log.info("    - 원급: {}", result.getComparatives().getPositive());
                log.info("    - 비교급: {}", result.getComparatives().getComparative());
                log.info("    - 최상급: {}", result.getComparatives().getSuperlative());
            }

            if (result.getPlural() != null) {
                log.info("  명사 복수:");
                log.info("    - 단수: {}", result.getPlural().getSingular());
                log.info("    - 복수: {}", result.getPlural().getPlural());
            }
        }

        log.info("\n" + "=".repeat(80) + "\n");
    }
}
