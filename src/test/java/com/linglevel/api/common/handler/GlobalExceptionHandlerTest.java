package com.linglevel.api.common.handler;

import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.i18n.LanguageCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

	@Test
	@DisplayName("요청 파라미터 타입 변환 실패는 400 Bad Request로 응답")
	void handleMethodArgumentTypeMismatchException_returnsBadRequest() {
		MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException("INVALID",
				LanguageCode.class, "targetLanguage", null, null);

		ResponseEntity<ExceptionResponse> response = globalExceptionHandler
			.handleMethodArgumentTypeMismatchException(exception);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getMessage()).isEqualTo("targetLanguage: 올바르지 않은 값입니다");
	}

}
