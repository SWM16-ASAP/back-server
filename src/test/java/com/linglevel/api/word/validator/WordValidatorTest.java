package com.linglevel.api.word.validator;

import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WordValidatorTest {

    private WordValidator wordValidator;

    @BeforeEach
    void setUp() {
        wordValidator = new WordValidator();
    }

    @Test
    @DisplayName("정상적인 영어 단어는 소문자로 변환하여 반환")
    void validateAndPreprocess_정상적인_영어_단어() {
        // given
        String word = "Hello";

        // when
        String result = wordValidator.validateAndPreprocess(word);

        // then
        assertThat(result).isEqualTo("hello");
    }

    @Test
    @DisplayName("정상적인 한글 단어는 그대로 반환")
    void validateAndPreprocess_정상적인_한글_단어() {
        // given
        String word = "안녕하세요";

        // when
        String result = wordValidator.validateAndPreprocess(word);

        // then
        assertThat(result).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("정상적인 일본어 단어는 그대로 반환")
    void validateAndPreprocess_정상적인_일본어_단어() {
        // given
        String word = "こんにちは";

        // when
        String result = wordValidator.validateAndPreprocess(word);

        // then
        assertThat(result).isEqualTo("こんにちは");
    }

    @Test
    @DisplayName("정상적인 중국어 단어는 그대로 반환")
    void validateAndPreprocess_정상적인_중국어_단어() {
        // given
        String word = "你好";

        // when
        String result = wordValidator.validateAndPreprocess(word);

        // then
        assertThat(result).isEqualTo("你好");
    }

    @Test
    @DisplayName("숫자가 포함된 단어는 허용")
    void validateAndPreprocess_숫자가_포함된_단어() {
        // given
        String word = "word123";

        // when
        String result = wordValidator.validateAndPreprocess(word);

        // then
        assertThat(result).isEqualTo("word123");
    }

    @Test
    @DisplayName("앞뒤 특수문자는 제거됨")
    void validateAndPreprocess_앞뒤_특수문자_제거() {
        // given
        String word = "!!!Hello???";

        // when
        String result = wordValidator.validateAndPreprocess(word);

        // then
        assertThat(result).isEqualTo("hello");
    }

    @ParameterizedTest
    @ValueSource(strings = {"'word'", "\"word\"", "...word...", "(word)", "[word]", "{word}"})
    @DisplayName("다양한 특수문자로 둘러싸인 단어는 전처리 후 반환")
    void validateAndPreprocess_다양한_특수문자_제거(String word) {
        // when
        String result = wordValidator.validateAndPreprocess(word);

        // then
        assertThat(result).isEqualTo("word");
    }

    @Test
    @DisplayName("대문자는 소문자로 변환")
    void validateAndPreprocess_대문자_소문자_변환() {
        // given
        String word = "HELLO";

        // when
        String result = wordValidator.validateAndPreprocess(word);

        // then
        assertThat(result).isEqualTo("hello");
    }

    @Test
    @DisplayName("혼합된 대소문자는 모두 소문자로 변환")
    void validateAndPreprocess_혼합_대소문자_변환() {
        // given
        String word = "HeLLo";

        // when
        String result = wordValidator.validateAndPreprocess(word);

        // then
        assertThat(result).isEqualTo("hello");
    }

    @Test
    @DisplayName("앞뒤 특수문자 제거 + 대소문자 변환이 함께 적용")
    void validateAndPreprocess_전처리_종합() {
        // given
        String word = "!!!HELLO???";

        // when
        String result = wordValidator.validateAndPreprocess(word);

        // then
        assertThat(result).isEqualTo("hello");
    }

    @Test
    @DisplayName("null 입력시 예외 발생")
    void validateAndPreprocess_null_입력() {
        // when & then
        assertThatThrownBy(() -> wordValidator.validateAndPreprocess(null))
                .isInstanceOf(WordsException.class)
                .hasMessage(WordsErrorCode.INVALID_WORD_FORMAT.getMessage());
    }

    @Test
    @DisplayName("빈 문자열 입력시 예외 발생")
    void validateAndPreprocess_빈_문자열() {
        // when & then
        assertThatThrownBy(() -> wordValidator.validateAndPreprocess(""))
                .isInstanceOf(WordsException.class)
                .hasMessage(WordsErrorCode.INVALID_WORD_FORMAT.getMessage());
    }

    @Test
    @DisplayName("특수문자만 있는 경우 예외 발생")
    void validateAndPreprocess_특수문자만_있음() {
        // when & then
        assertThatThrownBy(() -> wordValidator.validateAndPreprocess("!!!???"))
                .isInstanceOf(WordsException.class)
                .hasMessage(WordsErrorCode.INVALID_WORD_FORMAT.getMessage());
    }

    @Test
    @DisplayName("단어 내부에 공백이 있으면 예외 발생")
    void validateAndPreprocess_내부_공백() {
        // when & then
        assertThatThrownBy(() -> wordValidator.validateAndPreprocess("hello world"))
                .isInstanceOf(WordsException.class)
                .hasMessage(WordsErrorCode.INVALID_WORD_FORMAT.getMessage());
    }

    @Test
    @DisplayName("단어 내부에 탭이 있으면 예외 발생")
    void validateAndPreprocess_내부_탭() {
        // when & then
        assertThatThrownBy(() -> wordValidator.validateAndPreprocess("hello\tworld"))
                .isInstanceOf(WordsException.class)
                .hasMessage(WordsErrorCode.INVALID_WORD_FORMAT.getMessage());
    }

    @Test
    @DisplayName("단어 내부에 엔터가 있으면 예외 발생")
    void validateAndPreprocess_내부_엔터() {
        // when & then
        assertThatThrownBy(() -> wordValidator.validateAndPreprocess("hello\nworld"))
                .isInstanceOf(WordsException.class)
                .hasMessage(WordsErrorCode.INVALID_WORD_FORMAT.getMessage());
    }

    @Test
    @DisplayName("단어 내부에 특수문자가 있으면 예외 발생")
    void validateAndPreprocess_내부_특수문자() {
        // when & then
        assertThatThrownBy(() -> wordValidator.validateAndPreprocess("hello-world"))
                .isInstanceOf(WordsException.class)
                .hasMessage(WordsErrorCode.INVALID_WORD_FORMAT.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello@world", "hello#world", "hello$world", "hello%world"})
    @DisplayName("다양한 특수문자가 내부에 있으면 예외 발생")
    void validateAndPreprocess_다양한_내부_특수문자(String word) {
        // when & then
        assertThatThrownBy(() -> wordValidator.validateAndPreprocess(word))
                .isInstanceOf(WordsException.class)
                .hasMessage(WordsErrorCode.INVALID_WORD_FORMAT.getMessage());
    }

    @Test
    @DisplayName("정확히 50자인 단어는 허용")
    void validateAndPreprocess_50자_단어_허용() {
        // given
        String word = "a".repeat(50);

        // when
        String result = wordValidator.validateAndPreprocess(word);

        // then
        assertThat(result).isEqualTo(word);
        assertThat(result).hasSize(50);
    }

    @Test
    @DisplayName("50자를 초과하는 단어는 예외 발생")
    void validateAndPreprocess_50자_초과() {
        // given
        String word = "a".repeat(51);

        // when & then
        assertThatThrownBy(() -> wordValidator.validateAndPreprocess(word))
                .isInstanceOf(WordsException.class)
                .hasMessage(WordsErrorCode.WORD_TOO_LONG.getMessage());
    }

    @Test
    @DisplayName("전처리 후 50자를 초과하면 예외 발생하지 않음 - 전처리 전 길이가 51자, 전처리 후 50자")
    void validateAndPreprocess_전처리_후_50자() {
        // given - 앞뒤 특수문자 1개씩 포함하여 총 52자
        String word = "!" + "a".repeat(50) + "!";

        // when
        String result = wordValidator.validateAndPreprocess(word);

        // then
        assertThat(result).hasSize(50);
    }

    @Test
    @DisplayName("전처리 후 50자를 초과하면 예외 발생")
    void validateAndPreprocess_전처리_후_50자_초과() {
        // given - 앞뒤 특수문자 1개씩 포함하여 총 53자
        String word = "!" + "a".repeat(51) + "!";

        // when & then
        assertThatThrownBy(() -> wordValidator.validateAndPreprocess(word))
                .isInstanceOf(WordsException.class)
                .hasMessage(WordsErrorCode.WORD_TOO_LONG.getMessage());
    }

    @Test
    @DisplayName("한글 50자는 허용")
    void validateAndPreprocess_한글_50자() {
        // given
        String word = "가".repeat(50);

        // when
        String result = wordValidator.validateAndPreprocess(word);

        // then
        assertThat(result).hasSize(50);
    }

    @Test
    @DisplayName("한글 51자는 예외 발생")
    void validateAndPreprocess_한글_51자() {
        // given
        String word = "가".repeat(51);

        // when & then
        assertThatThrownBy(() -> wordValidator.validateAndPreprocess(word))
                .isInstanceOf(WordsException.class)
                .hasMessage(WordsErrorCode.WORD_TOO_LONG.getMessage());
    }
}
