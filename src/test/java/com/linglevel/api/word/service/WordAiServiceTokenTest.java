package com.linglevel.api.word.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * WordAiService 토큰 크기 및 비용 측정 테스트
 *
 * 주의:
 * - 실제 AI API를 호출하므로 비용이 발생합니다
 * - 기본적으로 @Disabled로 비활성화되어 있습니다
 * - 토큰 크기를 확인하고 싶을 때만 주석을 제거하여 실행하세요
 *
 * 실행 방법:
 * 1. @Disabled 주석 제거
 * 2. ./gradlew test --tests WordAiServiceTokenTest
 *
 * 참고:
 * - WordAiService에서 자동으로 토큰 사용량과 비용을 로깅합니다
 * - 입력: $0.00017 per 1K tokens, 출력: $0.000085 per 1K tokens
 * - 환율: 1 USD = 1430 KRW
 */
@SpringBootTest
@ActiveProfiles("local")
@Slf4j
@Disabled("실제 AI API를 호출하므로 필요할 때만 실행 (비용 발생)")
class WordAiServiceTokenTest {

    @Autowired
    private WordAiService wordAiService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Homograph 단어 'saw' 응답 토큰 크기 측정")
    void measureTokenSize_saw() throws Exception {
        // given
        String word = "saw";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();

        // JSON으로 직렬화
        String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(results);

        // 크기 측정
        int characterCount = jsonResponse.length();
        int estimatedTokens = characterCount / 4; // 대략적인 토큰 수 (1 토큰 ≈ 4자)

        // 로그 출력
        log.info("\n" + "=".repeat(100));
        log.info("단어: {}", word);
        log.info("결과 개수: {}", results.size());
        log.info("=".repeat(100));
        log.info("JSON 응답:\n{}", jsonResponse);
        log.info("=".repeat(100));
        log.info("📊 응답 크기 통계:");
        log.info("  - 문자 수: {} characters", characterCount);
        log.info("  - 예상 토큰 수: ~{} tokens (문자수 / 4)", estimatedTokens);
        log.info("  - 바이트 크기: {} bytes", jsonResponse.getBytes().length);
        log.info("=".repeat(100));

        // 현재 max-tokens 설정과 비교
        int currentMaxTokens = 2000;
        if (estimatedTokens > currentMaxTokens) {
            log.warn("⚠️  예상 토큰 수({})가 현재 설정({}tokens)을 초과합니다!", estimatedTokens, currentMaxTokens);
        } else {
            double usagePercentage = (double) estimatedTokens / currentMaxTokens * 100;
            log.info("✅ 현재 max-tokens 설정({})으로 충분합니다 (사용률: {:.1f}%)", currentMaxTokens, usagePercentage);
        }
    }

    @Test
    @DisplayName("Homograph 단어 'left' 응답 토큰 크기 측정")
    void measureTokenSize_left() throws Exception {
        // given
        String word = "left";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();

        String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(results);

        int characterCount = jsonResponse.length();
        int estimatedTokens = characterCount / 4;

        log.info("\n" + "=".repeat(100));
        log.info("단어: {}", word);
        log.info("결과 개수: {}", results.size());
        log.info("=".repeat(100));
        log.info("JSON 응답:\n{}", jsonResponse);
        log.info("=".repeat(100));
        log.info("📊 응답 크기 통계:");
        log.info("  - 문자 수: {} characters", characterCount);
        log.info("  - 예상 토큰 수: ~{} tokens", estimatedTokens);
        log.info("  - 바이트 크기: {} bytes", jsonResponse.getBytes().length);
        log.info("=".repeat(100));

        int currentMaxTokens = 2000;
        if (estimatedTokens > currentMaxTokens) {
            log.warn("⚠️  예상 토큰 수({})가 현재 설정({}tokens)을 초과합니다!", estimatedTokens, currentMaxTokens);
        } else {
            double usagePercentage = (double) estimatedTokens / currentMaxTokens * 100;
            log.info("✅ 현재 max-tokens 설정({})으로 충분합니다 (사용률: {:.1f}%)", currentMaxTokens, usagePercentage);
        }
    }

    @Test
    @DisplayName("Homograph 단어 'rose' 응답 토큰 크기 측정")
    void measureTokenSize_rose() throws Exception {
        // given
        String word = "rose";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();

        String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(results);

        int characterCount = jsonResponse.length();
        int estimatedTokens = characterCount / 4;

        log.info("\n" + "=".repeat(100));
        log.info("단어: {}", word);
        log.info("결과 개수: {}", results.size());
        log.info("=".repeat(100));
        log.info("JSON 응답:\n{}", jsonResponse);
        log.info("=".repeat(100));
        log.info("📊 응답 크기 통계:");
        log.info("  - 문자 수: {} characters", characterCount);
        log.info("  - 예상 토큰 수: ~{} tokens", estimatedTokens);
        log.info("  - 바이트 크기: {} bytes", jsonResponse.getBytes().length);
        log.info("=".repeat(100));

        int currentMaxTokens = 2000;
        if (estimatedTokens > currentMaxTokens) {
            log.warn("⚠️  예상 토큰 수({})가 현재 설정({}tokens)을 초과합니다!", estimatedTokens, currentMaxTokens);
        } else {
            double usagePercentage = (double) estimatedTokens / currentMaxTokens * 100;
            log.info("✅ 현재 max-tokens 설정({})으로 충분합니다 (사용률: {:.1f}%)", currentMaxTokens, usagePercentage);
        }
    }

    @Test
    @DisplayName("일반 단어 'run' 응답 토큰 크기 측정 (비교용)")
    void measureTokenSize_run() throws Exception {
        // given
        String word = "run";
        String targetLanguage = "KO";

        // when
        List<WordAnalysisResult> results = wordAiService.analyzeWord(word, targetLanguage);

        // then
        assertThat(results).isNotEmpty();

        String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(results);

        int characterCount = jsonResponse.length();
        int estimatedTokens = characterCount / 4;

        log.info("\n" + "=".repeat(100));
        log.info("단어: {} (일반 단어 - 비교용)", word);
        log.info("결과 개수: {}", results.size());
        log.info("=".repeat(100));
        log.info("JSON 응답:\n{}", jsonResponse);
        log.info("=".repeat(100));
        log.info("📊 응답 크기 통계:");
        log.info("  - 문자 수: {} characters", characterCount);
        log.info("  - 예상 토큰 수: ~{} tokens", estimatedTokens);
        log.info("  - 바이트 크기: {} bytes", jsonResponse.getBytes().length);
        log.info("=".repeat(100));

        int currentMaxTokens = 2000;
        if (estimatedTokens > currentMaxTokens) {
            log.warn("⚠️  예상 토큰 수({})가 현재 설정({}tokens)을 초과합니다!", estimatedTokens, currentMaxTokens);
        } else {
            double usagePercentage = (double) estimatedTokens / currentMaxTokens * 100;
            log.info("✅ 현재 max-tokens 설정({})으로 충분합니다 (사용률: {:.1f}%)", currentMaxTokens, usagePercentage);
        }
    }
}
