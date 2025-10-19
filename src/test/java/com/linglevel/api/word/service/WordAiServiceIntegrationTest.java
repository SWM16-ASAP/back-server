package com.linglevel.api.word.service;

import com.linglevel.api.word.dto.PartOfSpeech;
import com.linglevel.api.word.dto.VariantType;
import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.exception.WordsException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
//@Disabled("실제 AI API를 호출하므로 필요할 때만 실행 (비용 발생)")
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
        assertThat(result.getVariantTypes()).contains(VariantType.PAST_TENSE);
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
        assertThat(result.getVariantTypes()).contains(VariantType.SUPERLATIVE);
        assertThat(result.getComparatives()).isNotNull();
        assertThat(result.getComparatives().getSuperlative()).isEqualTo("prettiest");
    }

    @Test
    @DisplayName("일반 명사 - 복수형 입력 시 단수형으로 변환되어야 함 (books는 PLURAL과 THIRD_PERSON 둘 다)")
    void testNounPlural_books() {
        // given
        String word = "books";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).hasSize(1); // 하나의 entry로 병합되어야 함

        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("book");
        // books는 명사 복수형이면서 동시에 동사 3인칭 형태이므로 둘 다 포함해야 함
        assertThat(result.getVariantTypes()).containsAnyOf(VariantType.PLURAL, VariantType.THIRD_PERSON);
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

        assertThat(seeResult.getVariantTypes()).contains(VariantType.PAST_TENSE);
        assertThat(seeResult.getConjugations()).isNotNull();
        assertThat(seeResult.getConjugations().getPast()).isEqualTo("saw");
        assertThat(seeResult.getSummary()).contains("보다");

        // 두 번째 결과: 톱 (명사)
        WordAnalysisResult sawNounResult = results.stream()
                .filter(r -> r.getOriginalForm().equals("saw"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("'saw' (명사) 결과를 찾을 수 없습니다"));

        assertThat(sawNounResult.getVariantTypes()).contains(VariantType.ORIGINAL_FORM);
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

        assertThat(riseResult.getVariantTypes()).contains(VariantType.PAST_TENSE);

        // 장미 (명사)
        WordAnalysisResult roseNounResult = results.stream()
                .filter(r -> r.getOriginalForm().equals("rose"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("'rose' (명사) 결과를 찾을 수 없습니다"));

        assertThat(roseNounResult.getVariantTypes()).contains(VariantType.ORIGINAL_FORM);
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

        assertThat(leaveResult.getVariantTypes()).contains(VariantType.PAST_TENSE);
        assertThat(leaveResult.getSummary()).anyMatch(s -> s.contains("떠나다") || s.contains("남기다"));

        // 2. '왼쪽'이라는 의미의 'left' 검증
        WordAnalysisResult leftResult = results.stream()
                .filter(r -> r.getOriginalForm().equals("left"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("'left' (형용사/명사) 결과를 찾을 수 없습니다"));

        assertThat(leftResult.getVariantTypes()).contains(VariantType.ORIGINAL_FORM);
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
        assertThat(result.getVariantTypes()).contains(VariantType.ORIGINAL_FORM);
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
        assertThat(result.getVariantTypes()).contains(VariantType.PLURAL);
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
        assertThat(result.getVariantTypes()).contains(VariantType.PAST_TENSE);
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
        assertThat(result.getVariantTypes()).contains(VariantType.PRESENT_PARTICIPLE);
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
        assertThat(result.getVariantTypes()).contains(VariantType.THIRD_PERSON);
        assertThat(result.getConjugations()).isNotNull();
        assertThat(result.getConjugations().getThirdPerson()).isEqualTo("goes");
    }

    @Test
    @DisplayName("말이 안되는 단어 입력 시, WordsException 예외를 던져야 함")
    void testNonsensicalWord_shouldThrowException() {
        // given
        String word = "asdfqwer";
        String targetLanguage = "KO";

        // when & then
        // AI가 의미 없는 단어에 대해 빈 배열을 반환하면 WordsException을 던짐 (RuntimeException으로 래핑됨)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            wordAiService.analyzeWord(word, targetLanguage);
        });

        // RuntimeException의 cause가 WordsException인지 확인
        assertThat(exception.getCause()).isInstanceOf(WordsException.class);
    }

    // ===== 실패 사례 기반 추가 테스트 =====

    @Test
    @DisplayName("대명사 - 'I' (주격 1인칭 대명사)")
    void testPronoun_I() {
        // given
        String word = "I";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("I");
        assertThat(result.getVariantTypes()).contains(VariantType.ORIGINAL_FORM);
        assertThat(result.getMeanings()).isNotEmpty();
        assertThat(result.getMeanings().get(0).getPartOfSpeech()).isEqualTo(PartOfSpeech.PRONOUN);
    }

    @Test
    @DisplayName("대명사 - 'him' (목적격 3인칭 남성 대명사)")
    void testPronoun_him() {
        // given
        String word = "him";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isIn("he", "him");
        assertThat(result.getMeanings()).isNotEmpty();
    }

    @Test
    @DisplayName("대명사 - 'them' (목적격 3인칭 복수 대명사)")
    void testPronoun_them() {
        // given
        String word = "them";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isIn("they", "them");
        assertThat(result.getMeanings()).isNotEmpty();
    }

    @Test
    @DisplayName("부사 - 'absolutely' (-ly 형태 부사)")
    void testAdverb_absolutely() {
        // given
        String word = "absolutely";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("absolutely");
        assertThat(result.getVariantTypes()).contains(VariantType.ORIGINAL_FORM);
        assertThat(result.getMeanings()).isNotEmpty();
        assertThat(result.getMeanings().get(0).getPartOfSpeech()).isEqualTo(PartOfSpeech.ADVERB);
    }

    @Test
    @DisplayName("부사 - 'carefully' (형용사에서 파생된 부사)")
    void testAdverb_carefully() {
        // given
        String word = "carefully";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("carefully");
        assertThat(result.getMeanings()).isNotEmpty();
    }

    @Test
    @DisplayName("부사 - 'quickly'")
    void testAdverb_quickly() {
        // given
        String word = "quickly";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("quickly");
    }

    @Test
    @DisplayName("과거분사/형용사 - 'confused' (혼란스러운)")
    void testParticipleAdjective_confused() {
        // given
        String word = "confused";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        // confused는 confuse의 과거/과거분사이면서 동시에 형용사로도 쓰임
        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("confuse");
        assertThat(result.getVariantTypes()).containsAnyOf(VariantType.PAST_TENSE, VariantType.PAST_PARTICIPLE);
        assertThat(result.getMeanings()).isNotEmpty();
    }

    @Test
    @DisplayName("과거분사/형용사 - 'interested' (관심있는)")
    void testParticipleAdjective_interested() {
        // given
        String word = "interested";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("interest");
        assertThat(result.getVariantTypes()).containsAnyOf(VariantType.PAST_TENSE, VariantType.PAST_PARTICIPLE);
    }

    @Test
    @DisplayName("과거분사/형용사 - 'worried' (걱정하는)")
    void testParticipleAdjective_worried() {
        // given
        String word = "worried";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("worry");
        assertThat(result.getVariantTypes()).containsAnyOf(VariantType.PAST_TENSE, VariantType.PAST_PARTICIPLE);
    }

    @Test
    @DisplayName("숫자 - 'one' (기수)")
    void testNumber_one() {
        // given
        String word = "one";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("one");
        assertThat(result.getVariantTypes()).contains(VariantType.ORIGINAL_FORM);
        assertThat(result.getMeanings()).isNotEmpty();
    }

    @Test
    @DisplayName("숫자 - 'eight' (기수)")
    void testNumber_eight() {
        // given
        String word = "eight";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("eight");
    }

    @Test
    @DisplayName("서수 - 'eleventh' (11번째)")
    void testOrdinal_eleventh() {
        // given
        String word = "eleventh";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("eleventh");
    }

    @Test
    @DisplayName("서수 - 'twentieth' (20번째)")
    void testOrdinal_twentieth() {
        // given
        String word = "twentieth";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("twentieth");
    }

    @Test
    @DisplayName("호칭 - 'Mr' (미스터)")
    void testTitle_Mr() {
        // given
        String word = "Mr";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isIn("Mr", "mr", "MR");
    }

    @Test
    @DisplayName("호칭 - 'Ms' (미즈)")
    void testTitle_Ms() {
        // given
        String word = "Ms";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isIn("Ms", "ms", "MS");
    }

    @Test
    @DisplayName("관계대명사 - 'whom' (목적격 관계대명사)")
    void testRelativePronoun_whom() {
        // given
        String word = "whom";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("whom");
        assertThat(result.getMeanings()).isNotEmpty();
        assertThat(result.getMeanings().get(0).getExample()).isNotBlank();
    }

    @Test
    @DisplayName("의문부사 - 'where' (어디)")
    void testInterrogativeAdverb_where() {
        // given
        String word = "where";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();
        logResults(word, results);

        WordAnalysisResult result = results.get(0);
        assertThat(result.getOriginalForm()).isEqualTo("where");
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
            log.info("  변형 타입: {}", result.getVariantTypes());
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
