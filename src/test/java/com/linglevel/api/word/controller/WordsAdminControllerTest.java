package com.linglevel.api.word.controller;

import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.WordSearchResponse;
import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import com.linglevel.api.word.service.Oxford3000Service;
import com.linglevel.api.word.service.WordService;
import com.linglevel.api.word.validator.WordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WordsAdminControllerTest {

	@Mock
	private WordService wordService;

	@Mock
	private Oxford3000Service oxford3000Service;

	private WordsAdminController wordsAdminController;

	@BeforeEach
	void setUp() {
		wordsAdminController = new WordsAdminController(wordService, oxford3000Service, new WordValidator());
	}

	@Test
	@DisplayName("강제 재분석 단어는 일반 조회 API와 동일하게 정규화 후 서비스로 전달")
	void forceAnalyzeWord_normalizesWordBeforeServiceCall() {
		WordSearchResponse expectedResponse = WordSearchResponse.builder()
			.searchedWord("run")
			.results(List.of())
			.build();
		when(wordService.forceReanalyzeWord("run", LanguageCode.KO, true, false)).thenReturn(expectedResponse);

		ResponseEntity<WordSearchResponse> response = wordsAdminController.forceAnalyzeWord("Run!", LanguageCode.KO,
				true, false);

		assertThat(response.getBody()).isSameAs(expectedResponse);
		verify(wordService).forceReanalyzeWord("run", LanguageCode.KO, true, false);
	}

	@Test
	@DisplayName("강제 재분석 대상 언어가 영어면 서비스 호출 전 차단")
	void forceAnalyzeWord_rejectsEnglishTargetLanguage() {
		assertThatThrownBy(() -> wordsAdminController.forceAnalyzeWord("run", LanguageCode.EN, false, false))
			.isInstanceOfSatisfying(WordsException.class,
					e -> assertThat(e.getErrorCode()).isEqualTo(WordsErrorCode.SAME_SOURCE_TARGET_LANGUAGE));

		verify(wordService, never()).forceReanalyzeWord("run", LanguageCode.EN, false, false);
	}

}
